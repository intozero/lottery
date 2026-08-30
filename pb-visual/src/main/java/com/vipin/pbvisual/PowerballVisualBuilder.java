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
            "Observation Date", "Day Of Week", "Ascendant Sign", "Ascendant Deg",
            "Moon Illum %", "Moon Phase", "Moon Nakshatra",
            "Moon Nakshatra (ML)", "Nakshatra No", "Pada",
            "Sun Sign", "Sun Deg", "Sun House", "Sun Dignity",
            "Moon Sign", "Moon Deg", "Moon House", "Moon Dignity",
            "Mercury Sign", "Mercury Deg", "Mercury House", "Mercury Dignity",
            "Venus Sign", "Venus Deg", "Venus House", "Venus Dignity",
            "Mars Sign", "Mars Deg", "Mars House", "Mars Dignity",
            "Jupiter Sign", "Jupiter Deg", "Jupiter House", "Jupiter Dignity",
            "Saturn Sign", "Saturn Deg", "Saturn House", "Saturn Dignity",
            "Rahu Sign", "Rahu Deg", "Rahu House", "Rahu Dignity"));

    private static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "Date", "White Ball 1", "White Ball 2", "White Ball 3", "White Ball 4",
            "White Ball 5", "Powerball", "White Ball Sum", "White Ball Mean",
            "Jackpot (Annuity)", "Day Of Week", "Ascendant Sign", "Ascendant Deg",
            "Moon Illum %", "Moon Phase", "Moon Nakshatra",
            "Moon Nakshatra (ML)", "Nakshatra No", "Pada",
            "Sun Sign", "Sun Deg", "Sun House", "Sun Dignity",
            "Moon Sign", "Moon Deg", "Moon House", "Moon Dignity",
            "Mercury Sign", "Mercury Deg", "Mercury House", "Mercury Dignity",
            "Venus Sign", "Venus Deg", "Venus House", "Venus Dignity",
            "Mars Sign", "Mars Deg", "Mars House", "Mars Dignity",
            "Jupiter Sign", "Jupiter Deg", "Jupiter House", "Jupiter Dignity",
            "Saturn Sign", "Saturn Deg", "Saturn House", "Saturn Dignity",
            "Rahu Sign", "Rahu Deg", "Rahu House", "Rahu Dignity"));

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
        Path chart = args.length == 5
                ? chartPathFor(html) : dataDirectory.resolve("pb-whiteball-sum-chart.html");

        try {
            Map<LocalDate, Map<String, String>> rows =
                    new TreeMap<>(Collections.reverseOrder());
            int conflictingDraws = readDraws(draws, rows);
            readPipeTable(rewards, rows, "reward");
            readAstroInfo(astro, rows);
            createParent(csv);
            createParent(html);
            createParent(chart);
            writeCsv(csv, rows);
            writeHtml(html, rows);
            writeSumChart(chart, rows);
            System.out.println("Merged " + rows.size() + " dates.");
            if (conflictingDraws > 0) {
                System.out.println("Resolved " + conflictingDraws
                        + " conflicting duplicate draw rows by keeping the last row per date.");
            }
            System.out.println("CSV:  " + csv.toAbsolutePath());
            System.out.println("HTML: " + html.toAbsolutePath());
            System.out.println("Chart: " + chart.toAbsolutePath());
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
        List<String> headers = null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = parseCsvLine(path, lineNumber, line);
                if (headers == null) {
                    headers = fields;
                    validateAstroHeaders(path, headers);
                    continue;
                }
                if (fields.size() != headers.size()) {
                    throw invalid(path, lineNumber, "expected " + headers.size()
                            + " CSV fields but found " + fields.size());
                }
                LocalDate date;
                try {
                    date = LocalDate.parse(fields.get(headers.indexOf("Observation Date")));
                } catch (DateTimeParseException exception) {
                    throw invalid(path, lineNumber, "invalid observation date: " + fields.get(0));
                }

                Map<String, String> row = rows.get(date);
                if (row == null) {
                    continue;
                }
                for (int index = 0; index < headers.size(); index++) {
                    String header = headers.get(index);
                    if (!"Observation Date".equals(header)) {
                        put(row, header, fields.get(index), path, lineNumber);
                    }
                }
            }
        }
        if (headers == null) {
            throw new IllegalArgumentException(path + ": CSV header was not found");
        }
    }

    private static void validateAstroHeaders(Path path, List<String> headers) {
        if (headers.size() != ASTRO_COLUMNS.size()) {
            throw new IllegalArgumentException(path + ": expected " + ASTRO_COLUMNS.size()
                    + " astronomy columns but found " + headers.size());
        }
        for (String column : ASTRO_COLUMNS) {
            if (!headers.contains(column)) {
                throw new IllegalArgumentException(path + ": missing astronomy column: " + column);
            }
        }
    }

    private static List<String> parseCsvLine(Path path, int lineNumber, String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            throw invalid(path, lineNumber, "unclosed quoted CSV field");
        }
        fields.add(field.toString().trim());
        return fields;
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

    private static Path chartPathFor(Path html) {
        String fileName = html.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String base = extension > 0 ? fileName.substring(0, extension) : fileName;
        return html.resolveSibling(base + "-sum-chart.html");
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

    private static void writeSumChart(Path output, Map<LocalDate, Map<String, String>> rows)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>Powerball white-ball sum curve</title><style>"
                    + ":root{--navy:#101b42;--blue:#4169e1;--cyan:#22b8cf;--ink:#17213c;--muted:#6b7280}"
                    + "*{box-sizing:border-box}body{margin:0;min-height:100vh;font:14px Inter,system-ui,sans-serif;color:var(--ink);"
                    + "background:radial-gradient(circle at top right,#dff8ff,transparent 38%),linear-gradient(135deg,#edf2ff,#fafcff)}"
                    + ".shell{max-width:1500px;margin:auto;padding:28px}.hero{color:white;padding:25px 29px;border-radius:18px;"
                    + "background:linear-gradient(120deg,var(--navy),#2947a5 68%,#1686a5);box-shadow:0 16px 42px rgba(27,47,110,.24)}"
                    + "h1{margin:0 0 5px;font-size:28px}.hero p{margin:0;opacity:.78}.toolbar{display:flex;gap:12px;"
                    + "align-items:center;flex-wrap:wrap;margin:18px 0}.toolbar label{display:flex;gap:7px;align-items:center}"
                    + "select{padding:9px 12px;border:1px solid #ccd4e5;border-radius:8px;background:white}.cards{display:flex;"
                    + "gap:10px;flex-wrap:wrap;margin-left:auto}.card{min-width:110px;padding:9px 14px;background:white;border-radius:10px;"
                    + "box-shadow:0 5px 18px rgba(32,52,105,.08)}.card strong{display:block;color:var(--blue);font-size:18px}"
                    + ".card span{font-size:11px;color:var(--muted)}.chart{position:relative;background:rgba(255,255,255,.95);"
                    + "border:1px solid #e4e8f1;border-radius:16px;padding:14px;box-shadow:0 14px 38px rgba(31,51,100,.11)}"
                    + ".category{margin-top:20px}.category-head{display:flex;align-items:center;justify-content:space-between;"
                    + "gap:12px;flex-wrap:wrap;margin:2px 10px 12px 52px}.category-head h2{margin:0;font-size:18px}"
                    + "canvas{display:block;width:100%;height:560px}.legend{display:flex;gap:18px;margin:2px 0 8px 52px;color:var(--muted)}"
                    + ".key:before{content:'';display:inline-block;width:22px;height:3px;margin-right:7px;vertical-align:middle;"
                    + "background:var(--blue);border-radius:3px}.key.trend:before{background:#ef7d32}.tooltip{position:absolute;display:none;"
                    + "pointer-events:none;padding:9px 11px;color:white;background:rgba(16,27,66,.94);border-radius:8px;"
                    + "box-shadow:0 7px 20px rgba(0,0,0,.2);transform:translate(10px,-105%);line-height:1.5}"
                    + "@media(max-width:700px){.shell{padding:13px}.hero{padding:20px}h1{font-size:21px}.cards{margin-left:0}"
                    + "canvas{height:430px}}</style></head><body><main class=\"shell\">"
                    + "<section class=\"hero\"><h1>White-Ball Sum Curve</h1>"
                    + "<p>How the sum of the five Powerball white balls changes across drawing dates</p></section>"
                    + "<div class=\"toolbar\"><label>Time range <select id=\"range\"><option value=\"1\">Last year</option>"
                    + "<option value=\"3\">Last 3 years</option><option value=\"5\">Last 5 years</option>"
                    + "<option value=\"all\" selected>All dates</option></select></label>"
                    + "<label><input id=\"trend\" type=\"checkbox\" checked> Show 20-draw moving average</label>"
                    + "<div class=\"cards\"><div class=\"card\"><strong id=\"points\">0</strong><span>Draws</span></div>"
                    + "<div class=\"card\"><strong id=\"average\">0</strong><span>Average sum</span></div>"
                    + "<div class=\"card\"><strong id=\"minimum\">0</strong><span>Minimum</span></div>"
                    + "<div class=\"card\"><strong id=\"maximum\">0</strong><span>Maximum</span></div></div></div>"
                    + "<section class=\"chart\"><div class=\"legend\"><span class=\"key\">White-ball sum</span>"
                    + "<span class=\"key trend\">20-draw average</span></div><canvas id=\"canvas\"></canvas>"
                    + "<div id=\"tooltip\" class=\"tooltip\"></div></section>"
                    + "<section class=\"chart category\"><div class=\"category-head\"><h2>Average Sum by Nakshatra</h2>"
                    + "<label>Category <select id=\"categoryBy\"><option value=\"ml\">Malayalam Nakshatra</option>"
                    + "<option value=\"number\">Nakshatra Number</option></select></label></div>"
                    + "<canvas id=\"categoryCanvas\"></canvas><div id=\"categoryTooltip\" class=\"tooltip\"></div>"
                    + "</section></main><script>const all=[");

            boolean first = true;
            for (Map<String, String> row : rows.values()) {
                String sum = row.get("White Ball Sum");
                if (sum == null || sum.isEmpty()) {
                    continue;
                }
                if (!first) {
                    writer.write(',');
                }
                writer.write("{d:\"" + js(row.get("Date")) + "\",s:" + sum
                        + ",ml:\"" + js(row.getOrDefault("Moon Nakshatra (ML)", ""))
                        + "\",number:\"" + js(row.getOrDefault("Nakshatra No", "")) + "\"}");
                first = false;
            }

            writer.write("].map(x=>({...x,t:new Date(x.d+'T00:00:00').getTime()})).sort((a,b)=>a.t-b.t);"
                    + "const canvas=document.querySelector('#canvas'),ctx=canvas.getContext('2d'),tip=document.querySelector('#tooltip'),"
                    + "range=document.querySelector('#range'),trend=document.querySelector('#trend');let data=[],coords=[];"
                    + "function selectData(){if(range.value==='all')data=all;else{const end=new Date(all[all.length-1].t),"
                    + "start=new Date(end);start.setFullYear(end.getFullYear()-Number(range.value));data=all.filter(x=>x.t>=start.getTime());}"
                    + "const sums=data.map(x=>x.s),avg=sums.reduce((a,b)=>a+b,0)/sums.length;"
                    + "document.querySelector('#points').textContent=data.length;document.querySelector('#average').textContent=avg.toFixed(1);"
                    + "document.querySelector('#minimum').textContent=Math.min(...sums);document.querySelector('#maximum').textContent=Math.max(...sums);draw();}"
                    + "function draw(){const rect=canvas.getBoundingClientRect(),dpr=devicePixelRatio||1;canvas.width=rect.width*dpr;"
                    + "canvas.height=rect.height*dpr;ctx.setTransform(dpr,0,0,dpr,0,0);const w=rect.width,h=rect.height,p={l:58,r:24,t:22,b:43};"
                    + "ctx.clearRect(0,0,w,h);if(!data.length)return;const minT=data[0].t,maxT=data[data.length-1].t||minT+1;"
                    + "const sums=data.map(x=>x.s),rawMin=Math.min(...sums),rawMax=Math.max(...sums),minY=Math.floor((rawMin-10)/20)*20,"
                    + "maxY=Math.ceil((rawMax+10)/20)*20,x=t=>p.l+(t-minT)/(maxT-minT||1)*(w-p.l-p.r),"
                    + "y=v=>p.t+(maxY-v)/(maxY-minY)*(h-p.t-p.b);ctx.font='12px system-ui';ctx.fillStyle='#667085';"
                    + "ctx.strokeStyle='#e6eaf3';ctx.lineWidth=1;for(let i=0;i<=5;i++){const value=minY+(maxY-minY)*i/5,py=y(value);"
                    + "ctx.beginPath();ctx.moveTo(p.l,py);ctx.lineTo(w-p.r,py);ctx.stroke();ctx.fillText(Math.round(value),12,py+4);}"
                    + "for(let i=0;i<=6;i++){const t=minT+(maxT-minT)*i/6,px=x(t),label=new Date(t).toLocaleDateString(undefined,{year:'numeric',month:'short'});"
                    + "ctx.fillText(label,Math.max(p.l,Math.min(px-22,w-p.r-45)),h-14);}coords=data.map(d=>({x:x(d.t),y:y(d.s),d}));"
                    + "ctx.beginPath();coords.forEach((c,i)=>i?ctx.lineTo(c.x,c.y):ctx.moveTo(c.x,c.y));ctx.strokeStyle='#4169e1';"
                    + "ctx.lineWidth=2;ctx.lineJoin='round';ctx.stroke();if(trend.checked&&data.length>=20){ctx.beginPath();"
                    + "for(let i=19;i<data.length;i++){let total=0;for(let j=i-19;j<=i;j++)total+=data[j].s;const px=x(data[i].t),py=y(total/20);"
                    + "i===19?ctx.moveTo(px,py):ctx.lineTo(px,py);}ctx.strokeStyle='#ef7d32';ctx.lineWidth=2.5;ctx.stroke();}}"
                    + "canvas.addEventListener('mousemove',e=>{if(!coords.length)return;const r=canvas.getBoundingClientRect(),mx=e.clientX-r.left;"
                    + "let best=coords[0];for(const c of coords)if(Math.abs(c.x-mx)<Math.abs(best.x-mx))best=c;tip.style.display='block';"
                    + "tip.style.left=best.x+'px';tip.style.top=best.y+'px';tip.innerHTML='<strong>'+best.d.d+'</strong><br>White-ball sum: '+best.d.s;});"
                    + "canvas.addEventListener('mouseleave',()=>tip.style.display='none');range.addEventListener('change',selectData);"
                    + "trend.addEventListener('change',draw);"
                    + "const categoryCanvas=document.querySelector('#categoryCanvas'),categoryCtx=categoryCanvas.getContext('2d'),"
                    + "categoryBy=document.querySelector('#categoryBy'),categoryTip=document.querySelector('#categoryTooltip');let categoryCoords=[];"
                    + "function categoryData(){const key=categoryBy.value,groups=new Map();all.forEach(d=>{const label=d[key];if(!label)return;"
                    + "const g=groups.get(label)||{label,total:0,count:0};g.total+=d.s;g.count++;groups.set(label,g);});"
                    + "const values=[...groups.values()].map(g=>({...g,average:g.total/g.count}));"
                    + "values.sort((a,b)=>b.average-a.average||a.label.localeCompare(b.label,undefined,{numeric:true}));return values;}"
                    + "function drawCategory(){const values=categoryData(),rect=categoryCanvas.getBoundingClientRect(),dpr=devicePixelRatio||1;"
                    + "categoryCanvas.width=rect.width*dpr;categoryCanvas.height=rect.height*dpr;categoryCtx.setTransform(dpr,0,0,dpr,0,0);"
                    + "const w=rect.width,h=rect.height,p={l:58,r:24,t:22,b:105};categoryCtx.clearRect(0,0,w,h);if(!values.length)return;"
                    + "const averages=values.map(x=>x.average),minY=Math.floor((Math.min(...averages)-10)/20)*20,"
                    + "maxY=Math.ceil((Math.max(...averages)+10)/20)*20,y=v=>p.t+(maxY-v)/(maxY-minY||1)*(h-p.t-p.b),"
                    + "slot=(w-p.l-p.r)/values.length,bar=Math.max(5,Math.min(34,slot*.7));categoryCtx.font='12px system-ui';"
                    + "categoryCtx.fillStyle='#667085';categoryCtx.strokeStyle='#e6eaf3';for(let i=0;i<=5;i++){const value=minY+(maxY-minY)*i/5,py=y(value);"
                    + "categoryCtx.beginPath();categoryCtx.moveTo(p.l,py);categoryCtx.lineTo(w-p.r,py);categoryCtx.stroke();"
                    + "categoryCtx.fillText(Math.round(value),12,py+4);}categoryCoords=[];values.forEach((d,i)=>{const x=p.l+slot*(i+.5),top=y(d.average);"
                    + "categoryCtx.fillStyle='#4169e1';categoryCtx.fillRect(x-bar/2,top,bar,h-p.b-top);categoryCtx.save();"
                    + "categoryCtx.translate(x+3,h-p.b+8);categoryCtx.rotate(-Math.PI/4);categoryCtx.fillStyle='#667085';"
                    + "categoryCtx.textAlign='right';categoryCtx.fillText(d.label,0,0);categoryCtx.restore();categoryCoords.push({x,y:top,d});});}"
                    + "categoryCanvas.addEventListener('mousemove',e=>{if(!categoryCoords.length)return;const r=categoryCanvas.getBoundingClientRect(),mx=e.clientX-r.left;"
                    + "let best=categoryCoords[0];for(const c of categoryCoords)if(Math.abs(c.x-mx)<Math.abs(best.x-mx))best=c;"
                    + "categoryTip.style.display='block';categoryTip.style.left=best.x+'px';categoryTip.style.top=best.y+'px';"
                    + "categoryTip.innerHTML='<strong>'+best.d.label+'</strong><br>Average sum: '+best.d.average.toFixed(1)+'<br>Draws: '+best.d.count;});"
                    + "categoryCanvas.addEventListener('mouseleave',()=>categoryTip.style.display='none');categoryBy.addEventListener('change',drawCategory);"
                    + "addEventListener('resize',()=>{draw();drawCategory();});selectData();drawCategory();</script></body></html>");
        }
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
                    + ".column-picker{position:relative}.column-picker summary{list-style:none;padding:10px 15px;border-radius:8px;"
                    + "color:white;background:#263867;cursor:pointer;box-shadow:0 4px 12px rgba(38,56,103,.18)}"
                    + ".column-picker summary::-webkit-details-marker{display:none}.column-menu{position:absolute;z-index:10;top:46px;"
                    + "left:0;width:min(620px,90vw);max-height:420px;overflow:auto;padding:14px;background:white;border:1px solid var(--line);"
                    + "border-radius:12px;box-shadow:0 18px 45px rgba(24,33,61,.22)}.column-actions{display:flex;gap:8px;"
                    + "position:sticky;top:-14px;background:white;padding:0 0 10px}.column-actions button{padding:7px 11px}"
                    + ".column-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(155px,1fr));gap:5px 12px}"
                    + ".column-grid label{display:flex;gap:7px;align-items:center;padding:5px;border-radius:6px;cursor:pointer}"
                    + ".column-grid label:hover{background:#eef4ff}.column-grid input{padding:0;margin:0;accent-color:var(--blue)}"
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
                    + "thead th:first-child,tbody td:first-child{position:sticky;left:0;z-index:1;background:#fff;box-shadow:2px 0 0 #e5e9f2}"
                    + "thead tr:first-child th:first-child{z-index:4;background:#e8eefb}thead tr.filters th:first-child{z-index:3;background:#f8faff}"
                    + ".empty{color:#bec5d2}@media(max-width:700px){.shell{padding:14px}.hero{padding:20px}h1{font-size:22px}}"
                    + "</style></head><body><main class=\"shell\">");
            writer.write("<section class=\"hero\"><h1>Powerball Data Explorer</h1>"
                    + "<p class=\"subtitle\">Draw results, rewards, and astronomy data joined by date</p></section>"
                    + "<section class=\"cards\"><div class=\"card\"><strong id=\"visibleCount\">0</strong><span>Visible rows</span></div>"
                    + "<div class=\"card\"><strong id=\"totalCount\">0</strong><span>Total rows</span></div>"
                    + "<div class=\"card\"><strong id=\"activeCount\">0</strong><span>Active filters</span></div>"
                    + "<div class=\"card\"><strong id=\"selectedCount\">0</strong><span>Visible columns</span></div></section>"
                    + "<div class=\"toolbar\"><input id=\"search\" placeholder=\"Search all columns...\">"
                    + "<button id=\"reset\" type=\"button\">Reset filters</button>"
                    + "<details class=\"column-picker\"><summary>Choose columns</summary><div class=\"column-menu\">"
                    + "<div class=\"column-actions\"><button id=\"selectAll\" type=\"button\">Select all</button>"
                    + "<button id=\"selectNone\" type=\"button\">Select none</button></div>"
                    + "<div id=\"columnGrid\" class=\"column-grid\"></div></div></details>"
                    + "<span class=\"hint\">Numeric filters support &gt; 100, &gt;= 150, &lt; 200, or = 175</span></div>");
            writer.write("<div class=\"wrap\"><table id=\"data\"><thead><tr>");
            for (String column : COLUMNS) {
                writer.write("<th class=\"sortable\">" + html(column) + "</th>");
            }
            writer.write("</tr><tr class=\"filters\">");
            for (String column : COLUMNS) {
                boolean numeric = isNumericColumn(column);
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
                    + "total=document.querySelector('#totalCount'),active=document.querySelector('#activeCount'),"
                    + "selected=document.querySelector('#selectedCount'),headers=[...document.querySelectorAll('th.sortable')],"
                    + "grid=document.querySelector('#columnGrid');let saved=null;try{saved=JSON.parse(localStorage.getItem('pb-visible-columns-v2'));}catch(e){}"
                    + "const columnChecks=headers.map((h,i)=>{const label=document.createElement('label'),box=document.createElement('input');"
                    + "box.type='checkbox';box.checked=!Array.isArray(saved)||saved.includes(h.textContent);label.append(box,document.createTextNode(h.textContent));"
                    + "grid.append(label);box.addEventListener('change',()=>applyColumns(true));return box;});"
                    + "total.textContent=rows.length;function matches(value,query){const q=query.trim().toLowerCase();if(!q)return true;"
                    + "const m=q.match(/^(>=|<=|>|<|=)\\s*(-?\\d+(?:\\.\\d+)?)$/);if(m){"
                    + "const n=Number(value.replace(/[$,%]/g,''));if(Number.isNaN(n))return false;const target=Number(m[2]);"
                    + "return m[1]==='>'?n>target:m[1]==='>='?n>=target:m[1]==='<'?n<target:m[1]==='<='?n<=target:n===target;}"
                    + "return value.toLowerCase().includes(q);}function filter(){const q=search.value.toLowerCase();let shown=0;rows.forEach(r=>{"
                    + "const cells=[...r.cells].map(c=>c.textContent.toLowerCase());"
                    + "const searchable=cells.filter((c,i)=>columnChecks[i].checked).join(' ');"
                    + "const ok=searchable.includes(q)&&filters.every((f,i)=>!columnChecks[i].checked||matches(cells[i],f.value));"
                    + "r.hidden=!ok;if(ok)shown++;});filters.forEach(f=>f.classList.toggle('active',!!f.value.trim()));"
                    + "search.classList.toggle('active',!!search.value.trim());visible.textContent=shown;"
                    + "active.textContent=filters.filter((f,i)=>columnChecks[i].checked&&f.value.trim()).length+(search.value.trim()?1:0);}"
                    + "function applyColumns(save){columnChecks.forEach((box,i)=>{if(!box.checked)filters[i].value='';"
                    + "table.querySelectorAll('tr').forEach(r=>{if(r.cells[i])r.cells[i].hidden=!box.checked;});});"
                    + "selected.textContent=columnChecks.filter(x=>x.checked).length;if(save){try{localStorage.setItem('pb-visible-columns-v2',"
                    + "JSON.stringify(columnChecks.map((x,i)=>x.checked?headers[i].textContent:null).filter(x=>x!==null)));}catch(e){}}filter();}"
                    + "search.addEventListener('input',filter);filters.forEach(f=>f.addEventListener('input',filter));"
                    + "document.querySelector('#reset').onclick=()=>{search.value='';filters.forEach(f=>f.value='');filter();};"
                    + "document.querySelector('#selectAll').onclick=()=>{columnChecks.forEach(x=>x.checked=true);applyColumns(true);};"
                    + "document.querySelector('#selectNone').onclick=()=>{columnChecks.forEach(x=>x.checked=false);applyColumns(true);};"
                    + "let sortColumn=-1,ascending=true;document.querySelectorAll('th.sortable').forEach((h,i)=>h.onclick=()=>{"
                    + "ascending=sortColumn===i?!ascending:true;sortColumn=i;document.querySelectorAll('th.sortable')"
                    + ".forEach(x=>x.classList.remove('asc','desc'));h.classList.add(ascending?'asc':'desc');"
                    + "const value=r=>r.cells[i].textContent.trim();"
                    + "const parsed=v=>{const n=Number(v.replace(/[$,%]/g,''));return v!==''&&!Number.isNaN(n)?n:v;};"
                    + "rows.sort((a,b)=>{const x=parsed(value(a)),y=parsed(value(b));let c;"
                    + "if(typeof x==='number'&&typeof y==='number')c=x-y;else c=String(x).localeCompare(String(y),undefined,{numeric:true});"
                    + "return ascending?c:-c;}).forEach(r=>body.appendChild(r));filter();});applyColumns(false);");
            writer.write("</script></body></html>");
        }
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String js(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("<", "\\u003c");
    }

    private static boolean isNumericColumn(String column) {
        return column.startsWith("White Ball ") || "Powerball".equals(column)
                || "Moon Illum %".equals(column) || "Nakshatra No".equals(column)
                || "Pada".equals(column) || column.endsWith(" Deg")
                || column.endsWith(" House");
    }
}
