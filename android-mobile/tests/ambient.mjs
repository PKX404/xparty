import {readFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
const code=await readFile(new URL('../app/src/main/assets/client/ambient.js',import.meta.url),'utf8');
const {extractPalette,createAmbient}=await import('data:text/javascript;base64,'+Buffer.from(code).toString('base64'));
const pixels=new Uint8ClampedArray(32*18*4);
for(let y=0;y<18;y++)for(let x=0;x<32;x++){const i=(y*32+x)*4;pixels[i+Math.min(2,Math.floor(x*3/32))]=180;pixels[i+3]=255;}
assert.deepEqual(extractPalette(pixels,32,18),[[180,24,24],[24,180,24],[24,24,180]]);
assert.deepEqual(extractPalette(new Uint8ClampedArray(32*18*4),32,18),[[89,118,73],[54,94,106],[101,75,116]]);
let draws=0,tainted=false;const timers=new Map(),events=new Map(),images=[];let sequence=0;
const originalTimeout=globalThis.setTimeout,originalClear=globalThis.clearTimeout;
globalThis.setTimeout=fn=>{timers.set(++sequence,fn);return sequence;};globalThis.clearTimeout=id=>timers.delete(id);
const ctx={drawImage(){draws++;if(tainted)throw new Error('SecurityError');},getImageData:()=>({data:pixels})};
const elements=[];function element(tag){const e={tag,style:{},dataset:{},children:[],hidden:false,setAttribute(){},appendChild(c){this.children.push(c);return c;},prepend(c){this.children.unshift(c);},remove(){this.removed=true;},getContext:()=>ctx};elements.push(e);return e;}
const media={matches:false,addEventListener(){},removeEventListener(){}};
globalThis.window={matchMedia:()=>media};globalThis.document={hidden:false,body:element('body'),createElement:element,addEventListener:(n,f)=>events.set(n,f),removeEventListener:n=>events.delete(n)};
globalThis.Image=class{constructor(){images.push(this);}set src(v){this.url=v;}};
Object.defineProperty(globalThis,'navigator',{configurable:true,value:{deviceMemory:8,hardwareConcurrency:8}});
let source={type:'file',id:'one'},active=true;
const video=Object.freeze({readyState:2,videoWidth:640,currentTime:12});
try{
 const a=createAmbient({video,getSource:()=>source,isActive:()=>active});a.setMode('auto');
 assert.equal(draws,1);a.refresh();assert.equal(draws,1,'unchanged frame is not sampled again');
 assert.equal(elements.filter(e=>e.tag==='video').length,0,'no second decoder');
 assert.equal(a.getState().scheduled,true);a.setMode('off');assert.equal(timers.size,0);assert.equal(a.getState().scheduled,false);
 a.setMode('auto');document.hidden=true;events.get('visibilitychange')();assert.equal(timers.size,0);
 document.hidden=false;events.get('visibilitychange')();assert.equal(a.getState().scheduled,true);
 media.matches=true;a.refresh();assert.equal(document.body.children[0].dataset.motion,'off');
 tainted=true;source={type:'file',id:'tainted'};a.refresh();const attempts=draws;a.refresh();assert.equal(draws,attempts,'tainted source not retried every tick');tainted=false;
 source={type:'youtube',videoId:'dQw4w9WgXcQ'};a.refresh();assert.equal(images.length,1);assert.equal(images[0].crossOrigin,'anonymous');
 const staleLoad=images[0].onload;source={type:'youtube',videoId:'abcdefghijk'};a.refresh();const before=draws;staleLoad();assert.equal(draws,before,'stale artwork never wins');images[1].onload();assert.equal(draws,before+1);
 a.setMode('off');assert.equal(timers.size,0);a.setMode('auto');active=false;a.refresh();assert.equal(timers.size,0);
 a.dispose();assert.equal(events.size,0);assert.equal(timers.size,0);assert.equal(document.body.children[0].removed,true);
 console.log('PASS: palette, single decoder, frame deduplication, Off, visibility, reduced motion, CORS failure, source race, disposal');
}finally{globalThis.setTimeout=originalTimeout;globalThis.clearTimeout=originalClear;}
