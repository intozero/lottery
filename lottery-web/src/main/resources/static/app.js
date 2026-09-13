'use strict';
const $=s=>document.querySelector(s);
const esc=v=>String(v??'—').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const fmt=(n,d=0)=>Number(n).toLocaleString(undefined,{minimumFractionDigits:d,maximumFractionDigits:d});
const state={view:'overview',history:[],analysis:null,token:null,version:0,selection:null,occurrenceNumber:1,latestDraw:null};
const titles={overview:'Overview',history:'Draw history',numbers:'Number statistics',occurrences:'Ball occurrences',sums:'Sums & deviation',ranges:'Range explorer',digits:'Digit patterns',combinations:'Combinations',data:'Data manager'};
const balls=(whites,special)=>whites.map(n=>'<span class="ball">'+esc(n)+'</span>').join('')+(special==null?'':'<span class="ball red">'+esc(special)+'</span>');
const panel=(title,note,id)=>'<section class="panel"><h2>'+esc(title)+'</h2><p class="muted">'+esc(note)+'</p><div id="'+id+'"></div></section>';
const entries=obj=>Object.entries(obj).map(([value,count])=>({value,count}));
const filterQuery=()=>{const p=new URLSearchParams({startDraw:$('#startDraw').value,rowStep:$('#rowStep').value,game:$('#game').value});if($('#lastDraw').value&&$('#lastDraw').value!==String(state.latestDraw))p.set('lastDraw',$('#lastDraw').value);if($('#from').value)p.set('from',$('#from').value);if($('#to').value)p.set('to',$('#to').value);return p};
const query=()=>new URLSearchParams(state.selection||filterQuery());
function notify(message,error=false){const n=$('#notice');n.textContent=message;n.className='notice'+(error?' error':'');n.hidden=false;}
async function api(path,options={}){
 if(options.method&&options.method!=='GET'){
  if(!state.token)state.token=await api('/api/csrf');
  options.headers={...options.headers,[state.token.header]:state.token.token};
 }
 const response=await fetch(path,options);
 if(!response.ok){let e;try{e=await response.json()}catch{e={}}throw new Error(e.error||'Request failed ('+response.status+'). Refresh and try again.');}
 return response.json();
}
function exportLink(report,label){return '<a class="secondary" href="/api/export?'+esc(query().toString())+'&report='+report+'">'+esc(label)+'</a>';}
function table(id,columns,data,{size=50}={}){
 const root=typeof id==='string'?document.getElementById(id):id;
 if(!root)return;
 let page=0,search='',sort=null,ascending=true;
 root.innerHTML='<div class="tablecontrols"><input aria-label="Search table" placeholder="Search this table…"><span></span></div><div class="tablewrap"></div><div class="pagination"></div>';
 root.querySelector('input').addEventListener('input',event=>{search=event.target.value.toLowerCase();page=0;draw()});
 function draw(){
  let rows=data.filter(r=>!search||Object.values(r).some(v=>String(v??'').toLowerCase().includes(search)));
  if(sort)rows=[...rows].sort((a,b)=>{const av=a[sort],bv=b[sort];const c=av==null?(bv==null?0:-1):bv==null?1:typeof av==='number'&&typeof bv==='number'?av-bv:String(av).localeCompare(String(bv));return ascending?c:-c;});
  const pages=Math.max(1,Math.ceil(rows.length/size));page=Math.min(page,pages-1);
  root.querySelector('.tablecontrols span').textContent=fmt(rows.length)+' rows';
  const body=rows.slice(page*size,(page+1)*size);
  root.querySelector('.tablewrap').innerHTML=body.length?'<table><thead><tr>'+columns.map(c=>'<th class="'+(c.num?'num':'')+'"><button data-sort="'+c.key+'">'+esc(c.label)+(sort===c.key?(ascending?' ↑':' ↓'):'')+'</button></th>').join('')+'</tr></thead><tbody>'+body.map(r=>'<tr>'+columns.map(c=>'<td class="'+(c.num?'num':'')+'">'+(c.render?c.render(r):esc(r[c.key]))+'</td>').join('')+'</tr>').join('')+'</tbody></table>':'<div class="empty">No rows match this selection.</div>';
  root.querySelectorAll('[data-sort]').forEach(b=>b.addEventListener('click',()=>{ascending=sort===b.dataset.sort?!ascending:true;sort=b.dataset.sort;draw()}));
  root.querySelector('.pagination').innerHTML='<span>Page '+(page+1)+' of '+pages+'</span><button aria-label="Previous page" '+(page===0?'disabled':'')+'>←</button><button aria-label="Next page" '+(page===pages-1?'disabled':'')+'>→</button>';
  const buttons=root.querySelectorAll('.pagination button');buttons[0].onclick=()=>{page--;draw()};buttons[1].onclick=()=>{page++;draw()};
 }
 draw();
}
const col=(key,label,num=false,render)=>({key,label,num,render});
function histogram(data,key,value,label){
 const rows=data.slice(0,90);if(!rows.length)return '<div class="empty">Import history to see this chart.</div>';
 const max=Math.max(...rows.map(r=>r[value]),1), width=650,height=180,step=width/rows.length;
 return '<svg class="chart" viewBox="0 0 680 220" role="img" aria-label="'+esc(label)+'"><line x1="15" x2="665" y1="185" y2="185" stroke="#dce5e1"/>'+rows.map((r,i)=>{const h=r[value]/max*height;return '<rect x="'+(15+i*step)+'" y="'+(185-h)+'" width="'+Math.max(1,step-2)+'" height="'+h+'" rx="2" fill="#4b9c84"><title>'+esc(r[key])+': '+esc(r[value])+'</title></rect>'+(i%Math.max(1,Math.floor(rows.length/10))===0?'<text x="'+(15+i*step)+'" y="206">'+esc(r[key])+'</text>':'')}).join('')+'</svg>';
}
function sumDistribution(counts){
 const sums=Object.keys(counts).map(Number).sort((a,b)=>a-b);
 if(!sums.length)return '<div class="empty">No draws in this selection.</div>';
 const first=sums[0],last=sums[sums.length-1],bins=last-first+1;
 const total=Object.values(counts).reduce((a,b)=>a+b,0),max=Math.max(...Object.values(counts));
 const left=65,top=25,width=810,height=230,bottom=top+height,step=width/bins;
 const niceStep=n=>{const power=10**Math.floor(Math.log10(Math.max(1,n)));return [1,2,5,10].map(v=>v*power).find(v=>v>=n)};
 const yStep=niceStep(max/5),yMax=Math.max(yStep,Math.ceil(max/yStep)*yStep);
 const xStep=niceStep((last-first)/10);
 let grid='',ticks='',bars='';
 for(let count=0;count<=yMax;count+=yStep){const y=bottom-count/yMax*height;
  grid+='<line x1="'+left+'" x2="'+(left+width)+'" y1="'+y+'" y2="'+y+'" stroke="#dce5e1"/><text x="'+(left-10)+'" y="'+(y+4)+'" text-anchor="end">'+fmt(count)+'</text>';
 }
 const xTicks=[first];
 for(let sum=Math.ceil(first/xStep)*xStep;sum<=last;sum+=xStep)if(sum>first&&(sum-first)*step>35&&(last-sum)*step>35)xTicks.push(sum);
 if(last!==first)xTicks.push(last);
 for(const sum of xTicks){const x=left+(sum-first+.5)*step;
  ticks+='<text x="'+x+'" y="'+(bottom+21)+'" text-anchor="middle">'+sum+'</text>';
 }
 for(let sum=first;sum<=last;sum++){
  const count=counts[sum]||0,h=count/yMax*height,x=left+(sum-first)*step;
  const detail='Sum '+sum+': '+fmt(count)+' '+(count===1?'draw':'draws')+' ('+fmt(total?count/total*100:0,2)+'%)';
  bars+='<g class="sum-bin" data-sum="'+sum+'" data-count="'+count+'" tabindex="0" role="img" aria-label="'+esc(detail)+'"><title>'+esc(detail)+'</title><rect class="sum-bar" x="'+(x+step*.1)+'" y="'+(bottom-h)+'" width="'+step*.8+'" height="'+h+'" fill="#4b9c84"/><rect x="'+x+'" y="'+top+'" width="'+step+'" height="'+height+'" fill="transparent"/></g>';
 }
 return '<div class="sum-chart-scroll"><svg class="chart sum-chart" viewBox="0 0 900 320" role="group" aria-label="Sum distribution: white-ball sum versus draw count"><text x="18" y="140" transform="rotate(-90 18 140)" text-anchor="middle">Draw count</text>'+grid+bars+ticks+'<text x="470" y="308" text-anchor="middle">White-ball sum</text></svg></div><p id="sumChartDetail" class="muted" role="status">'+fmt(total)+' selected draws · '+first+'–'+last+' sum range. Hover, tap, or focus a bar for exact values.</p>';
}
function bindSumDistribution(){
 document.querySelectorAll('.sum-bin').forEach(bin=>{
  const show=()=>{$('#sumChartDetail').textContent=bin.getAttribute('aria-label')};
  bin.addEventListener('mouseenter',show);bin.addEventListener('focus',show);bin.addEventListener('click',show);
 });
}
function numberChartControls(){
 return '<div class="formrow"><label>Chart measure<select id="numberMeasure"><option value="both">Occurrences & recency</option><option value="total">Total occurrences</option><option value="since">Draws since last seen</option></select></label><label>Order by<select id="numberOrder"><option value="number">White-ball number</option><option value="total">Most occurrences first</option><option value="since">Longest since last seen first</option></select></label></div><p class="muted">Each pair compares occurrence count with selected draws since last seen. Hover or focus for exact values; click, tap, or press Enter to select a ball. Never-seen balls use the full selected draw count for Since.</p><div class="number-legend"><span><i class="total-key"></i> Total occurrences</span><span><i class="since-key"></i> Draws since last seen</span></div><div id="numberChart" class="number-chart-scroll"></div><p id="numberChartDetail" class="muted" role="status">Select a ball to explore its history.</p><div class="actions"><button id="chartOccurrences" class="secondary" disabled>View occurrences</button><button id="chartTimeline" class="secondary" disabled>View timeline</button></div>';
}
function bindNumberChart(numbers,count){
 let selected=null;
 const detail=n=>'White ball '+n.number+' · '+fmt(n.total)+' occurrences · Since: '+fmt(n.since)+' selected draws'+(!n.lastDate?' (NEVER seen)':'')+' · Last date: '+(n.lastDate||'NEVER')+' · Min / max gap: '+(n.minGap??'—')+' / '+(n.maxGap??'—');
 function draw(){
  const metric=$('#numberMeasure').value,order=$('#numberOrder').value;
  const keys=metric==='both'?['total','since']:[metric];
  const rows=[...numbers].sort((a,b)=>order==='number'?a.number-b.number:b[order]-a[order]||a.number-b.number);
  if(!count||!rows.length){$('#numberChart').innerHTML='<div class="empty">No draws in this selection.</div>';return;}
  const left=60,top=25,height=225,width=Math.max(820,rows.length*16),bottom=top+height,step=width/rows.length;
  const max=Math.max(1,...rows.flatMap(n=>keys.map(k=>n[k]))),power=10**Math.floor(Math.log10(Math.max(1,max/5)));
  const tick=[1,2,5,10].map(v=>v*power).find(v=>v>=max/5),ceiling=Math.ceil(max/tick)*tick;
  let grid='',bars='';
  for(let value=0;value<=ceiling;value+=tick){const y=bottom-value/ceiling*height;
   grid+='<line x1="'+left+'" x2="'+(left+width)+'" y1="'+y+'" y2="'+y+'" stroke="#dce5e1"/><text x="'+(left-8)+'" y="'+(y+4)+'" text-anchor="end">'+fmt(value)+'</text>';
  }
  rows.forEach((n,i)=>{const x=left+i*step;
   bars+='<g class="number-bin" data-number="'+n.number+'" tabindex="0" role="button" aria-pressed="'+(selected===n.number)+'" aria-label="'+esc(detail(n))+'"><title>'+esc(detail(n))+'</title>'+keys.map((key,j)=>{const h=n[key]/ceiling*height;return '<rect class="number-bar '+key+'-bar" data-value="'+n[key]+'" x="'+(x+step*.1+j*step*.8/keys.length)+'" y="'+(bottom-h)+'" width="'+step*.8/keys.length+'" height="'+h+'"/>'}).join('')+'<rect class="number-hit" x="'+x+'" y="'+top+'" width="'+step+'" height="'+height+'" fill="transparent"/><text x="'+(x+step/2)+'" y="'+(bottom+20)+'" text-anchor="middle">'+n.number+'</text></g>';
  });
  $('#numberChart').innerHTML='<svg class="chart number-chart" viewBox="0 0 '+(width+85)+' 310" role="group" aria-label="White-ball occurrence and recency chart"><text x="18" y="140" transform="rotate(-90 18 140)" text-anchor="middle">Count (draws)</text>'+grid+bars+'<text x="'+(left+width/2)+'" y="300" text-anchor="middle">White-ball number'+(order==='number'?'':' · ordered by '+(order==='total'?'occurrences':'recency'))+'</text></svg>';
  document.querySelectorAll('.number-bin').forEach(bin=>{
   const n=numbers.find(n=>n.number===Number(bin.dataset.number));
   const inspect=()=>{$('#numberChartDetail').textContent=detail(n)};
   const select=()=>{selected=n.number;inspect();$('#trackNumber').value=n.number;document.querySelectorAll('.number-bin').forEach(b=>b.setAttribute('aria-pressed',String(Number(b.dataset.number)===selected)));$('#chartOccurrences').disabled=false;$('#chartTimeline').disabled=false;$('#chartOccurrences').textContent='Occurrences for '+selected;$('#chartTimeline').textContent='Timeline for '+selected;};
   bin.addEventListener('mouseenter',inspect);bin.addEventListener('focus',inspect);bin.addEventListener('click',select);
   bin.addEventListener('keydown',ev=>{if(ev.key==='Enter'||ev.key===' '){ev.preventDefault();select()}});
  });
 }
 $('#numberMeasure').onchange=draw;$('#numberOrder').onchange=draw;
 $('#chartOccurrences').onclick=()=>{state.occurrenceNumber=selected;navigate('occurrences')};
 $('#chartTimeline').onclick=()=>{$('#trackNumber').value=selected;$('#numberForm').requestSubmit($('#numberForm button'));$('#numberForm').scrollIntoView?.({behavior:'smooth',block:'start'})};
 draw();
}
async function reload(){
 if(!$('#filters').reportValidity())return;
 state.selection=filterQuery().toString();
 const version=++state.version;
 $('#notice').hidden=true;$('#content').classList.add('loader');
 try {
  const requested=query();
  const [history,analysis,bounds]=await Promise.all([api('/api/history?'+requested),api('/api/analysis?'+requested),api('/api/history-bounds?'+new URLSearchParams({game:requested.get('game')}))]);
  if(version!==state.version)return;
  state.history=history;state.analysis=analysis;
  state.latestDraw=bounds.lastDraw;
  if(!requested.has('lastDraw'))$('#lastDraw').value=bounds.lastDraw||'';
  $('#scope').textContent=fmt(history.length)+' draws selected · starting draw '+query().get('startDraw')+' · last draw '+(query().get('lastDraw')||state.latestDraw||'—')+' · row step '+query().get('rowStep');
  await render();
 }catch(error){if(version===state.version){notify(error.message,true);$('#content').innerHTML='<div class="empty">Could not load this selection. Adjust the date range or refresh to retry.</div>';}}
 finally{if(version===state.version)$('#content').classList.remove('loader');}
}
async function render(){
 $('#title').textContent=titles[state.view];
 document.querySelectorAll('[data-view]').forEach(b=>{const active=b.dataset.view===state.view;b.classList.toggle('active',active);if(active)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});
 const a=state.analysis, h=state.history, root=$('#content');
 if(!a)return;
 const empty=!h.length?'<div class="notice">No draws in this selection. Adjust the dates or import a history file in Data manager.</div>':'';
 switch(state.view){
 case 'overview':{
  const latest=a.latest, frequent=[...a.numbers].sort((a,b)=>b.total-a.total).slice(0,5);
  root.innerHTML=empty+'<div class="metrics">'+[
    ['Recorded draws',fmt(a.count),'In the selected history'],
    ['Average white-ball sum',fmt(a.averageSum,2),'Five white balls per draw'],
    ['Latest recorded draw',latest?latest.date:'—','Stored history, not a live feed'],
    ['Repeated combinations',fmt(a.repeated.length),'Exact white + special ball matches']
   ].map(m=>'<article class="metric"><span class="label">'+m[0]+'</span><strong>'+m[1]+'</strong><small>'+m[2]+'</small></article>').join('')+'</div>'+
   '<div class="grid"><section class="panel"><div class="paneltop"><div><h2>White-ball frequency</h2><p class="muted">Occurrences across the selected history</p></div><span class="tag">'+esc($('#game').value)+'</span></div>'+histogram(a.numbers,'number','total','White-ball occurrence counts')+'</section>'+
   '<section class="panel accentpanel"><span class="eyebrow">LATEST IN YOUR HISTORY</span><h2>'+esc(latest?.date||'No recorded draws')+'</h2><div class="bigballs">'+(latest?balls(latest.whites,latest.special):'—')+'</div><p class="muted">White balls shown ascending. The special ball is shown in red.</p>'+exportLink('history','Export history ↗')+'</section></div>'+
   '<div class="grid"><section class="panel"><h2>Most frequent numbers</h2><p class="muted">Counts describe this selection only.</p>'+frequent.map(n=>'<div class="featureline"><span>'+balls([n.number],null)+'</span><strong>'+fmt(n.total)+' appearances</strong><span>'+n.since+' draws since</span></div>').join('')+'</section>'+
   '<section class="panel"><h2>A connected analysis workspace</h2><p class="muted">Start with a question, then explore the history.</p><div class="featureline">How have sums changed? <button class="linkbutton" data-go="sums">Explore sums →</button></div><div class="featureline">Which ranges occur together? <button class="linkbutton" data-go="ranges">Explore ranges →</button></div><div class="featureline">Need to add recent results? <button class="linkbutton" data-go="data">Manage data →</button></div></section></div>';
  root.querySelectorAll('[data-go]').forEach(b=>b.onclick=()=>navigate(b.dataset.go));break;
 }
 case 'history':
  root.innerHTML=empty+'<section class="panel"><div class="paneltop"><div><h2>Recorded draws</h2><p class="muted">Draw # counts from the oldest stored draw (1) for this game, before date, starting-draw, and row-step filters. Click a column heading to sort.</p></div>'+exportLink('history','Export .txt')+'</div><div id="drawTable"></div></section>'+panel('Repeated combinations','Repeated results on different dates are valid draws and are retained.','repeatTable');
  table('drawTable',[col('drawNumber','Draw #',true),col('date','Draw date'),col('whites','White balls',false,r=>balls(r.whites,null)),col('special','Special ball',true,r=>r.special==null?'—':balls([],r.special)),col('sum','Sum',true)],h.map(r=>({...r,sum:r.whites.reduce((a,b)=>a+b,0)})).reverse());
  table('repeatTable',[col('balls','Combination'),col('dates','Draw dates',false,r=>esc(r.dates.join(', ')))],a.repeated);break;
 case 'numbers':
  root.innerHTML=empty+'<section class="panel"><div class="paneltop"><div><h2>Occurrence & recency</h2><p class="muted">Since = completed draws after the last appearance. NEVER = not seen in this selection.</p></div>'+exportLink('last','Export LAST report')+'</div>'+numberChartControls()+'<div id="numberTable"></div></section>'+
  '<section class="panel"><h2>Follow one number</h2><p class="muted">Cumulative appearances and draws since last seen, after every draw.</p><form id="numberForm" class="formrow"><label>White ball<input id="trackNumber" type="number" min="1" max="'+($('#game').value==='PB'?69:75)+'" value="1" required></label><button class="primary">View timeline</button></form><div id="timeline"></div></section>'+
  '<section class="panel"><h2>Full historical reports</h2><p class="muted">SIM includes every number after every draw. NUM_OCCUR includes cumulative range totals.</p><div class="actions">'+exportLink('sim','Export SIM')+exportLink('num_occur','Export NUM_OCCUR')+'</div></section>';
  table('numberTable',[col('number','Number',true),col('total','Total',true),col('since','Since',true),col('lastDate','Last date',false,r=>esc(r.lastDate||'NEVER')),col('minGap','Min gap',true),col('maxGap','Max gap',true)],a.numbers);
  bindNumberChart(a.numbers,h.length);
  $('#numberForm').onsubmit=async ev=>{ev.preventDefault();await task(ev.submitter,async()=>{const p=query();p.set('number',$('#trackNumber').value);const rows=await api('/api/timeline?'+p);table('timeline',[col('date','Date'),col('draw','Draw',true),col('appeared','Appeared',false,r=>r.appeared?'Yes':'—'),col('total','Total',true),col('since','Since',true)],rows);})};break;
 case 'occurrences':{
  const maximum=query().get('game')==='PB'?69:75;
  state.occurrenceNumber=Math.min(state.occurrenceNumber,maximum);
  root.innerHTML=empty+'<section class="panel"><h2>White-ball occurrences</h2><p class="muted">Find every appearance in the selected history. Draw # is the original number counted from the oldest stored draw. Draw gap is the difference between consecutive matching draw numbers (adjacent draws = 1). Running total counts matches in this selection only; the first match has no previous gap.</p><form id="occurrenceForm" class="formrow"><label>White ball<input id="occurrenceNumber" type="number" min="1" max="'+maximum+'" step="1" value="'+state.occurrenceNumber+'" required></label><button class="primary">Find occurrences</button></form><p id="occurrenceStatus" class="muted" role="status"></p><div id="occurrenceTable"></div></section>';
  const show=()=>{
   const number=state.occurrenceNumber,rows=[];let previous=null;
   for(const draw of h){
    if(!draw.whites.includes(number))continue;
    rows.push({...draw,gap:previous===null?null:draw.drawNumber-previous,total:rows.length+1});
    previous=draw.drawNumber;
   }
   $('#occurrenceStatus').textContent='White ball '+number+' · '+fmt(rows.length)+' occurrences in '+fmt(h.length)+' selected draws';
   table('occurrenceTable',[col('date','Draw date'),col('drawNumber','Draw #',true),col('whites','White balls',false,r=>balls(r.whites,null)),col('special','Special ball',true,r=>r.special==null?'—':balls([],r.special)),col('gap','Draw gap',true),col('total','Running total',true)],rows);
  };
  $('#occurrenceForm').onsubmit=ev=>{ev.preventDefault();if(!ev.currentTarget.reportValidity())return;state.occurrenceNumber=Number($('#occurrenceNumber').value);show()};
  show();break;
 }
 case 'sums':
  root.innerHTML=empty+'<section class="panel"><div class="paneltop"><div><h2>Sum distribution</h2><p class="muted">Each bar shows the exact count for one five-white-ball sum. Gaps represent sums with zero draws in this selection.</p></div>'+exportLink('sums','Export sums report')+'</div>'+sumDistribution(a.sumCounts)+'<div id="sumFrequency"></div></section>'+
   panel('Per-draw measurements','Mean = sum ÷ 5. Deviation is the population standard deviation, calculated without integer truncation.','sumTable')+
   '<div class="grid">'+panel('Deviation distribution','Grouped by floor of the exact standard deviation.','deviationTable')+panel('First & last ball study','Frequency of each smallest white ball, largest white ball, and their sum.','endTable')+'</div>';
  bindSumDistribution();
  table('sumFrequency',[col('value','Sum',true),col('count','Draw count',true)],entries(a.sumCounts));
  table('sumTable',[col('date','Date'),col('sum','Sum',true),col('mean','Mean',true,r=>fmt(r.mean,2)),col('deviation','Deviation',true,r=>fmt(r.deviation,2)),col('runningTotal','Running total',true,r=>fmt(r.runningTotal)),col('runningAverage','Running average',true,r=>fmt(r.runningAverage,2))],a.sums);
  table('deviationTable',[col('value','Deviation floor',true),col('count','Count',true)],entries(a.deviations));
  const endRows=[...entries(a.first).map(r=>({...r,kind:'Smallest ball'})),...entries(a.last).map(r=>({...r,kind:'Largest ball'})),...entries(a.endSums).map(r=>({...r,kind:'Smallest + largest'}))];
  table('endTable',[col('kind','Measurement'),col('value','Value',true),col('count','Count',true)],endRows);break;
 case 'ranges':
  root.innerHTML=empty+panel('Range totals','Ranges are 1–9, 10–19, etc. PB ends at 69; the historical MM analysis universe ends at 75.','rangeTotals')+
   '<div class="grid">'+panel('Observed range patterns','Counts in each ordered range, joined with dashes. They sum to five.','patternTable')+panel('Occupancy shapes','Nonempty bucket counts sorted descending, e.g. 3+2.','shapeTable')+'</div>'+
   '<section class="panel"><div class="paneltop"><div><h2>Compare with all possible range patterns</h2><p class="muted">Counts are combinatorial, within the fixed game analysis universe. Unseen does not mean more likely.</p></div>'+exportLink('ran','Export RAN')+'</div><label><span><input id="unseen" type="checkbox"> Show unseen patterns only</span></label><div id="universe"></div></section>';
  table('rangeTotals',[col('range','Range'),col('total','Total appearances',true),col('since','Sum of since',true)],a.ranges);
  table('patternTable',[col('value','Pattern'),col('count','Draw count',true)],entries(a.patterns));
  table('shapeTable',[col('value','Shape'),col('count','Draw count',true)],entries(a.shapes));
  {const p=query().toString();try{const rows=await api('/api/ranges?'+p);if(state.view!=='ranges'||p!==query().toString())break;
   const draw=()=>table('universe',[col('pattern','Pattern'),col('observed','Observed',true),col('combinations','Possible combinations',true,r=>fmt(r.combinations)),col('square','Sum of squared counts',true)],$('#unseen').checked?rows.filter(r=>!r.observed):rows);
   $('#unseen').onchange=draw;draw();}catch(e){notify(e.message,true);}}break;
 case 'digits':
  root.innerHTML=empty+'<section class="panel"><h2>Search the digit stream</h2><p class="muted">Sorted white balls, then special ball, without padding or separators. Overlapping windows include draw boundaries. Missing special balls must be filled before this analysis.</p><form id="digitForm" class="formrow"><label>Window length<input id="window" type="number" min="1" max="100" value="10" required></label><button class="primary">Analyze windows</button></form><div id="digitStats"></div><div id="digitTable"></div></section>';
  $('#digitForm').onsubmit=async ev=>{ev.preventDefault();await task(ev.submitter,async()=>{const p=query();p.set('window',$('#window').value);const d=await api('/api/digits?'+p);$('#digitStats').textContent=fmt(d.digits)+' digits · '+fmt(d.windows)+' windows · '+fmt(d.unique)+' unique patterns'+(d.truncated?' · Showing the top 1,000':'');table('digitTable',[col('pattern','Pattern'),col('count','Count',true)],d.rows);})};break;
 case 'combinations':
  root.innerHTML='<section class="panel"><h2>Find five-ball combinations</h2><p class="muted">A mathematical search independent of history and row-step filters. Results are unique ascending white-ball sets, not recommendations.</p><form id="comboForm" class="formrow"><label>Maximum white ball<input id="maximum" type="number" min="5" max="75" value="'+($('#game').value==='PB'?69:75)+'" required></label><label>Target sum<input id="targetSum" type="number" min="15" max="365" value="188" required></label><label>Deviation floor (optional)<input id="deviation" type="number" min="0" max="40" placeholder="Any"></label><button class="primary">Find combinations</button></form><p id="comboStatus" class="muted">Results are limited to 500; narrow the filters for a smaller set.</p><div id="comboTable"></div></section>';
  $('#comboForm').onsubmit=async ev=>{ev.preventDefault();await task(ev.submitter,async()=>{const p=new URLSearchParams({maximum:$('#maximum').value,sum:$('#targetSum').value});if($('#deviation').value!=='')p.set('deviation',$('#deviation').value);const d=await api('/api/combinations?'+p);$('#comboStatus').textContent=d.truncated?'Showing the first 500 matches; additional matches exist.':d.rows.length+' matching combinations.';table('comboTable',[col('index','#',true),col('balls','White balls',false,r=>balls(r.balls,null)),col('sum','Sum',true)],d.rows.map((b,i)=>({index:i+1,balls:b,sum:b.reduce((a,b)=>a+b,0)})));})};break;
 case 'data':
  root.innerHTML='<div class="grid"><section class="panel"><h2>Import draw history</h2><p class="muted">Import into '+esc($('#game').value)+'. Date and row-step filters do not restrict imports. Existing identical draws are skipped; conflicting known results reject the whole import.</p><form id="uploadForm"><label>Text file · up to 3 MB<input type="file" id="upload" accept=".txt,text/plain" required></label><div class="actions"><button class="primary">Import file</button></div></form><div class="separator"></div><form id="pasteForm"><label>Or paste dated draws<textarea id="paste" placeholder="10/17/2015  48  49  57  62  69  19" required maxlength="3000000"></textarea></label><div class="actions"><button class="secondary">Import pasted text</button></div></form></section>'+
   '<section class="panel accentpanel"><span class="eyebrow">OFFICIAL HISTORY</span><h2>Validate & update Powerball</h2><p class="muted">Checks every stored Powerball draw against the Texas Lottery history, repairs mismatches, and adds missing published draws. Corrections are recorded below. This updates the database only.</p><button id="sync" class="primary">Sync Powerball results</button><div class="separator"></div><h3>Take a copy of your data</h3><p class="muted">Exports use the current game, dates, starting draw, last draw, and row step. Original repository files are never changed.</p>'+exportLink('history','Download history')+'<div class="separator"></div><p class="muted">PB analysis uses 1–69. MM uses a historical 1–75 universe. Imports check structure, not date-specific rules or official results.</p></section></div>'+
   panel('Import activity','Successful imports and official updates, newest first.','importLog')+panel('Correction history','Previous values are retained when an official update repairs a draw or an import fills a missing special ball.','changeLog');
  $('#uploadForm').onsubmit=async ev=>{ev.preventDefault();await task(ev.submitter,async()=>{const file=$('#upload').files[0];if(!file||file.size>3000000)throw new Error('Choose a text file up to 3 MB.');const data=new FormData();data.append('game',$('#game').value);data.append('file',file);await imported(await api('/api/import',{method:'POST',body:data}));})};
  $('#pasteForm').onsubmit=async ev=>{ev.preventDefault();await task(ev.submitter,async()=>await imported(await api('/api/import-text',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({game:$('#game').value,text:$('#paste').value})})))};
  $('#sync').onclick=async ev=>await task(ev.currentTarget,async()=>await imported(await api('/api/powerball/sync',{method:'POST'})),'Checking official history…');
  try{const [imports,changes]=await Promise.all([api('/api/imports'),api('/api/changes')]);if(state.view!=='data')break;
   const normalize=rows=>rows.map(r=>Object.fromEntries(Object.entries(r).map(([k,v])=>[k.toLowerCase(),v])));
   table('importLog',[col('created_at','Time'),col('game','Game'),col('source','Source'),col('added','Added',true),col('corrected','Corrected',true),col('unchanged','Unchanged',true)],normalize(imports));
   table('changeLog',[col('created_at','Time'),col('game','Game'),col('draw_date','Draw date'),col('previous_values','Previous'),col('new_values','Corrected'),col('source','Source')],normalize(changes));
  }catch(e){notify(e.message,true);}break;
 }
}
async function task(button,work,label='Working…'){
 const previous=button.textContent;button.disabled=true;button.textContent=label;$('#notice').hidden=true;
 try{await work()}catch(e){notify(e.message,true)}finally{button.disabled=false;button.textContent=previous;}
}
async function imported(result){await reload();notify(fmt(result.added)+' draws added · '+fmt(result.corrected)+' corrected · '+fmt(result.unchanged)+' already matched.');}
function navigate(view){if(!titles[view])view='overview';state.view=view;location.hash=view;render().catch(e=>notify(e.message,true));}
document.querySelectorAll('[data-view]').forEach(b=>b.onclick=()=>navigate(b.dataset.view));
$('#filters').onsubmit=ev=>{ev.preventDefault();reload()};
$('#game').onchange=()=>{$('#lastDraw').value='';state.latestDraw=null;reload()};
$('#reset').onclick=()=>{$('#from').value='';$('#to').value='';$('#rowStep').value='1';$('#startDraw').value='1';$('#lastDraw').value='';reload()};
window.addEventListener('hashchange',()=>{const view=location.hash.slice(1);if(view!==state.view)navigate(view)});
state.view=titles[location.hash.slice(1)]?location.hash.slice(1):'overview';
reload();
