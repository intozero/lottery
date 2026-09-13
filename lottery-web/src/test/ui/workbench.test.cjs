const {test}=require('node:test');
const assert=require('node:assert/strict');
const {JSDOM,VirtualConsole}=require('jsdom');
const fs=require('node:fs');
const path=require('node:path');
const resources=path.resolve(__dirname,'../../main/resources/static');
const draws=[{drawNumber:1,date:'2026-01-01',whites:[1,2,3,4,5],special:6},{drawNumber:2,date:'2026-01-02',whites:[1,2,3,4,6],special:2}];
const analysis={
 count:2,latest:draws[1],totalSum:31,averageSum:15.5,
 numbers:Array.from({length:69},(_,i)=>({number:i+1,total:i<4?2:i<6?1:0,since:i===4?1:i<6?0:2,lastDate:i<6?'2026-01-02':null,minGap:i<4?1:null,maxGap:i<4?1:null})),
 sums:draws.map((d,i)=>({...d,sum:15+i,mean:(15+i)/5,deviation:1.5,runningTotal:i?31:15,runningAverage:i?15.5:15})),
 sumCounts:{15:1,16:1},deviations:{1:2},first:{1:2},last:{5:1,6:1},endSums:{6:1,7:1},
 ranges:[{range:'1-9',total:10,since:6}],patterns:{'5-0-0-0-0-0-0':2},shapes:{5:2},occupancies:{'1-9: 5 balls':2},repeated:[]
};
async function setup(analysisData=analysis,historyData=draws){
 const errors=[],calls=[],vc=new VirtualConsole();vc.on('jsdomError',e=>errors.push(e));
 const dom=new JSDOM(fs.readFileSync(path.join(resources,'index.html'),'utf8'),{url:'http://localhost:8080',runScripts:'outside-only',virtualConsole:vc});
 dom.window.fetch=async (url,options={})=>{
  calls.push({url,options});
  const endpoint=new URL(url,'http://localhost').pathname;
  let data;
  switch(endpoint){
   case '/api/analysis':data=analysisData;break;
   case '/api/history':data=historyData;break;
   case '/api/history-bounds':data={lastDraw:historyData.at(-1)?.drawNumber||0};break;
   case '/api/csrf':data={header:'X-CSRF-TOKEN',token:'test'};break;
   case '/api/ranges':data=[{pattern:'5-0-0-0-0-0-0',observed:2,combinations:126,square:25},{pattern:'0-5-0-0-0-0-0',observed:0,combinations:252,square:25}];break;
   case '/api/digits':data={digits:12,windows:3,unique:3,rows:[{pattern:'12345',count:2}],truncated:false};break;
   case '/api/timeline':data=[{date:'2026-01-01',draw:1,appeared:true,total:1,since:0}];break;
   case '/api/combinations':data={rows:[[1,2,3,4,5]],truncated:false};break;
   case '/api/imports':case '/api/changes':data=[];break;
   case '/api/import-text':case '/api/import':case '/api/powerball/sync':data={added:1,corrected:0,unchanged:1};break;
   default:throw new Error('Unexpected endpoint '+endpoint);
  }
  return {ok:true,json:async()=>structuredClone(data)};
 };
 dom.window.eval(fs.readFileSync(path.join(resources,'app.js'),'utf8'));
 await settle();
 return {dom,doc:dom.window.document,calls,errors};
}
async function settle(){for(let i=0;i<8;i++)await new Promise(r=>setImmediate(r));}
function submit(dom,form){
 const event=new dom.window.Event('submit',{bubbles:true,cancelable:true});
 Object.defineProperty(event,'submitter',{value:form.querySelector('button')});form.dispatchEvent(event);
}
test('all workspace pages render with live-data-shaped responses',async()=>{
 const {dom,doc,errors}=await setup();
 try{
  for(const view of ['overview','history','numbers','occurrences','sums','ranges','digits','combinations','data']){
   doc.querySelector('[data-view="'+view+'"]').click();await settle();
   assert.ok(doc.querySelector('#content').textContent.trim().length>30,view);
   assert.equal(doc.querySelector('[data-view="'+view+'"]').getAttribute('aria-current'),'page');
   assert.equal(doc.querySelector('#notice').hidden,true,doc.querySelector('#notice').textContent);
  }
  assert.deepEqual(errors,[]);
 }finally{dom.window.close();}
});
test('table search, pagination and dates trigger the intended behavior',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  doc.querySelector('[data-view="numbers"]').click();await settle();
  const root=doc.querySelector('#numberTable');assert.equal(root.querySelectorAll('tbody tr').length,50);
  root.querySelectorAll('.pagination button')[1].click();assert.equal(root.querySelectorAll('tbody tr').length,19);
  const search=root.querySelector('input');search.value='69';search.dispatchEvent(new dom.window.Event('input'));assert.equal(root.querySelectorAll('tbody tr').length,1);
  doc.querySelector('#from').value='2026-01-02';submit(dom,doc.querySelector('#filters'));await settle();
  assert.ok(calls.some(c=>c.url.includes('from=2026-01-02')));
 }finally{dom.window.close();}
});
test('analysis forms, unseen filter, and timeline work',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  for(const [view,form,result] of [['digits','digitForm','digitTable'],['combinations','comboForm','comboTable'],['numbers','numberForm','timeline']]){
   doc.querySelector('[data-view="'+view+'"]').click();await settle();
   submit(dom,doc.getElementById(form));await settle();
   assert.ok(doc.getElementById(result).querySelector('tbody tr'),view);
  }
  doc.querySelector('[data-view="ranges"]').click();await settle();
  const box=doc.querySelector('#unseen');box.checked=true;box.dispatchEvent(new dom.window.Event('change'));
  assert.equal(doc.querySelectorAll('#universe tbody tr').length,1);
  assert.ok(calls.some(c=>c.url.startsWith('/api/digits')));
 }finally{dom.window.close();}
});
test('import and sync send CSRF tokens and display results',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  doc.querySelector('[data-view="data"]').click();await settle();
  doc.querySelector('#paste').value='1/1/2026 1 2 3 4 5 6';submit(dom,doc.querySelector('#pasteForm'));await settle();
  assert.ok(doc.querySelector('#notice').textContent.includes('1 draws added'));
  const mutation=calls.find(c=>c.url==='/api/import-text');assert.equal(mutation.options.headers['X-CSRF-TOKEN'],'test');
  doc.querySelector('#sync').click();await settle();
  assert.ok(calls.find(c=>c.url==='/api/powerball/sync').options.headers['X-CSRF-TOKEN']);
 }finally{dom.window.close();}
});
test('network failures produce a visible error instead of an empty workspace',async()=>{
 const {dom,doc}=await setup();
 try{
  dom.window.fetch=async()=>({ok:false,status:400,json:async()=>({error:'Invalid date range'})});
  submit(dom,doc.querySelector('#filters'));await settle();
  assert.equal(doc.querySelector('#notice').hidden,false);
  assert.match(doc.querySelector('#notice').textContent,/Invalid date range/);
 }finally{dom.window.close();}
});

test('row step applies to every history request and export, stays applied until submit, and resets',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  doc.querySelector('#startDraw').value='2';
  doc.querySelector('#rowStep').value='5';submit(dom,doc.querySelector('#filters'));await settle();
  assert.match(doc.querySelector('#scope').textContent,/row step 5/);
  assert.match(doc.querySelector('#scope').textContent,/starting draw 2/);
  doc.querySelector('#startDraw').value='3';
  doc.querySelector('#rowStep').value='2';
  for(const [view,form] of [['history',null],['numbers','numberForm'],['digits','digitForm'],['ranges',null],['sums',null],['data',null]]){
   doc.querySelector('[data-view="'+view+'"]').click();await settle();
   if(form){submit(dom,doc.getElementById(form));await settle();}
   for(const a of doc.querySelectorAll('a[href^="/api/export"]')){
    assert.equal(new URL(a.href).searchParams.get('rowStep'),'5');
    assert.equal(new URL(a.href).searchParams.get('startDraw'),'2');
   }
  }
  for(const endpoint of ['history','analysis','timeline','digits','ranges']){
   const call=calls.filter(c=>c.url.startsWith('/api/'+endpoint+'?')).at(-1);
   assert.equal(new URL(call.url,'http://localhost').searchParams.get('rowStep'),'5',endpoint);
   assert.equal(new URL(call.url,'http://localhost').searchParams.get('startDraw'),'2',endpoint);
  }
  doc.querySelector('#reset').click();await settle();
  assert.equal(doc.querySelector('#rowStep').value,'1');
  assert.equal(doc.querySelector('#startDraw').value,'1');
  assert.match(doc.querySelector('#scope').textContent,/row step 1/);
  const count=calls.length;
  for(const field of ['rowStep','startDraw'])
  for(const value of ['0','-1','1.5','']){
   doc.querySelector('#rowStep').value='1';doc.querySelector('#startDraw').value='1';
   doc.getElementById(field).value=value;submit(dom,doc.querySelector('#filters'));await settle();
   assert.equal(calls.length,count);
  }
 }finally{dom.window.close();}
});

test('draw history shows original draw numbers when sorted newest or oldest first',async()=>{
 const {dom,doc}=await setup();
 try{
  doc.querySelector('[data-view="history"]').click();await settle();
  const numbers=()=>Array.from(doc.querySelectorAll('#drawTable tbody tr'),r=>r.cells[0].textContent);
  assert.deepEqual(numbers(),['2','1']);
  doc.querySelector('#drawTable [data-sort="drawNumber"]').click();
  assert.deepEqual(numbers(),['1','2']);
 }finally{dom.window.close();}
});

test('sum chart includes the full distribution, numeric gaps, and proportional counts',async()=>{
 const counts=Object.fromEntries(Array.from({length:100},(_,i)=>[15+i,1]));
 counts[200]=4;counts[202]=2;
 const {dom,doc}=await setup({...analysis,sumCounts:counts});
 try{
  doc.querySelector('[data-view="sums"]').click();await settle();
  const bin=n=>doc.querySelector('.sum-bin[data-sum="'+n+'"]');
  assert.ok(bin(202),'values beyond the first 90 sums must be plotted');
  assert.equal(bin(201).dataset.count,'0');
  const bar=n=>bin(n).querySelector('.sum-bar');
  assert.equal(Number(bar(200).getAttribute('height')),2*Number(bar(202).getAttribute('height')));
  const x=n=>Number(bar(n).getAttribute('x'));
  assert.ok(Math.abs((x(202)-x(200))-2*(x(201)-x(200)))<1e-8);
  assert.equal(Array.from(doc.querySelectorAll('.sum-bin')).reduce((sum,b)=>sum+Number(b.dataset.count),0),106);
  bin(200).dispatchEvent(new dom.window.Event('mouseenter'));
  assert.equal(doc.querySelector('#sumChartDetail').textContent,'Sum 200: 4 draws (3.77%)');
  bin(201).focus();assert.match(doc.querySelector('#sumChartDetail').textContent,/Sum 201: 0 draws/);
  assert.match(doc.querySelector('.sum-chart').textContent,/Draw count/);
 }finally{dom.window.close();}
});
test('sum chart handles a single sum and empty selections',async()=>{
 for(const counts of [{188:1},{}]){
  const {dom,doc,errors}=await setup({...analysis,sumCounts:counts});
  try{
   doc.querySelector('[data-view="sums"]').click();await settle();
   assert.deepEqual(errors,[]);
   assert.doesNotMatch(doc.querySelector('#content').innerHTML,/NaN|Infinity/);
   if(counts[188])assert.match(doc.querySelector('.sum-bin').getAttribute('aria-label'),/Sum 188: 1 draw \(100.00%\)/);
   else assert.equal(doc.querySelector('.sum-chart'),null);
  }finally{dom.window.close();}
 }
});

test('ball occurrences use original draw gaps and cumulative matches, excluding special-only hits',async()=>{
 const history=[
  {drawNumber:2,date:'2026-01-02',whites:[1,2,3,4,5],special:6},
  {drawNumber:5,date:'2026-01-05',whites:[2,3,4,5,6],special:1},
  {drawNumber:8,date:'2026-01-08',whites:[1,3,4,5,6],special:2},
  {drawNumber:11,date:'2026-01-11',whites:[1,2,4,5,6],special:3}
 ];
 const {dom,doc}=await setup(analysis,history);
 try{
  doc.querySelector('[data-view="occurrences"]').click();await settle();
  const rows=()=>Array.from(doc.querySelectorAll('#occurrenceTable tbody tr'),r=>[r.cells[0].textContent,r.cells[1].textContent,r.cells[4].textContent,r.cells[5].textContent]);
  assert.deepEqual(rows(),[['2026-01-02','2','—','1'],['2026-01-08','8','6','2'],['2026-01-11','11','3','3']]);
  doc.querySelector('#occurrenceTable [data-sort="drawNumber"]').click();
  doc.querySelector('#occurrenceTable [data-sort="drawNumber"]').click();
  assert.deepEqual(rows()[0],['2026-01-11','11','3','3']);
  doc.querySelector('#occurrenceNumber').value='3';submit(dom,doc.querySelector('#occurrenceForm'));
  assert.deepEqual(rows().map(r=>r.slice(1)),[['2','—','1'],['5','3','2'],['8','3','3']]);
  doc.querySelector('#startDraw').value='2';submit(dom,doc.querySelector('#filters'));await settle();
  assert.equal(doc.querySelector('#occurrenceNumber').value,'3');
  assert.match(doc.querySelector('#occurrenceStatus').textContent,/White ball 3/);
  doc.querySelector('#occurrenceNumber').value='69';submit(dom,doc.querySelector('#occurrenceForm'));
  assert.equal(rows().length,0);
  assert.match(doc.querySelector('#occurrenceStatus').textContent,/0 occurrences/);
  for(const value of ['0','70','1.5','']){
   doc.querySelector('#occurrenceNumber').value=value;submit(dom,doc.querySelector('#occurrenceForm'));
   assert.match(doc.querySelector('#occurrenceStatus').textContent,/White ball 69/);
  }
 }finally{dom.window.close();}
});

test('occurrence chart supports measures, ordering, exact inspection and history shortcuts',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  doc.querySelector('[data-view="numbers"]').click();await settle();
  const bin=n=>doc.querySelector('.number-bin[data-number="'+n+'"]');
  assert.equal(doc.querySelectorAll('.number-bin').length,69);
  assert.equal(doc.querySelectorAll('.number-bar').length,138);
  const height=(n,key)=>Number(bin(n).querySelector('.'+key+'-bar').getAttribute('height'));
  assert.equal(height(1,'total'),2*height(5,'total'));
  assert.equal(height(69,'total'),0);
  bin(69).focus();assert.match(doc.querySelector('#numberChartDetail').textContent,/NEVER seen/);
  assert.equal(doc.querySelector('#chartTimeline').disabled,true);
  bin(5).dispatchEvent(new dom.window.KeyboardEvent('keydown',{key:'Enter',bubbles:true}));
  assert.equal(bin(5).getAttribute('aria-pressed'),'true');
  assert.equal(doc.querySelector('#trackNumber').value,'5');
  doc.querySelector('#numberMeasure').value='since';doc.querySelector('#numberMeasure').dispatchEvent(new dom.window.Event('change'));
  assert.equal(doc.querySelectorAll('.total-bar').length,0);
  assert.equal(doc.querySelectorAll('.since-bar').length,69);
  assert.equal(bin(5).getAttribute('aria-pressed'),'true');
  doc.querySelector('#numberOrder').value='since';doc.querySelector('#numberOrder').dispatchEvent(new dom.window.Event('change'));
  assert.equal(doc.querySelector('.number-bin').dataset.number,'7');
  doc.querySelector('#chartTimeline').click();await settle();
  assert.ok(calls.some(c=>c.url.startsWith('/api/timeline?')&&new URL(c.url,'http://localhost').searchParams.get('number')==='5'));
  assert.ok(doc.querySelector('#timeline tbody tr'));
  doc.querySelector('#chartOccurrences').click();await settle();
  assert.equal(doc.querySelector('#occurrenceNumber').value,'5');
  assert.match(doc.querySelector('#occurrenceStatus').textContent,/White ball 5/);
 }finally{dom.window.close();}
});
test('occurrence chart handles empty history and the full MM number universe',async()=>{
 const {dom,doc}=await setup({...analysis,numbers:Array.from({length:75},(_,i)=>({number:i+1,total:0,since:0,lastDate:null,minGap:null,maxGap:null}))},[]);
 try{
  doc.querySelector('[data-view="numbers"]').click();await settle();
  assert.match(doc.querySelector('#numberChart').textContent,/No draws/);
  assert.equal(doc.querySelector('#chartOccurrences').disabled,true);
  assert.doesNotMatch(doc.querySelector('#numberChart').innerHTML,/NaN|Infinity/);
 }finally{dom.window.close();}
 const populated=await setup({...analysis,numbers:Array.from({length:75},(_,i)=>({number:i+1,total:i===74?1:0,since:i===74?0:2,lastDate:i===74?'2026-01-02':null,minGap:null,maxGap:null}))});
 try{
  populated.doc.querySelector('#game').value='MM';populated.doc.querySelector('#game').dispatchEvent(new populated.dom.window.Event('change'));await settle();
  populated.doc.querySelector('[data-view="numbers"]').click();await settle();
  assert.equal(populated.doc.querySelectorAll('.number-bin').length,75);
  populated.doc.querySelector('.number-bin[data-number="75"]').dispatchEvent(new populated.dom.window.MouseEvent('click',{bubbles:true}));
  assert.equal(populated.doc.querySelector('#trackNumber').value,'75');
 }finally{populated.dom.window.close();}
});

test('last draw defaults to latest, scopes requests and exports, and resets after game changes',async()=>{
 const {dom,doc,calls}=await setup();
 try{
  assert.equal(doc.querySelector('#lastDraw').value,'2');
  doc.querySelector('#lastDraw').value='1';submit(dom,doc.querySelector('#filters'));await settle();
  for(const endpoint of ['history','analysis'])assert.equal(new URL(calls.filter(c=>c.url.startsWith('/api/'+endpoint+'?')).at(-1).url,'http://localhost').searchParams.get('lastDraw'),'1');
  assert.match(doc.querySelector('#scope').textContent,/last draw 1/);
  doc.querySelector('[data-view="numbers"]').click();await settle();
  assert.equal(new URL(doc.querySelector('a[href^="/api/export"]').href).searchParams.get('lastDraw'),'1');
  submit(dom,doc.querySelector('#numberForm'));await settle();
  assert.equal(new URL(calls.filter(c=>c.url.startsWith('/api/timeline?')).at(-1).url,'http://localhost').searchParams.get('lastDraw'),'1');
  const fetch=dom.window.fetch;
  dom.window.fetch=async(url,options)=>url.startsWith('/api/history-bounds?')?{ok:true,json:async()=>({lastDraw:3})}:fetch(url,options);
  submit(dom,doc.querySelector('#filters'));await settle();
  assert.equal(doc.querySelector('#lastDraw').value,'1');
  doc.querySelector('#reset').click();await settle();
  assert.equal(doc.querySelector('#lastDraw').value,'3');
  doc.querySelector('#lastDraw').value='1';submit(dom,doc.querySelector('#filters'));await settle();
  doc.querySelector('#game').value='MM';doc.querySelector('#game').dispatchEvent(new dom.window.Event('change'));await settle();
  assert.equal(doc.querySelector('#lastDraw').value,'3');
  doc.querySelector('#lastDraw').value='';submit(dom,doc.querySelector('#filters'));await settle();
  assert.equal(doc.querySelector('#lastDraw').value,'3');
  const count=calls.length;
  for(const value of ['0','-1','1.5']){
   doc.querySelector('#lastDraw').value=value;submit(dom,doc.querySelector('#filters'));await settle();assert.equal(calls.length,count);
  }
 }finally{dom.window.close();}
});
