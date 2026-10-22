import {createImmersiveRenderer} from './immersive.js';

export function colorsFromPixels(data,width,height){
 const sums=Array.from({length:3},()=>[0,0,0,0]);
 for(let y=0;y<height;y++)for(let x=0;x<width;x++){const i=(y*width+x)*4;if(data[i+3]<128)continue;const s=sums[Math.min(2,Math.floor(x*3/width))];for(let c=0;c<3;c++)s[c]+=data[i+c];s[3]++;}
 return sums.map(s=>s[3]?s.slice(0,3).map(v=>Math.round(v/s[3])):[47,66,87]);
}
export function ambientActive({enabled,hidden,reduced,room,playing,visible=true}){return !!(enabled&&!hidden&&!reduced&&room?.source&&playing&&visible);}
export function validAmbientCapture(track,expectedHandle,expectedOrigin){try{const h=track?.getCaptureHandle?.();return track?.getSettings?.().displaySurface==='browser'&&h?.handle===expectedHandle&&h?.origin===expectedOrigin;}catch{return false;}}
export function captureCrop(rect,viewportWidth,viewportHeight,videoWidth,videoHeight){
 if(!rect||![viewportWidth,viewportHeight,videoWidth,videoHeight].every(n=>Number.isFinite(n)&&n>0))return null;
 const left=Math.max(0,Math.min(viewportWidth,rect.left)),top=Math.max(0,Math.min(viewportHeight,rect.top)),right=Math.max(left,Math.min(viewportWidth,rect.right)),bottom=Math.max(top,Math.min(viewportHeight,rect.bottom));
 if(![left,top,right,bottom].every(Number.isFinite)||right<=left||bottom<=top)return null;
 return {x:left*videoWidth/viewportWidth,y:top*videoHeight/viewportHeight,width:(right-left)*videoWidth/viewportWidth,height:(bottom-top)*videoHeight/viewportHeight};
}
export function stopAmbientStream(stream){for(const track of stream?.getTracks?.()||[])track.stop();}

// One pending callback, driven by presented video frames rather than a slow timer.
export function createFramePump(video,onFrame,{active=()=>true,raf=globalThis.requestAnimationFrame,caf=globalThis.cancelAnimationFrame}={}){
 let pending=null,stopped=true,lastTime=-1;const native=typeof video.requestVideoFrameCallback==='function';
 const request=native?fn=>video.requestVideoFrameCallback(fn):raf|| (fn=>setTimeout(()=>fn(performance.now()),16));
 const cancel=native?id=>video.cancelVideoFrameCallback?.(id):caf||clearTimeout;
 function schedule(){if(stopped||pending!==null||!active())return;pending=request((now,metadata)=>{pending=null;if(stopped||!active())return;const time=metadata?.mediaTime??video.currentTime;if(native||time!==lastTime){lastTime=time;onFrame(now,metadata);}schedule();});}
 return {start(){stopped=false;schedule();},stop(){stopped=true;if(pending!==null)cancel(pending);pending=null;},get pending(){return pending!==null;}};
}

export function initAmbient({getContext,panel=document.getElementById('room')}={}){
 if(!panel||typeof getContext!=='function')return {update(){},destroy(){}};
 const preference='xparty.ambient-chat';let enabled=true;try{enabled=localStorage.getItem(preference)!=='off';}catch{}
 const motion=matchMedia('(prefers-reduced-motion: reduce)');
 const layer=document.createElement('div');layer.className='room-ambient-layer';layer.setAttribute('aria-hidden','true');panel.prepend(layer);
 const renderer=createImmersiveRenderer(layer),toolbar=document.querySelector?.('.theatre-toggle-group')||panel.querySelector?.('.panel-title')||panel;
 const button=document.createElement('button');button.id='immersive-toggle';button.type='button';button.className='ambient-chat-toggle secondary';button.textContent='Immersive';button.title='Video light across the whole room';toolbar.append(button);
 const live=document.createElement('button');live.id='immersive-live-toggle';live.type='button';live.className='ambient-chat-toggle secondary';live.textContent='Live background';live.title='Use colors from this Xparty tab';
 const note=document.createElement('p');note.className='ambient-chat-note';note.setAttribute('role','status');note.hidden=true;(document.querySelector?.('.theatre-toolbar')||panel).append(note);
 const fallback=document.createElement('canvas');fallback.width=12;fallback.height=8;const ctx=fallback.getContext('2d',{willReadFrequently:true});
 let key=null,image=null,failed=false,destroyed=false,hasFrame=false,idleTimer=null,frameVideo=null,pump=null,lastFallbackAt=-Infinity;
 const media=navigator.mediaDevices,origin=location.origin;let handle=null,capture=null,captureVideo=null,captureTrack=null,captureGeneration=0,capturePending=false;
 const embedded=source=>['youtube','vimeo','twitch','soundcloud'].includes(source?.type);
 try{if(media?.getDisplayMedia&&media?.setCaptureHandleConfig&&typeof MediaStreamTrack!=='undefined'&&MediaStreamTrack.prototype.getCaptureHandle){handle=crypto.randomUUID();media.setCaptureHandleConfig({handle,exposeOrigin:true,permittedOrigins:[origin]});toolbar.append(live);}}catch{handle=null;}
 function status(text){note.textContent=text;note.hidden=!text;}
 function stopFrames(){pump?.stop();pump=null;frameVideo=null;}
 function stopCapture(message=''){captureGeneration++;capturePending=false;stopFrames();const old=capture;capture=null;captureTrack=null;if(captureVideo){captureVideo.pause();captureVideo.srcObject=null;captureVideo=null;}stopAmbientStream(old);live.disabled=false;live.textContent='Live background';live.setAttribute('aria-pressed','false');failed=false;hasFrame=false;if(message)status(message);}
 function verifyCapture(){if(captureTrack&&!validAmbientCapture(captureTrack,handle,origin)){stopCapture('Choose this XParty tab to use its video background.');return false;}return !!captureTrack;}
 async function startCapture(){
  if(capture){stopCapture();update();return;}
  if(!handle||!enabled||!embedded(getContext()?.room?.source)||capturePending)return;
  status('Choose this XParty tab. Video colors stay on your device.');capturePending=true;live.disabled=true;const generation=++captureGeneration;let stream;
  try{stream=await media.getDisplayMedia({video:{frameRate:{ideal:30,max:60},displaySurface:'browser'},audio:false,preferCurrentTab:true,selfBrowserSurface:'include',surfaceSwitching:'exclude',monitorTypeSurfaces:'exclude',systemAudio:'exclude'});
   if(destroyed||generation!==captureGeneration||!enabled||!embedded(getContext()?.room?.source)){stopAmbientStream(stream);return;}
   const track=stream.getVideoTracks()[0];if(!validAmbientCapture(track,handle,origin)){stopAmbientStream(stream);status('Choose this XParty tab. Other tabs and screens cannot supply this background.');return;}
   capture=stream;captureTrack=track;captureVideo=document.createElement('video');captureVideo.muted=true;captureVideo.playsInline=true;captureVideo.srcObject=stream;
   track.addEventListener('capturehandlechange',()=>{verifyCapture();update();});track.addEventListener('ended',()=>{if(capture===stream){stopCapture();update();}},{once:true});
   await captureVideo.play();if(generation!==captureGeneration)return;failed=false;hasFrame=false;live.textContent='Stop live background';live.setAttribute('aria-pressed','true');status('Live tab background · capture may trail the player slightly.');update();
  }catch{stopAmbientStream(stream);if(generation===captureGeneration){stopCapture();status('Live capture unavailable. Showing the room backdrop.');}}
  finally{if(generation===captureGeneration){capturePending=false;live.disabled=false;}}
 }
 function visible(){return !document.hidden&&!panel.hidden&&panel.getClientRects().length>0;}
 function animated(){const c=getContext()||{};return ambientActive({enabled,hidden:document.hidden,reduced:motion.matches,room:c.room,playing:c.playing??c.room?.playback?.playing,visible:visible()});}
 function cropFor(video){if(video!==captureVideo)return null;return captureCrop(document.getElementById('watch-stage')?.getBoundingClientRect(),innerWidth,innerHeight,video.videoWidth,video.videoHeight);}
 function sample(video,metadata){
  if(failed||destroyed||!enabled||!visible())return;
  const crop=cropFor(video);if(video===captureVideo&&(!verifyCapture()||!crop))return;
  const start=performance.now();
  try{
   const drawn=renderer.render(video,crop);
   if(drawn){hasFrame=true;if(layer.dataset)layer.dataset.mode=video===captureVideo?'capture':'video';}
   else if(!renderer.available&&ctx&&start-lastFallbackAt>=100){lastFallbackAt=start;if(crop)ctx.drawImage(video,crop.x,crop.y,crop.width,crop.height,0,0,12,8);else ctx.drawImage(video,0,0,12,8);colorsFromPixels(ctx.getImageData(0,0,12,8).data,12,8).forEach((rgb,i)=>layer.style.setProperty('--ambient-'+i,`rgb(${rgb.join(' ')})`));hasFrame=true;}
   else if(renderer.available){failed=true;status('This source does not expose video colors to the browser.');stopFrames();}
   if(layer.dataset){layer.dataset.renderMs=(performance.now()-start).toFixed(2);if(metadata?.mediaTime!==undefined)layer.dataset.mediaTime=String(metadata.mediaTime);}
  }catch{failed=true;stopFrames();}
 }
 function clear(){stopFrames();renderer.clear();key=null;image=null;failed=false;hasFrame=false;fallback.width=12;for(let i=0;i<3;i++)layer.style.removeProperty('--ambient-'+i);}
 function update(){
  clearTimeout(idleTimer);idleTimer=null;if(destroyed)return;
  const c=getContext()||{},source=c.room?.source,on=enabled&&!!source&&visible();button.setAttribute('aria-pressed',String(enabled));panel.classList.toggle('room-immersive-on',on);document.body?.classList.toggle('room-immersive-on',on);
  if(!enabled||!source){stopCapture();status('');clear();return;}
  live.hidden=!embedded(source);if(!embedded(source)&&(capture||capturePending))stopCapture();
  const nextKey=source.id||source.videoId||source.url;
  if(key!==nextKey){clear();key=nextKey;status('');if(source.type==='youtube'&&source.videoId){const expected=key;image=new Image();image.crossOrigin='anonymous';image.onload=()=>{if(!destroyed&&key===expected&&!capture){sample(image);status('Artwork background · choose Live background for moving colors.');}};image.src='https://i.ytimg.com/vi/'+encodeURIComponent(source.videoId)+'/hqdefault.jpg';}}
  const video=['file','direct'].includes(source.type)?c.video:captureVideo;
  if(video&&video.readyState>=2&&on&&!failed){
   if(!hasFrame)sample(video);
   if(animated()&&!video.paused&&!video.ended){if(frameVideo!==video||!pump){stopFrames();frameVideo=video;pump=createFramePump(video,(_,metadata)=>sample(video,metadata),{active:()=>!destroyed&&!failed&&animated()&&frameVideo===video});}pump.start();}
   else stopFrames();
  }else stopFrames();
  // State watchdog only; actual frame rendering is driven by the video pump.
  if(!document.hidden)idleTimer=setTimeout(update,1000);
 }
 function toggle(){enabled=!enabled;try{localStorage.setItem(preference,enabled?'on':'off');}catch{}update();}
 const sourceVideo=getContext()?.video||document.getElementById('file-player');const videoChanged=()=>{hasFrame=false;update();};const events=['loadeddata','seeked','play','pause','ended'];for(const event of events)sourceVideo?.addEventListener?.(event,videoChanged);
 const onPageHide=()=>{stopCapture();clearTimeout(idleTimer);};window.addEventListener('pagehide',onPageHide);button.addEventListener('click',toggle);live.addEventListener('click',startCapture);document.addEventListener('visibilitychange',update);motion.addEventListener('change',update);update();
 return {update,destroy(){if(destroyed)return;destroyed=true;stopCapture();clearTimeout(idleTimer);for(const event of events)sourceVideo?.removeEventListener?.(event,videoChanged);window.removeEventListener('pagehide',onPageHide);live.removeEventListener('click',startCapture);live.remove();note.remove();button.removeEventListener('click',toggle);document.removeEventListener('visibilitychange',update);motion.removeEventListener('change',update);button.remove();clear();document.body?.classList.remove('room-immersive-on');panel.classList.remove('room-immersive-on');renderer.destroy();layer.remove();}};
}
