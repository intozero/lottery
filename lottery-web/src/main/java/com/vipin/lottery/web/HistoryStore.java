package com.vipin.lottery.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Service
public class HistoryStore {
    private final JdbcTemplate db;
    private final TransactionTemplate transactions;
    public HistoryStore(JdbcTemplate db, TransactionTemplate transactions) { this.db = db; this.transactions = transactions; }

    public List<DrawRecord> all(String game) {
        DrawRecord.maximum(game);
        return db.query("SELECT * FROM draws WHERE game=? ORDER BY draw_date", (r, i) ->
                new DrawRecord(r.getDate("draw_date").toLocalDate(),
                        List.of(r.getInt("w1"),r.getInt("w2"),r.getInt("w3"),r.getInt("w4"),r.getInt("w5")),
                        r.getObject("special", Integer.class)), game);
    }
    public List<DrawRecord> selected(String game, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Start date must be before end date");
        return all(game).stream().filter(d -> (from == null || !d.date().isBefore(from)) && (to == null || !d.date().isAfter(to))).toList();
    }

    public record ImportResult(int added, int corrected, int unchanged) {}
    // Serializes in-process imports; the database also enforces unique(game,date).
    public synchronized ImportResult save(String game, List<DrawRecord> rows, String source, boolean official) {
        DrawRecord.maximum(game);
        if (source == null || source.isBlank() || source.length() > 255) throw new IllegalArgumentException("Invalid source label");
        return transactions.execute(status -> {
            Map<LocalDate, DrawRecord> existing = new HashMap<>();
            all(game).forEach(d -> existing.put(d.date(),d));
            int added=0, corrected=0, unchanged=0;
            for (DrawRecord row : rows) {
                if (row.whites().get(4) > DrawRecord.maximum(game)) throw new IllegalArgumentException("White ball outside game range");
                DrawRecord old = existing.get(row.date());
                if (old != null && old.equals(row)) { unchanged++; continue; }
                // Ordinary imports may fill a missing special ball but never replace known results.
                if (old != null && !official && !(old.whites().equals(row.whites()) && old.special() == null && row.special() != null))
                    throw new IllegalArgumentException("Existing draw conflicts on " + row.date() + "; no rows imported");
                List<Integer> w = row.whites();
                if (old == null) {
                    db.update("INSERT INTO draws(game,draw_date,w1,w2,w3,w4,w5,special,source) VALUES(?,?,?,?,?,?,?,?,?)",
                            game,Date.valueOf(row.date()),w.get(0),w.get(1),w.get(2),w.get(3),w.get(4),row.special(),source);
                    added++;
                } else {
                    db.update("INSERT INTO draw_changes(game,draw_date,previous_values,new_values,source) VALUES(?,?,?,?,?)",
                            game,Date.valueOf(row.date()),old.values(),row.values(),source);
                    db.update("UPDATE draws SET w1=?,w2=?,w3=?,w4=?,w5=?,special=?,source=?,updated_at=CURRENT_TIMESTAMP WHERE game=? AND draw_date=?",
                            w.get(0),w.get(1),w.get(2),w.get(3),w.get(4),row.special(),source,game,Date.valueOf(row.date()));
                    corrected++;
                }
                existing.put(row.date(),row);
            }
            db.update("INSERT INTO imports(game,source,added,corrected,unchanged) VALUES(?,?,?,?,?)",game,source,added,corrected,unchanged);
            return new ImportResult(added,corrected,unchanged);
        });
    }
    public List<Map<String,Object>> imports() { return db.queryForList("SELECT * FROM imports ORDER BY id DESC LIMIT 100"); }
    public List<Map<String,Object>> changes() { return db.queryForList("SELECT * FROM draw_changes ORDER BY id DESC LIMIT 200"); }
}
