const {test}=require('node:test');
const assert=require('node:assert/strict');
const {JSDOM,VirtualConsole}=require('jsdom');
const fs=require('node:fs');
const path=require('node:path');
const resources=path.resolve(__dirname,'../../main/resources/static');
const draws=[{date:'2026-01-01',whites:[1,2,3,4,5],special:6},{date:'2026-01-02',whites:[1,2,3,4,6],special:2}];
const analysis={
 count:2,latest:draws[1],totalSum:31,averageSum:15.5,
 numbers:Array.from({length:69},(_,i)=>({number:i+1,total:i<4?2:i<6?1:0,since:i===4?1:i<6?0:2,lastDate:i<6?'2026-01-02':null,minGap:i<4?1:null,maxGap:i<4?1:null})),
 sums:draws.map((d,i)=>({...d,sum:15+i,mean:(15+i)/5,deviation:1.5,runningTotal:i?31:15,runningAverage:i?15.5:15})),
 sumCounts:{15:1,16:1},deviations:{1:2},first:{1:2},last:{5:1,6:1},endSums:{6:1,7:1},
 ranges:[{range:'1-9',total:10,since:6}],patterns:{'5-0-0-0-0-0-0':2},shapes:{5:2},occupancies:{'1-9: 5 balls':2},repeated:[]
};
async function setup(){
 const errors=[],calls=[],vc=new VirtualConsole();vc.on('jsdomError',e=>errors.push(e));
 const dom=new JSDOM(fs.readFileSync(path.join(resources,'index.html'),'utf8'),{url:'http://localhost:8080',runScripts:'outside-only',virtualConsole:vc});
 dom.window.fetch=async (url,options={})=>{
  calls.push({url,options});
  const endpoint=new URL(url,'http://localhost').pathname;
  let data;
  switch(endpoint){
   case '/api/analysis':data=analysis;break;
   case '/api/history':data=draws;break;
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
  for(const view of ['overview','history','numbers','sums','ranges','digits','combinations','data']){
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
