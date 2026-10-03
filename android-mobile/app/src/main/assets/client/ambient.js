// Decorative only: never creates a media element or writes playback state.
const FALLBACK = [[89,118,73],[54,94,106],[101,75,116]];
export function extractPalette(data, width, height) {
 const buckets = [new Map(), new Map(), new Map()];
 for (let i=0;i<width*height*4;i+=4) {
  const r=data[i],g=data[i+1],b=data[i+2];
  if(data[i+3]<200||Math.max(r,g,b)<24||Math.min(r,g,b)>238)continue;
  const region=Math.min(2,Math.floor((i/4%width)*3/width));
  const key=(r>>5)*64+(g>>5)*8+(b>>5),weight=1+(Math.max(r,g,b)-Math.min(r,g,b))/128;
  const bucket=buckets[region].get(key)||[0,0,0,0];
  bucket[0]+=r*weight;bucket[1]+=g*weight;bucket[2]+=b*weight;bucket[3]+=weight;buckets[region].set(key,bucket);
 }
 return buckets.map((map,i)=>{
  const best=[...map.values()].sort((a,b)=>b[3]-a[3])[0];
  return best?best.slice(0,3).map(v=>Math.max(24,Math.min(200,Math.round(v/best[3])))):[...FALLBACK[i]];
 });
}
export function createAmbient({video,getSource,isActive,host=document.body}) {
 const light=document.createElement('div');light.id='ambient-light';light.setAttribute('aria-hidden','true');
 const blobs=Array.from({length:3},()=>light.appendChild(document.createElement('i')));host.prepend(light);
 const canvas=document.createElement('canvas');canvas.width=32;canvas.height=18;
 let ctx;try{ctx=canvas.getContext('2d',{willReadFrequently:true});}catch{}
 const motion=window.matchMedia('(prefers-reduced-motion: reduce)');
 let mode='auto',timer=null,disposed=false,key='',failed='',lastFrame=-1,generation=0,pending=null,cost=0;
 let samples=0,artworkLoads=0,palette=FALLBACK.map(c=>[...c]),lastCost=0;
 const limited=()=>motion.matches||navigator.connection?.saveData||navigator.deviceMemory<=4||navigator.hardwareConcurrency<=4;
 function apply(colors){palette=colors;colors.forEach((c,i)=>blobs[i].style.backgroundColor=`rgb(${c.join(',')})`);}
 function stop(){if(timer!==null)clearTimeout(timer);timer=null;}
 function cancelImage(){generation++;if(pending){clearTimeout(pending.timeout);pending.img.onload=pending.img.onerror=null;pending.img.src='';pending=null;}}
 function sample(image){const start=performance.now();ctx.drawImage(image,0,0,32,18);apply(extractPalette(ctx.getImageData(0,0,32,18).data,32,18));lastCost=performance.now()-start;cost=cost*.75+lastCost*.25;samples++;}
 function artwork(id){
  if(!/^[a-zA-Z0-9_-]{11}$/.test(id||''))return;
  const ticket=++generation,img=new Image();artworkLoads++;img.crossOrigin='anonymous';
  const finish=()=>{if(pending?.img===img){clearTimeout(pending.timeout);pending=null;}img.onload=img.onerror=null;};
  img.onload=()=>{if(ticket===generation&&!disposed&&mode!=='off'&&!document.hidden&&isActive())try{sample(img);}catch{failed=key;apply(FALLBACK);}finish();};
  img.onerror=()=>{if(ticket===generation){failed=key;apply(FALLBACK);}finish();};
  pending={img,timeout:setTimeout(()=>{if(ticket===generation){failed=key;apply(FALLBACK);}finish();img.src='';},8000)};
  img.src=`https://i.ytimg.com/vi/${id}/mqdefault.jpg`;
 }
 function refresh(){
  stop();if(disposed)return;
  const active=mode!=='off'&&!document.hidden&&isActive();light.hidden=!active;
  light.dataset.motion=active&&mode==='auto'&&!limited()?'on':'off';light.dataset.mode=mode;
  if(!active){cancelImage();key='';return;}
  const source=getSource(),next=`${source?.type||''}:${source?.videoId||source?.id||source?.fileId||''}`;
  if(next!==key){cancelImage();key=next;failed='';lastFrame=-1;apply(FALLBACK);if(source?.type==='youtube'&&ctx)artwork(source.videoId);}
  if(ctx&&source?.type==='file'&&failed!==key&&video.readyState>=2&&video.videoWidth>0&&video.currentTime!==lastFrame){
   lastFrame=video.currentTime;try{sample(video);}catch{failed=key;apply(FALLBACK);}
  }
  timer=setTimeout(refresh,cost>8?1800:limited()||mode==='soft'?1400:800);
 }
 const visibility=()=>refresh();document.addEventListener('visibilitychange',visibility);motion.addEventListener?.('change',visibility);
 apply(palette);
 return {
  setMode(value){mode=['auto','soft','off'].includes(value)?value:'auto';refresh();},refresh,
  getState:()=>({mode,samples,artworkLoads,lastCost,scheduled:timer!==null,source:key,palette:palette.map(c=>[...c])}),
  dispose(){disposed=true;stop();cancelImage();document.removeEventListener('visibilitychange',visibility);motion.removeEventListener?.('change',visibility);light.remove();ctx=null;}
 };
}
export function mountAmbient(options){
 const ambient=createAmbient(options),storageKey='xparty-ambient-v1';let value='auto';
 const remember=()=>{try{return JSON.parse(localStorage.getItem('xparty-consent')||'{}').preferences===true;}catch{return false;}};
 try{value=(remember()?localStorage:sessionStorage).getItem(storageKey)||'auto';}catch{}
 const section=document.createElement('details');section.id='ambient-settings';
 section.innerHTML='<summary>Ambient background</summary><p class="small">Soft colour around your room. YouTube uses artwork; local videos use their playing frames.</p><div class="ambient-options" role="group" aria-label="Ambient background"><button type="button" data-mode="auto">Auto</button><button type="button" data-mode="soft">Soft</button><button type="button" data-mode="off">Off</button></div><p class="small">Auto adapts to device hints. Soft removes movement. Off stops the effect and its sampling.</p>';
 const buttons=[...section.querySelectorAll('button')];
 function select(mode){value=['auto','soft','off'].includes(mode)?mode:'auto';ambient.setMode(value);buttons.forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.mode===value)));}
 buttons.forEach(b=>b.onclick=()=>{select(b.dataset.mode);try{(remember()?localStorage:sessionStorage).setItem(storageKey,value);}catch{}});
 document.getElementById('settings-dialog').append(section);
 const consent=()=>{try{if(!remember()){localStorage.removeItem(storageKey);sessionStorage.setItem(storageKey,value);}}catch{}};window.addEventListener('xparty:consent',consent);
 const observer=new MutationObserver(()=>ambient.refresh());observer.observe(document.getElementById('room'),{attributes:true,attributeFilter:['hidden']});
 const link=document.createElement('link');link.rel='stylesheet';link.href='ambient.css';document.head.append(link);
 select(value);window.XPARTY_AMBIENT_STATE=ambient.getState;
 window.addEventListener('pagehide',()=>{observer.disconnect();ambient.dispose();window.removeEventListener('xparty:consent',consent);},{once:true});
 return ambient;
}
