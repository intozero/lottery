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
import java.util.Map;
import java.util.TreeMap;

/** Builds a selectable HTML chart for five white-ball positions and their sum. */
public final class WhiteBallSeriesChartBuilder {
    private static final DateTimeFormatter DRAW_DATE =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    private WhiteBallSeriesChartBuilder() {
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println("Usage: WhiteBallSeriesChartBuilder [draws-file] [output-html]");
            System.exit(1);
        }

        Path input = args.length > 0
                ? Paths.get(args[0]) : Paths.get("files", "pb_visual", "pb-sorted.txt");
        Path output = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("files", "pb_visual", "pb-whiteball-series-chart.html");

        try {
            Map<LocalDate, Draw> draws = readDraws(input);
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeHtml(output, draws);
            System.out.println("Plotted " + draws.size() + " unique draw dates.");
            System.out.println("HTML: " + output.toAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to create white-ball chart: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static Map<LocalDate, Draw> readDraws(Path input) throws IOException {
        Map<LocalDate, Draw> draws = new TreeMap<>();
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 6 && fields.length != 7) {
                    throw invalid(input, lineNumber, "expected date and at least five white balls");
                }
                try {
                    LocalDate date = LocalDate.parse(fields[0], DRAW_DATE);
                    int[] balls = new int[5];
                    int sum = 0;
                    for (int index = 0; index < balls.length; index++) {
                        balls[index] = Integer.parseInt(fields[index + 1]);
                        sum += balls[index];
                    }
                    // Date is the key. The final row wins when the source contains duplicates.
                    draws.put(date, new Draw(balls, sum));
                } catch (DateTimeParseException | NumberFormatException exception) {
                    throw invalid(input, lineNumber, "invalid draw data: " + exception.getMessage());
                }
            }
        }
        if (draws.isEmpty()) {
            throw new IllegalArgumentException(input + ": no draws found");
        }
        return draws;
    }

    private static IllegalArgumentException invalid(Path file, int line, String reason) {
        return new IllegalArgumentException(file + ":" + line + ": " + reason);
    }

    private static void writeHtml(Path output, Map<LocalDate, Draw> draws) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>Powerball white-ball series</title><style>"
                    + ":root{--ink:#17213c;--muted:#667085;--blue:#3157d5}*{box-sizing:border-box}"
                    + "body{margin:0;min-height:100vh;font:14px Inter,system-ui,sans-serif;color:var(--ink);"
                    + "background:radial-gradient(circle at 90% 0,#dff8ff,transparent 38%),linear-gradient(135deg,#edf2ff,#fbfcff)}"
                    + ".shell{max-width:1550px;margin:auto;padding:28px}.hero{padding:25px 30px;color:white;border-radius:18px;"
                    + "background:linear-gradient(120deg,#111c44,#2c4eb8 68%,#1787a5);box-shadow:0 15px 42px rgba(28,48,110,.24)}"
                    + "h1{margin:0 0 5px;font-size:28px}.hero p{margin:0;opacity:.78}.controls{display:flex;gap:12px;"
                    + "align-items:center;flex-wrap:wrap;margin:18px 0;padding:13px 16px;background:white;border-radius:13px;"
                    + "box-shadow:0 7px 24px rgba(31,51,100,.09)}select{padding:8px 11px;border:1px solid #ccd4e5;border-radius:8px}"
                    + ".series{display:flex;gap:8px;flex-wrap:wrap}.series label{display:flex;gap:6px;align-items:center;padding:7px 10px;"
                    + "border:1px solid #dfe4ee;border-radius:20px;background:#fafcff;cursor:pointer}.series input{accent-color:var(--color)}"
                    + ".dot{width:9px;height:9px;border-radius:50%;background:var(--color)}.chart{position:relative;padding:14px;"
                    + "background:rgba(255,255,255,.96);border:1px solid #e3e8f1;border-radius:16px;"
                    + "box-shadow:0 14px 38px rgba(31,51,100,.11)}canvas{display:block;width:100%;height:600px}.tooltip{display:none;"
                    + "position:absolute;pointer-events:none;padding:10px 12px;color:white;background:rgba(15,26,64,.95);"
                    + "border-radius:8px;box-shadow:0 8px 22px rgba(0,0,0,.2);line-height:1.55;transform:translate(12px,-105%)}"
                    + ".axis-note{display:flex;justify-content:space-between;color:var(--muted);font-size:12px;padding:0 44px 3px}"
                    + "@media(max-width:700px){.shell{padding:13px}.hero{padding:20px}h1{font-size:21px}canvas{height:450px}}"
                    + "</style></head><body><main class=\"shell\"><section class=\"hero\">"
                    + "<h1>White-Ball Values and Sum</h1><p>Select the values you want to compare across drawing dates</p></section>"
                    + "<section class=\"controls\"><label>Time range <select id=\"range\"><option value=\"1\">Last year</option>"
                    + "<option value=\"3\">Last 3 years</option><option value=\"5\">Last 5 years</option>"
                    + "<option value=\"all\" selected>All dates</option></select></label><div id=\"series\" class=\"series\"></div></section>"
                    + "<section class=\"chart\"><div class=\"axis-note\"><span>White-ball value (left scale)</span>"
                    + "<span>White-ball sum (right scale)</span></div><canvas id=\"canvas\"></canvas>"
                    + "<div id=\"tooltip\" class=\"tooltip\"></div></section></main><script>const all=[");

            boolean first = true;
            for (Map.Entry<LocalDate, Draw> entry : draws.entrySet()) {
                if (!first) {
                    writer.write(',');
                }
                Draw draw = entry.getValue();
                writer.write("{d:\"" + entry.getKey() + "\",b:[" + draw.balls[0] + ','
                        + draw.balls[1] + ',' + draw.balls[2] + ',' + draw.balls[3] + ','
                        + draw.balls[4] + "],sum:" + draw.sum + '}');
                first = false;
            }

            writer.write("].map(x=>({...x,t:new Date(x.d+'T00:00:00').getTime()}));"
                    + "const defs=[{name:'White Ball 1',key:0,color:'#3157d5'},{name:'White Ball 2',key:1,color:'#00a6a6'},"
                    + "{name:'White Ball 3',key:2,color:'#7c4dff'},{name:'White Ball 4',key:3,color:'#e3498b'},"
                    + "{name:'White Ball 5',key:4,color:'#e09b24'},{name:'Sum',key:'sum',color:'#e63946'}],enabled=new Set(defs.map(d=>d.key)),"
                    + "canvas=document.querySelector('#canvas'),ctx=canvas.getContext('2d'),tip=document.querySelector('#tooltip'),"
                    + "range=document.querySelector('#range'),series=document.querySelector('#series');let data=[],xPoints=[];"
                    + "defs.forEach(d=>{const label=document.createElement('label');label.style.setProperty('--color',d.color);"
                    + "label.innerHTML='<input type=\"checkbox\" checked><span class=\"dot\"></span>'+d.name;"
                    + "label.querySelector('input').onchange=e=>{e.target.checked?enabled.add(d.key):enabled.delete(d.key);draw();};series.append(label);});"
                    + "function selectData(){if(range.value==='all')data=all;else{const end=new Date(all[all.length-1].t),start=new Date(end);"
                    + "start.setFullYear(end.getFullYear()-Number(range.value));data=all.filter(x=>x.t>=start.getTime());}draw();}"
                    + "function draw(){const r=canvas.getBoundingClientRect(),dpr=devicePixelRatio||1;canvas.width=r.width*dpr;canvas.height=r.height*dpr;"
                    + "ctx.setTransform(dpr,0,0,dpr,0,0);const w=r.width,h=r.height,p={l:55,r:57,t:20,b:43};ctx.clearRect(0,0,w,h);"
                    + "if(!data.length)return;const minT=data[0].t,maxT=data[data.length-1].t,x=t=>p.l+(t-minT)/(maxT-minT||1)*(w-p.l-p.r),"
                    + "ballY=v=>p.t+(70-v)/70*(h-p.t-p.b),sumY=v=>p.t+(350-v)/350*(h-p.t-p.b);ctx.font='12px system-ui';"
                    + "for(let i=0;i<=7;i++){const v=i*10,py=ballY(v);ctx.strokeStyle='#e7eaf2';ctx.beginPath();ctx.moveTo(p.l,py);"
                    + "ctx.lineTo(w-p.r,py);ctx.stroke();ctx.fillStyle='#667085';ctx.fillText(v,17,py+4);ctx.fillText(v*5,w-p.r+10,py+4);}"
                    + "for(let i=0;i<=6;i++){const t=minT+(maxT-minT)*i/6,label=new Date(t).toLocaleDateString(undefined,{year:'numeric',month:'short'});"
                    + "ctx.fillText(label,Math.max(p.l,Math.min(x(t)-22,w-p.r-45)),h-14);}xPoints=data.map(d=>x(d.t));defs.forEach(def=>{"
                    + "if(!enabled.has(def.key))return;ctx.beginPath();data.forEach((d,i)=>{const value=def.key==='sum'?d.sum:d.b[def.key],"
                    + "py=def.key==='sum'?sumY(value):ballY(value),px=xPoints[i];i?ctx.lineTo(px,py):ctx.moveTo(px,py);});"
                    + "ctx.strokeStyle=def.color;ctx.lineWidth=def.key==='sum'?2.8:1.6;ctx.lineJoin='round';ctx.stroke();});}"
                    + "canvas.addEventListener('mousemove',e=>{if(!xPoints.length)return;const r=canvas.getBoundingClientRect(),mx=e.clientX-r.left;"
                    + "let best=0;for(let i=1;i<xPoints.length;i++)if(Math.abs(xPoints[i]-mx)<Math.abs(xPoints[best]-mx))best=i;"
                    + "const d=data[best],values=defs.filter(x=>enabled.has(x.key)).map(x=>x.name+': <b>'+(x.key==='sum'?d.sum:d.b[x.key])+'</b>');"
                    + "tip.style.display='block';tip.style.left=xPoints[best]+'px';tip.style.top=(e.clientY-r.top)+'px';"
                    + "tip.innerHTML='<strong>'+d.d+'</strong><br>'+values.join('<br>');});canvas.addEventListener('mouseleave',()=>tip.style.display='none');"
                    + "range.addEventListener('change',selectData);addEventListener('resize',draw);selectData();</script></body></html>");
        }
    }

    private static final class Draw {
        private final int[] balls;
        private final int sum;

        private Draw(int[] balls, int sum) {
            this.balls = balls;
            this.sum = sum;
        }
    }
}
