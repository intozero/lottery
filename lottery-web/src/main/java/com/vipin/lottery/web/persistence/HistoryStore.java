package com.vipin.lottery.web.persistence;

import com.vipin.lottery.core.model.DrawRecord;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class HistoryStore {
    private final JdbcTemplate db;
    private final TransactionTemplate transactions;

    public HistoryStore(JdbcTemplate db, TransactionTemplate transactions) {
        this.db = db;
        this.transactions = transactions;
    }

    public List<DrawRecord> all(String game) {
        DrawRecord.maximum(game);
        return db.query(
                "SELECT * FROM draws WHERE game=? ORDER BY draw_date",
                (r, i) ->
                        new DrawRecord(
                                r.getDate("draw_date").toLocalDate(),
                                List.of(
                                        r.getInt("w1"),
                                        r.getInt("w2"),
                                        r.getInt("w3"),
                                        r.getInt("w4"),
                                        r.getInt("w5")),
                                r.getObject("special", Integer.class)),
                game);
    }

    public List<DrawRecord> selected(String game, LocalDate from, LocalDate to) {
        return selected(game, from, to, 1);
    }

    public List<DrawRecord> selected(String game, LocalDate from, LocalDate to, int rowStep) {
        return selected(game, from, to, rowStep, 1);
    }

    public List<DrawRecord> selected(
            String game, LocalDate from, LocalDate to, int rowStep, int startDraw) {
        return selected(game, from, to, rowStep, startDraw, null);
    }

    public List<DrawRecord> selected(
            String game,
            LocalDate from,
            LocalDate to,
            int rowStep,
            int startDraw,
            Integer lastDraw) {
        return selectedNumbered(game, from, to, rowStep, startDraw, lastDraw).stream()
                .map(NumberedDraw::draw)
                .toList();
    }

    public record NumberedDraw(int drawNumber, DrawRecord draw) {}

    public List<NumberedDraw> selectedNumbered(
            String game,
            LocalDate from,
            LocalDate to,
            int rowStep,
            int startDraw,
            Integer lastDraw) {
        if (lastDraw != null && (lastDraw < 1 || lastDraw < startDraw))
            throw new IllegalArgumentException(
                    "Last draw must be a positive whole number at or after starting draw");
        if (startDraw < 1)
            throw new IllegalArgumentException("Starting draw must be a positive whole number");
        if (rowStep < 1)
            throw new IllegalArgumentException("Row step must be a positive whole number");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must be before end date");
        var source = all(game);
        return java.util.stream.IntStream.range(
                        startDraw - 1,
                        lastDraw == null ? source.size() : Math.min(lastDraw, source.size()))
                .filter(i -> (i - (startDraw - 1)) % rowStep == 0)
                .mapToObj(i -> new NumberedDraw(i + 1, source.get(i)))
                .filter(
                        d ->
                                (from == null || !d.draw().date().isBefore(from))
                                        && (to == null || !d.draw().date().isAfter(to)))
                .toList();
    }

    public int lastDraw(String game) {
        DrawRecord.maximum(game);
        return db.queryForObject("SELECT COUNT(*) FROM draws WHERE game=?", Integer.class, game);
    }

    public record ImportResult(int added, int corrected, int unchanged) {}

    // Serializes in-process imports; the database also enforces unique(game,date).
    public synchronized ImportResult save(
            String game, List<DrawRecord> rows, String source, boolean official) {
        DrawRecord.maximum(game);
        if (source == null || source.isBlank() || source.length() > 255)
            throw new IllegalArgumentException("Invalid source label");
        return transactions.execute(
                status -> {
                    Map<LocalDate, DrawRecord> existing = new HashMap<>();
                    all(game).forEach(d -> existing.put(d.date(), d));
                    int added = 0, corrected = 0, unchanged = 0;
                    for (DrawRecord row : rows) {
                        if (row.whites().get(4) > DrawRecord.maximum(game))
                            throw new IllegalArgumentException("White ball outside game range");
                        DrawRecord old = existing.get(row.date());
                        if (old != null && old.equals(row)) {
                            unchanged++;
                            continue;
                        }
                        // Ordinary imports may fill a missing special ball but never replace known
                        // results.
                        if (old != null
                                && !official
                                && !(old.whites().equals(row.whites())
                                        && old.special() == null
                                        && row.special() != null))
                            throw new IllegalArgumentException(
                                    "Existing draw conflicts on "
                                            + row.date()
                                            + "; no rows imported");
                        List<Integer> w = row.whites();
                        if (old == null) {
                            db.update(
                                    "INSERT INTO draws(game,draw_date,w1,w2,w3,w4,w5,special,source) VALUES(?,?,?,?,?,?,?,?,?)",
                                    game,
                                    Date.valueOf(row.date()),
                                    w.get(0),
                                    w.get(1),
                                    w.get(2),
                                    w.get(3),
                                    w.get(4),
                                    row.special(),
                                    source);
                            added++;
                        } else {
                            db.update(
                                    "INSERT INTO draw_changes(game,draw_date,previous_values,new_values,source) VALUES(?,?,?,?,?)",
                                    game,
                                    Date.valueOf(row.date()),
                                    old.values(),
                                    row.values(),
                                    source);
                            db.update(
                                    "UPDATE draws SET w1=?,w2=?,w3=?,w4=?,w5=?,special=?,source=?,updated_at=CURRENT_TIMESTAMP WHERE game=? AND draw_date=?",
                                    w.get(0),
                                    w.get(1),
                                    w.get(2),
                                    w.get(3),
                                    w.get(4),
                                    row.special(),
                                    source,
                                    game,
                                    Date.valueOf(row.date()));
                            corrected++;
                        }
                        existing.put(row.date(), row);
                    }
                    db.update(
                            "INSERT INTO imports(game,source,added,corrected,unchanged) VALUES(?,?,?,?,?)",
                            game,
                            source,
                            added,
                            corrected,
                            unchanged);
                    return new ImportResult(added, corrected, unchanged);
                });
    }

    public List<Map<String, Object>> imports() {
        return db.queryForList("SELECT * FROM imports ORDER BY id DESC LIMIT 100");
    }

    public List<Map<String, Object>> changes() {
        return db.queryForList("SELECT * FROM draw_changes ORDER BY id DESC LIMIT 200");
    }
}
