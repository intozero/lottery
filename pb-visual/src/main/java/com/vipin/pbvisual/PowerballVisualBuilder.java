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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Left-joins reward and astronomy data onto Powerball draws using date as the key. */
public final class PowerballVisualBuilder {
    private static final DateTimeFormatter DRAW_DATE =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    private static final List<String> ASTRO_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "Observation Date", "Day Of Week", "Moon Illum %",
            "Sun House", "Sun Sign", "Sun Deg", "Sun Dignity",
            "Moon House", "Moon Sign", "Moon Deg", "Moon Dignity",
            "Mercury House", "Mercury Sign", "Mercury Deg", "Mercury Dignity",
            "Venus House", "Venus Sign", "Venus Deg", "Venus Dignity",
            "Mars House", "Mars Sign", "Mars Deg", "Mars Dignity",
            "Jupiter House", "Jupiter Sign", "Jupiter Deg", "Jupiter Dignity",
            "Saturn House", "Saturn Sign", "Saturn Deg", "Saturn Dignity",
            "Rahu House", "Rahu Sign", "Rahu Deg", "Rahu Dignity"));

    private static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "Date", "White Ball 1", "White Ball 2", "White Ball 3", "White Ball 4",
            "White Ball 5", "Powerball", "White Ball Sum", "White Ball Mean",
            "Jackpot (Annuity)", "Day Of Week", "Moon Illum %",
            "Sun House", "Sun Sign", "Sun Deg", "Sun Dignity",
            "Moon House", "Moon Sign", "Moon Deg", "Moon Dignity",
            "Mercury House", "Mercury Sign", "Mercury Deg", "Mercury Dignity",
            "Venus House", "Venus Sign", "Venus Deg", "Venus Dignity",
            "Mars House", "Mars Sign", "Mars Deg", "Mars Dignity",
            "Jupiter House", "Jupiter Sign", "Jupiter Deg", "Jupiter Dignity",
            "Saturn House", "Saturn Sign", "Saturn Deg", "Saturn Dignity",
            "Rahu House", "Rahu Sign", "Rahu Deg", "Rahu Dignity"));

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
        Path astro = args.length == 5 ? Paths.get(args[1]) : dataDirectory.resolve("astro_info.txt");
        Path rewards = args.length == 5 ? Paths.get(args[2]) : dataDirectory.resolve("reward.txt");
        Path csv = args.length == 5 ? Paths.get(args[3]) : dataDirectory.resolve("pb-merged.csv");
        Path html = args.length == 5 ? Paths.get(args[4]) : dataDirectory.resolve("pb-merged.html");

        try {
            Map<LocalDate, Map<String, String>> rows =
                    new TreeMap<>(Collections.reverseOrder());
            int conflictingDraws = readDraws(draws, rows);
            readPipeTable(rewards, rows, "reward");
            readAstroInfo(astro, rows);
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
                    int whiteBallSum = 0;
                    for (int index = 1; index <= 5; index++) {
                        whiteBallSum += Integer.parseInt(fields[index]);
                        row.put("White Ball " + index, fields[index]);
                    }
                    row.put("White Ball Sum", Integer.toString(whiteBallSum));
                    row.put("White Ball Mean",
                            String.format(Locale.ROOT, "%.1f", whiteBallSum / 5.0));
                    if (fields.length == 7) {
                        row.put("Powerball", fields[6]);
                    } else {
                        row.remove("Powerball");
                    }
                } catch (DateTimeParseException | NumberFormatException exception) {
                    throw invalid(path, lineNumber, "invalid draw data: " + exception.getMessage());
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
                Map<String, String> row = rows.get(date);
                if (row == null) {
                    // The draw file is the master: ignore non-draw dates.
                    continue;
                }
                for (int index = 1; index < headers.size(); index++) {
                    put(row, headers.get(index), fields.get(index), path, lineNumber);
                }
            }
        }
        if (headers == null) {
            throw new IllegalArgumentException(path + ": table is empty");
        }
    }

    private static void readAstroInfo(Path path, Map<LocalDate, Map<String, String>> rows)
            throws IOException {
        String header = null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (header == null) {
                    if (line.startsWith("Observation Date")) {
                        header = line;
                        validateAstroHeader(path, header);
                    }
                    continue;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("Process finished")) {
                    continue;
                }

                List<String> fields = parseAstroRow(path, lineNumber, line);
                LocalDate date;
                try {
                    date = LocalDate.parse(fields.get(0));
                } catch (DateTimeParseException exception) {
                    throw invalid(path, lineNumber, "invalid observation date: " + fields.get(0));
                }

                Map<String, String> row = rows.get(date);
                if (row == null) {
                    continue;
                }
                for (int index = 1; index < ASTRO_COLUMNS.size(); index++) {
                    put(row, ASTRO_COLUMNS.get(index), fields.get(index), path, lineNumber);
                }
            }
        }
        if (header == null) {
            throw new IllegalArgumentException(path + ": Observation Date header was not found");
        }
    }

    private static void validateAstroHeader(Path path, String header) {
        int searchFrom = 0;
        for (String column : ASTRO_COLUMNS) {
            int start = header.indexOf(column, searchFrom);
            if (start < 0) {
                throw new IllegalArgumentException(path + ": missing astronomy column: " + column);
            }
            searchFrom = start + column.length();
        }
    }

    private static List<String> parseAstroRow(Path path, int lineNumber, String line) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length < 27) {
            throw invalid(path, lineNumber, "astronomy row has too few values");
        }

        List<String> fields = new ArrayList<>();
        fields.add(tokens[0]);
        fields.add(tokens[1]);
        fields.add(tokens[2]);

        int token = 3;
        for (int planet = 0; planet < 8; planet++) {
            if (token + 2 >= tokens.length
                    || !isInteger(tokens[token]) || !isInteger(tokens[token + 2])) {
                throw invalid(path, lineNumber,
                        "invalid house/sign/degree values for " + ASTRO_COLUMNS.get(3 + planet * 4));
            }
            fields.add(tokens[token++]);
            fields.add(tokens[token++]);
            fields.add(tokens[token++]);

            String dignity = "";
            if (token < tokens.length && !isInteger(tokens[token])) {
                dignity = tokens[token++];
            }
            fields.add(dignity);
        }
        if (token != tokens.length) {
            throw invalid(path, lineNumber, "unexpected extra astronomy values");
        }
        return fields;
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
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
            writer.write(":root{--navy:#111c44;--blue:#3157d5;--cyan:#35b9dc;--ink:#18213d;--muted:#667085;"
                    + "--line:#e5e9f2;--panel:rgba(255,255,255,.94)}*{box-sizing:border-box}"
                    + "body{font:14px Inter,ui-sans-serif,system-ui,sans-serif;margin:0;color:var(--ink);"
                    + "background:linear-gradient(135deg,#edf3ff 0%,#f7f9ff 45%,#eefbff 100%);min-height:100vh}"
                    + ".shell{max-width:1800px;margin:auto;padding:28px}.hero{position:relative;overflow:hidden;color:white;"
                    + "background:linear-gradient(120deg,var(--navy),#243f9a 65%,#147fa3);padding:26px 30px;border-radius:18px;"
                    + "box-shadow:0 14px 40px rgba(28,52,120,.22)}.hero:after{content:'';position:absolute;width:260px;"
                    + "height:260px;border-radius:50%;right:-70px;top:-130px;background:rgba(255,255,255,.09)}"
                    + "h1{font-size:28px;margin:0 0 5px}.subtitle{margin:0;opacity:.78}.cards{display:flex;gap:12px;"
                    + "flex-wrap:wrap;margin:18px 0}.card{min-width:145px;background:var(--panel);padding:14px 18px;"
                    + "border:1px solid rgba(255,255,255,.8);border-radius:13px;box-shadow:0 7px 22px rgba(37,55,100,.08)}"
                    + ".card strong{display:block;font-size:22px;color:var(--blue)}.card span{color:var(--muted);font-size:12px}"
                    + ".toolbar{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:14px 0}input,button{font:inherit}"
                    + "input{padding:9px 11px;border:1px solid #cdd5e5;border-radius:8px;background:white;outline:none;"
                    + "transition:.15s}input:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgba(49,87,213,.12)}"
                    + "input.active{border-color:var(--cyan);background:#effcff}#search{width:min(480px,100%);padding:11px 13px}"
                    + "button{padding:10px 15px;border:0;border-radius:8px;color:white;background:var(--blue);cursor:pointer;"
                    + "box-shadow:0 4px 12px rgba(49,87,213,.2)}button:hover{filter:brightness(1.08)}"
                    + ".hint{color:var(--muted);font-size:12px}.wrap{overflow:auto;max-height:72vh;border:1px solid var(--line);"
                    + "border-radius:14px;background:white;box-shadow:0 12px 35px rgba(31,52,110,.1)}table{border-collapse:separate;"
                    + "border-spacing:0;white-space:nowrap;width:100%}th,td{padding:9px 11px;border-bottom:1px solid var(--line);"
                    + "text-align:left}thead{position:sticky;top:0;z-index:2}thead tr:first-child th{background:#eef2fb;color:#25355f;"
                    + "font-size:12px;text-transform:uppercase;letter-spacing:.03em}th.sortable{cursor:pointer;user-select:none}"
                    + "th.sortable:after{content:'  <>';color:#9aa6c3}th.sortable.asc:after{content:'  ^';color:var(--blue)}"
                    + "th.sortable.desc:after{content:'  v';color:var(--blue)}th.sortable:hover{background:#e1e8f8}"
                    + "tr.filters th{background:#f8faff;padding:6px}tr.filters input{width:100%;min-width:82px;padding:6px 7px}"
                    + "tr.filters input.numeric{min-width:105px}tbody tr:nth-child(even){background:#fafcff}"
                    + "tbody tr:hover{background:#eaf6ff}td:nth-child(8),td:nth-child(9){font-weight:700;color:#244bbd}"
                    + ".empty{color:#bec5d2}@media(max-width:700px){.shell{padding:14px}.hero{padding:20px}h1{font-size:22px}}"
                    + "</style></head><body><main class=\"shell\">");
            writer.write("<section class=\"hero\"><h1>Powerball Data Explorer</h1>"
                    + "<p class=\"subtitle\">Draw results, rewards, and astronomy data joined by date</p></section>"
                    + "<section class=\"cards\"><div class=\"card\"><strong id=\"visibleCount\">0</strong><span>Visible rows</span></div>"
                    + "<div class=\"card\"><strong id=\"totalCount\">0</strong><span>Total rows</span></div>"
                    + "<div class=\"card\"><strong id=\"activeCount\">0</strong><span>Active filters</span></div></section>"
                    + "<div class=\"toolbar\"><input id=\"search\" placeholder=\"Search all columns...\">"
                    + "<button id=\"reset\" type=\"button\">Reset filters</button>"
                    + "<span class=\"hint\">Numeric filters support &gt; 100, &gt;= 150, &lt; 200, or = 175</span></div>");
            writer.write("<div class=\"wrap\"><table id=\"data\"><thead><tr>");
            for (String column : COLUMNS) {
                writer.write("<th class=\"sortable\">" + html(column) + "</th>");
            }
            writer.write("</tr><tr class=\"filters\">");
            for (String column : COLUMNS) {
                boolean numeric = "White Ball Sum".equals(column) || "White Ball Mean".equals(column);
                writer.write("<th><input aria-label=\"Filter " + html(column)
                        + "\" class=\"" + (numeric ? "numeric" : "") + "\" placeholder=\""
                        + (numeric ? "> 100" : "Filter") + "\"></th>");
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
            writer.write("</tbody></table></div></main><script>");
            writer.write("const table=document.querySelector('#data'),body=table.tBodies[0],"
                    + "rows=[...body.rows],search=document.querySelector('#search'),"
                    + "filters=[...document.querySelectorAll('.filters input')],visible=document.querySelector('#visibleCount'),"
                    + "total=document.querySelector('#totalCount'),active=document.querySelector('#activeCount');"
                    + "total.textContent=rows.length;function matches(value,query){const q=query.trim().toLowerCase();if(!q)return true;"
                    + "const m=q.match(/^(>=|<=|>|<|=)\\s*(-?\\d+(?:\\.\\d+)?)$/);if(m){"
                    + "const n=Number(value.replace(/[$,%]/g,''));if(Number.isNaN(n))return false;const target=Number(m[2]);"
                    + "return m[1]==='>'?n>target:m[1]==='>='?n>=target:m[1]==='<'?n<target:m[1]==='<='?n<=target:n===target;}"
                    + "return value.toLowerCase().includes(q);}function filter(){const q=search.value.toLowerCase();let shown=0;rows.forEach(r=>{"
                    + "const cells=[...r.cells].map(c=>c.textContent.toLowerCase());"
                    + "const ok=cells.join(' ').includes(q)&&filters.every((f,i)=>matches(cells[i],f.value));"
                    + "r.hidden=!ok;if(ok)shown++;});filters.forEach(f=>f.classList.toggle('active',!!f.value.trim()));"
                    + "search.classList.toggle('active',!!search.value.trim());visible.textContent=shown;"
                    + "active.textContent=filters.filter(f=>f.value.trim()).length+(search.value.trim()?1:0);}"
                    + "search.addEventListener('input',filter);filters.forEach(f=>f.addEventListener('input',filter));"
                    + "document.querySelector('#reset').onclick=()=>{search.value='';filters.forEach(f=>f.value='');filter();};"
                    + "let sortColumn=-1,ascending=true;document.querySelectorAll('th.sortable').forEach((h,i)=>h.onclick=()=>{"
                    + "ascending=sortColumn===i?!ascending:true;sortColumn=i;document.querySelectorAll('th.sortable')"
                    + ".forEach(x=>x.classList.remove('asc','desc'));h.classList.add(ascending?'asc':'desc');"
                    + "const value=r=>r.cells[i].textContent.trim();"
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
