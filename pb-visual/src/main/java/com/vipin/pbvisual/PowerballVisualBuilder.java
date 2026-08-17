package com.vipin.pbvisual;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Full-outer-joins Powerball, reward, and astronomy tables using date as the key. */
public final class PowerballVisualBuilder {
    private static final DateTimeFormatter DRAW_DATE =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    private static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "Date", "White Ball 1", "White Ball 2", "White Ball 3", "White Ball 4",
            "White Ball 5", "Powerball", "Jackpot (Annuity)", "Day", "Time", "Moon %",
            "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Rahu"));

    private PowerballVisualBuilder() {
    }

    public static void main(String[] args) {
        if (args.length != 0 && args.length != 5) {
            System.err.println("Usage: PowerballVisualBuilder "
                    + "[draws-file astro-file reward-file output-csv output-html]");
            System.exit(1);
        }

        Path dataDirectory = Paths.get("files", "pb_visual");
        Path draws = args.length == 5 ? Paths.get(args[0]) : dataDirectory.resolve("pb-sorted.txt");
        Path astro = args.length == 5 ? Paths.get(args[1]) : dataDirectory.resolve("astro_dates.txt");
        Path rewards = args.length == 5 ? Paths.get(args[2]) : dataDirectory.resolve("reward.txt");
        Path csv = args.length == 5 ? Paths.get(args[3]) : dataDirectory.resolve("pb-merged.csv");
        Path html = args.length == 5 ? Paths.get(args[4]) : dataDirectory.resolve("pb-merged.html");

        try {
            Map<LocalDate, Map<String, String>> rows =
                    new TreeMap<>(Collections.reverseOrder());
            int conflictingDraws = readDraws(draws, rows);
            readPipeTable(rewards, rows, "reward");
            readPipeTable(astro, rows, "astronomy");
            createParent(csv);
            createParent(html);
            writeCsv(csv, rows);
            writeHtml(html, rows);
            System.out.println("Merged " + rows.size() + " dates.");
            if (conflictingDraws > 0) {
                System.out.println("Resolved " + conflictingDraws
                        + " conflicting duplicate draw rows by keeping the last row per date.");
            }
            System.out.println("CSV:  " + csv.toAbsolutePath());
            System.out.println("HTML: " + html.toAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to build Powerball visualization: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static int readDraws(Path path, Map<LocalDate, Map<String, String>> rows)
            throws IOException {
        int conflicts = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 6 && fields.length != 7) {
                    throw invalid(path, lineNumber,
                            "expected date, five white balls, and optional Powerball");
                }
                try {
                    LocalDate date = LocalDate.parse(fields[0], DRAW_DATE);
                    Map<String, String> row = rowFor(rows, date);
                    if (row.containsKey("White Ball 1") && !sameDraw(row, fields)) {
                        conflicts++;
                    }
                    for (int index = 1; index <= 5; index++) {
                        row.put("White Ball " + index, fields[index]);
                    }
                    if (fields.length == 7) {
                        row.put("Powerball", fields[6]);
                    } else {
                        row.remove("Powerball");
                    }
                } catch (DateTimeParseException exception) {
                    throw invalid(path, lineNumber, "invalid date: " + fields[0]);
                }
            }
        }
        return conflicts;
    }

    private static boolean sameDraw(Map<String, String> row, String[] fields) {
        for (int index = 1; index <= 5; index++) {
            if (!fields[index].equals(row.get("White Ball " + index))) {
                return false;
            }
        }
        String powerball = fields.length == 7 ? fields[6] : null;
        return powerball == null ? row.get("Powerball") == null
                : powerball.equals(row.get("Powerball"));
    }

    private static void readPipeTable(Path path, Map<LocalDate, Map<String, String>> rows,
                                      String tableName) throws IOException {
        List<String> headers = null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("+") || isMarkdownSeparator(trimmed)) {
                    continue;
                }
                if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
                    throw invalid(path, lineNumber, "expected a pipe-delimited table row");
                }
                List<String> fields = splitPipeRow(trimmed);
                if (headers == null) {
                    headers = fields;
                    validateHeaders(path, headers, tableName);
                    continue;
                }
                if (fields.size() != headers.size()) {
                    throw invalid(path, lineNumber, "expected " + headers.size()
                            + " columns but found " + fields.size());
                }
                LocalDate date;
                try {
                    date = LocalDate.parse(fields.get(0));
                } catch (DateTimeParseException exception) {
                    throw invalid(path, lineNumber, "invalid date: " + fields.get(0));
                }
                Map<String, String> row = rowFor(rows, date);
                for (int index = 1; index < headers.size(); index++) {
                    put(row, headers.get(index), fields.get(index), path, lineNumber);
                }
            }
        }
        if (headers == null) {
            throw new IllegalArgumentException(path + ": table is empty");
        }
    }

    private static boolean isMarkdownSeparator(String line) {
        if (!line.startsWith("|") || !line.endsWith("|")) {
            return false;
        }
        return line.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty();
    }

    private static List<String> splitPipeRow(String line) {
        String content = line.substring(1, line.length() - 1);
        String[] fields = content.split("\\|", -1);
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            result.add(field.trim());
        }
        return result;
    }

    private static void validateHeaders(Path path, List<String> headers, String tableName) {
        if (headers.isEmpty() || !"Date".equals(headers.get(0))) {
            throw new IllegalArgumentException(path + ": " + tableName
                    + " table's first column must be Date");
        }
        for (String header : headers) {
            if (!COLUMNS.contains(header)) {
                throw new IllegalArgumentException(path + ": unsupported column: " + header);
            }
        }
    }

    private static Map<String, String> rowFor(
            Map<LocalDate, Map<String, String>> rows, LocalDate date) {
        Map<String, String> row = rows.get(date);
        if (row == null) {
            row = new LinkedHashMap<>();
            row.put("Date", date.toString());
            rows.put(date, row);
        }
        return row;
    }

    private static void put(Map<String, String> row, String column, String value,
                            Path path, int lineNumber) {
        String previous = row.put(column, value);
        if (previous != null && !previous.equals(value)) {
            throw invalid(path, lineNumber, "conflicting duplicate value for " + column);
        }
    }

    private static IllegalArgumentException invalid(Path path, int line, String reason) {
        return new IllegalArgumentException(path + ":" + line + ": " + reason);
    }

    private static void createParent(Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void writeCsv(Path output, Map<LocalDate, Map<String, String>> rows)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writeCsvRow(writer, COLUMNS);
            for (Map<String, String> row : rows.values()) {
                List<String> values = new ArrayList<>();
                for (String column : COLUMNS) {
                    values.add(row.getOrDefault(column, ""));
                }
                writeCsvRow(writer, values);
            }
        }
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            String value = values.get(index);
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }

    private static void writeHtml(Path output, Map<LocalDate, Map<String, String>> rows)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
            writer.write("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
            writer.write("<title>Powerball merged data</title><style>");
            writer.write("body{font:14px system-ui,sans-serif;margin:20px;color:#17202a}"
                    + "h1{margin-bottom:8px}.toolbar{display:flex;gap:12px;align-items:center;margin:12px 0}"
                    + "input{box-sizing:border-box;padding:7px;border:1px solid #adb5bd;border-radius:4px}"
                    + "#search{width:min(420px,70vw)}.wrap{overflow:auto;max-height:75vh;border:1px solid #ccd1d1}"
                    + "table{border-collapse:collapse;white-space:nowrap;width:100%}th,td{padding:7px 9px;"
                    + "border-bottom:1px solid #e5e7e9;text-align:left}thead{position:sticky;top:0;background:#f4f6f7;z-index:1}"
                    + "th.sortable{cursor:pointer;user-select:none}th.sortable:hover{background:#e5e7e9}"
                    + "tr.filters input{width:100%;min-width:75px;padding:4px}tbody tr:nth-child(even){background:#fafafa}"
                    + "tbody tr:hover{background:#eaf2f8}.empty{color:#aaa}</style></head><body>");
            writer.write("<h1>Powerball merged data</h1><div class=\"toolbar\"><input id=\"search\" "
                    + "placeholder=\"Search all columns...\"><span id=\"count\"></span></div>");
            writer.write("<div class=\"wrap\"><table id=\"data\"><thead><tr>");
            for (String column : COLUMNS) {
                writer.write("<th class=\"sortable\">" + html(column) + "</th>");
            }
            writer.write("</tr><tr class=\"filters\">");
            for (String column : COLUMNS) {
                writer.write("<th><input aria-label=\"Filter " + html(column)
                        + "\" placeholder=\"Filter\"></th>");
            }
            writer.write("</tr></thead><tbody>");
            for (Map<String, String> row : rows.values()) {
                writer.write("<tr>");
                for (String column : COLUMNS) {
                    String value = row.getOrDefault(column, "");
                    writer.write(value.isEmpty() ? "<td class=\"empty\"></td>"
                            : "<td>" + html(value) + "</td>");
                }
                writer.write("</tr>");
            }
            writer.write("</tbody></table></div><script>");
            writer.write("const table=document.querySelector('#data'),body=table.tBodies[0],"
                    + "rows=[...body.rows],search=document.querySelector('#search'),"
                    + "filters=[...document.querySelectorAll('.filters input')],count=document.querySelector('#count');"
                    + "function filter(){const q=search.value.toLowerCase();let shown=0;rows.forEach(r=>{"
                    + "const cells=[...r.cells].map(c=>c.textContent.toLowerCase());"
                    + "const ok=cells.join(' ').includes(q)&&filters.every((f,i)=>cells[i].includes(f.value.toLowerCase()));"
                    + "r.hidden=!ok;if(ok)shown++;});count.textContent=shown+' of '+rows.length+' rows';}"
                    + "search.addEventListener('input',filter);filters.forEach(f=>f.addEventListener('input',filter));"
                    + "let sortColumn=-1,ascending=true;document.querySelectorAll('th.sortable').forEach((h,i)=>h.onclick=()=>{"
                    + "ascending=sortColumn===i?!ascending:true;sortColumn=i;const value=r=>r.cells[i].textContent.trim();"
                    + "const parsed=v=>{const n=Number(v.replace(/[$,%]/g,''));return v!==''&&!Number.isNaN(n)?n:v;};"
                    + "rows.sort((a,b)=>{const x=parsed(value(a)),y=parsed(value(b));let c;"
                    + "if(typeof x==='number'&&typeof y==='number')c=x-y;else c=String(x).localeCompare(String(y),undefined,{numeric:true});"
                    + "return ascending?c:-c;}).forEach(r=>body.appendChild(r));filter();});filter();");
            writer.write("</script></body></html>");
        }
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
