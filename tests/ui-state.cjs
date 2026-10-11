const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const elements=new Map();
function element(){return {value:'',textContent:'',dataset:{},style:{setProperty(){}},classList:{add(){},remove(){},toggle(){}},append(){},replaceChildren(){},setAttribute(){},addEventListener(){},querySelector(){return element()},pause(){},play(){return Promise.resolve()},load(){},removeAttribute(){},showModal(){this.open=true},close(){this.open=false}};}
const storage={getItem(){return null},setItem(){},removeItem(){}};
let respond;
const context=vm.createContext({console,URLSearchParams,AbortController,setTimeout(){return 1},clearTimeout(){},location:{hash:'',pathname:'/',host:'local'},history:{replaceState(){}},sessionStorage:storage,localStorage:storage,Native:{},window:{scrollTo(){}},document:{getElementById(id){if(!elements.has(id))elements.set(id,element());return elements.get(id)},querySelectorAll(){return []},createElement:element,documentElement:element(),body:element()},fetch:(...args)=>respond(...args),confirm:()=>true});
vm.runInContext(fs.readFileSync('app/src/main/assets/app.js','utf8'),context);
const run=s=>vm.runInContext(s,context);
(async()=>{
 run("renderState=()=>{}; token='test';");
 respond=async()=>({status:400,ok:false,json:async()=>({error:'Queue is full'})});
 assert.equal(await run("command('reserve',{id:'000001'})"),false);
 assert.equal(elements.get('banner').textContent,'Queue is full');
 let active=0,maxActive=0,requests=[];
 respond=async(url,opt)=>{active++;maxActive=Math.max(maxActive,active);requests.push(JSON.parse(opt.body));await new Promise(r=>setImmediate(r));active--;return {ok:true,status:200,json:async()=>({key:requests.length})};};
 await Promise.all([run("command('key',{delta:1})"),run("command('key',{delta:1})")]);
 assert.equal(maxActive,1);assert.deepEqual(requests.map(x=>x.delta),[1,1]);
 let resolvePoll;
 respond=()=>new Promise(resolve=>resolvePoll=resolve);
 const pending=run('poll()');
 run('stateEpoch++;state={key:6};');
 resolvePoll({ok:true,status:200,json:async()=>({key:0})});await pending;
 assert.equal(run('state.key'),6,'stale poll must not replace newer command state');
 console.log('PASS: failed actions, ordered rapid controls and stale poll protection');
})().catch(e=>{console.error(e);process.exitCode=1;});
