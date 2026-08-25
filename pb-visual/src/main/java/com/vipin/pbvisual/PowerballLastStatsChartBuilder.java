package com.vipin.pbvisual;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Creates interactive charts from the two Markdown tables in pb-last.txt. */
public final class PowerballLastStatsChartBuilder {

    private PowerballLastStatsChartBuilder() {
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println("Usage: PowerballLastStatsChartBuilder [pb-last.txt] [output.html]");
            System.exit(1);
        }

        Path input = args.length > 0
                ? Paths.get(args[0]) : Paths.get("files", "pb_stats", "pb-last.txt");
        Path output = args.length > 1
                ? Paths.get(args[1]) : Paths.get("files", "pb_stats", "pb-last-charts.html");

        try {
            List<List<Stat>> tables = readTables(input);
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeHtml(output, tables.get(0), tables.get(1));
            System.out.println("Created charts from " + tables.get(0).size()
                    + " total-ranked rows and " + tables.get(1).size() + " since-ranked rows.");
            System.out.println("HTML: " + output.toAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to create pb-last charts: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static List<List<Stat>> readTables(Path input) throws IOException {
        List<List<Stat>> tables = new ArrayList<>();
        List<Stat> current = null;
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.startsWith("| Number |")) {
                    current = new ArrayList<>();
                    tables.add(current);
                    continue;
                }
                if (current == null || !trimmed.startsWith("|")
                        || trimmed.startsWith("|---") || trimmed.startsWith("|---:")) {
                    continue;
                }
                String[] fields = trimmed.substring(1, trimmed.length() - 1).split("\\|");
                if (fields.length != 3) {
                    throw invalid(input, lineNumber, "expected three table columns");
                }
                try {
                    current.add(new Stat(Integer.parseInt(fields[0].trim()),
                            Integer.parseInt(fields[1].trim()), Integer.parseInt(fields[2].trim())));
                } catch (NumberFormatException exception) {
                    throw invalid(input, lineNumber, "invalid numeric table value");
                }
            }
        }

        if (tables.size() != 2) {
            throw new IllegalArgumentException(input + ": expected exactly two value tables, found "
                    + tables.size());
        }
        if (tables.get(0).isEmpty() || tables.get(1).isEmpty()) {
            throw new IllegalArgumentException(input + ": both value tables must contain rows");
        }
        return tables;
    }

    private static IllegalArgumentException invalid(Path input, int line, String reason) {
        return new IllegalArgumentException(input + ":" + line + ": " + reason);
    }

    private static void writeHtml(Path output, List<Stat> totals, List<Stat> since)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>Powerball last statistics</title><style>"
                    + ":root{--ink:#17213c;--muted:#687086;--blue:#3867dd;--orange:#ed7b32}*{box-sizing:border-box}"
                    + "body{margin:0;min-height:100vh;font:14px Inter,system-ui,sans-serif;color:var(--ink);"
                    + "background:radial-gradient(circle at top right,#def8ff,transparent 35%),linear-gradient(135deg,#edf2ff,#fbfcff)}"
                    + ".shell{max-width:1550px;margin:auto;padding:28px}.hero{padding:25px 30px;color:white;border-radius:18px;"
                    + "background:linear-gradient(120deg,#111c44,#2949aa 68%,#1683a4);box-shadow:0 15px 42px rgba(28,48,110,.24)}"
                    + "h1{margin:0 0 5px;font-size:28px}.hero p{margin:0;opacity:.78}.toolbar{display:flex;gap:12px;"
                    + "align-items:center;flex-wrap:wrap;margin:18px 0}select{padding:9px 12px;border:1px solid #ccd4e5;border-radius:8px;"
                    + "background:white}.grid{display:grid;grid-template-columns:1fr;gap:20px}.panel{position:relative;background:white;"
                    + "padding:18px;border:1px solid #e3e8f1;border-radius:16px;box-shadow:0 14px 38px rgba(31,51,100,.1)}"
                    + ".panel h2{margin:0;font-size:19px}.panel p{margin:4px 0 12px;color:var(--muted)}"
                    + ".plot{position:relative;max-height:680px;overflow:auto;border:1px solid #edf0f6;border-radius:10px;"
                    + "background:linear-gradient(90deg,#fbfcff,#fff)}canvas{display:block;width:100%;min-width:720px}"
                    + ".tooltip{display:none;position:absolute;pointer-events:none;padding:9px 11px;color:white;"
                    + "background:rgba(15,26,64,.95);border-radius:8px;box-shadow:0 8px 22px rgba(0,0,0,.2);"
                    + "transform:translate(10px,-105%);line-height:1.5}@media(max-width:700px){.shell{padding:13px}.hero{padding:20px}"
                    + "h1{font-size:21px}.plot{max-height:560px}}</style></head><body><main class=\"shell\">"
                    + "<section class=\"hero\"><h1>Powerball Number Statistics</h1>"
                    + "<p>Total occurrence and time-since-last-occurrence rankings from pb-last.txt</p></section>"
                    + "<div class=\"toolbar\"><label>Graph style <select id=\"style\"><option value=\"bar\">Bar</option>"
                    + "<option value=\"line\">Line</option></select></label></div><section class=\"grid\">"
                    + "<article class=\"panel\"><h2>Total Occurrences</h2><p>Numbers in the order of the first value table</p>"
                    + "<div class=\"plot\"><canvas id=\"totals\"></canvas><div class=\"tooltip\"></div></div></article>"
                    + "<article class=\"panel\"><h2>Draws Since Last Occurrence</h2>"
                    + "<p>Numbers in the order of the second value table</p><div class=\"plot\"><canvas id=\"since\"></canvas>"
                    + "<div class=\"tooltip\"></div></div></article></section></main><script>const totals=");
            writeData(writer, totals);
            writer.write(",since=");
            writeData(writer, since);
            writer.write(",charts=[];function create(canvasId,data,key,color){const canvas=document.querySelector('#'+canvasId),"
                    + "ctx=canvas.getContext('2d'),tip=canvas.nextElementSibling,chart={canvas,ctx,tip,data,key,color,bars:[]};charts.push(chart);"
                    + "canvas.addEventListener('mousemove',e=>hover(chart,e));canvas.addEventListener('mouseleave',()=>tip.style.display='none');return chart;}"
                    + "function draw(c){c.canvas.style.height=(c.data.length*30+54)+'px';const r=c.canvas.getBoundingClientRect(),"
                    + "dpr=devicePixelRatio||1;c.canvas.width=r.width*dpr;c.canvas.height=r.height*dpr;c.ctx.setTransform(dpr,0,0,dpr,0,0);"
                    + "const x=c.ctx,w=r.width,h=r.height,p={l:62,r:60,t:34,b:14},max=Math.max(...c.data.map(d=>d[c.key])),"
                    + "step=(h-p.t-p.b)/c.data.length,valueX=v=>p.l+v/(max||1)*(w-p.l-p.r);x.clearRect(0,0,w,h);"
                    + "x.font='12px system-ui';for(let i=0;i<=5;i++){const value=Math.round(max*i/5),px=valueX(value);"
                    + "x.strokeStyle='#e4e9f2';x.beginPath();x.moveTo(px,p.t-5);x.lineTo(px,h-p.b);x.stroke();"
                    + "x.fillStyle='#687086';x.fillText(value,px-8,18);}c.bars=[];const line=document.querySelector('#style').value==='line';"
                    + "if(line){x.beginPath();c.data.forEach((d,i)=>{const px=valueX(d[c.key]),py=p.t+step*(i+.5);"
                    + "i?x.lineTo(px,py):x.moveTo(px,py);c.bars.push({x:px,y:py,d});});x.strokeStyle=c.color;x.lineWidth=2;x.stroke();"
                    + "c.bars.forEach(b=>{x.beginPath();x.arc(b.x,b.y,3.5,0,Math.PI*2);x.fillStyle=c.color;x.fill();});}"
                    + "else c.data.forEach((d,i)=>{const py=p.t+step*(i+.5),px=valueX(d[c.key]);x.fillStyle=c.color;"
                    + "x.globalAlpha=.84;x.fillRect(p.l,py-step*.3,Math.max(1,px-p.l),step*.6);x.globalAlpha=1;"
                    + "c.bars.push({x:px,y:py,d});});c.bars.forEach(b=>{x.strokeStyle='#f0f2f7';x.beginPath();"
                    + "x.moveTo(0,b.y+step/2);x.lineTo(w,b.y+step/2);x.stroke();x.font='bold 12px system-ui';x.fillStyle='#26345c';"
                    + "x.fillText('No. '+b.d.n,10,b.y+4);x.font='12px system-ui';x.fillStyle=c.color;"
                    + "x.fillText(b.d[c.key],Math.min(b.x+7,w-p.r+28),b.y+4);});}"
                    + "function hover(c,e){if(!c.bars.length)return;const r=c.canvas.getBoundingClientRect(),"
                    + "my=e.clientY-r.top;let best=c.bars[0];for(const b of c.bars)if(Math.abs(b.y-my)<Math.abs(best.y-my))best=b;"
                    + "c.tip.style.display='block';c.tip.style.left=Math.min(best.x,r.width-190)+'px';c.tip.style.top=best.y+'px';"
                    + "c.tip.innerHTML='<strong>Number '+best.d.n+'</strong><br>Total occurrences: '+best.d.total+"
                    + "'<br>Draws since: '+best.d.since;}create('totals',totals,'total','#3867dd');"
                    + "create('since',since,'since','#ed7b32');function drawAll(){charts.forEach(draw);}"
                    + "document.querySelector('#style').addEventListener('change',drawAll);addEventListener('resize',drawAll);drawAll();"
                    + "</script></body></html>");
        }
    }

    private static void writeData(BufferedWriter writer, List<Stat> data) throws IOException {
        writer.write('[');
        for (int index = 0; index < data.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            Stat stat = data.get(index);
            writer.write("{n:" + stat.number + ",total:" + stat.total
                    + ",since:" + stat.since + '}');
        }
        writer.write(']');
    }

    private static final class Stat {
        private final int number;
        private final int total;
        private final int since;

        private Stat(int number, int total, int since) {
            this.number = number;
            this.total = total;
            this.since = since;
        }
    }
}
