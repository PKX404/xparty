import {createImmersiveRenderer} from './immersive.js';
﻿export function colorsFromPixels(data,width,height){
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
export function initAmbient({getContext,panel=document.querySelector('.chat-panel')}={}){
 if(!panel||typeof getContext!=='function')return {update(){},destroy(){}};
 const preference='xparty.ambient-chat';let enabled=true;try{enabled=localStorage.getItem(preference)!=='off';}catch{}
 const motion=matchMedia('(prefers-reduced-motion: reduce)'),canvas=document.createElement('canvas');canvas.width=12;canvas.height=8;const ctx=canvas.getContext('2d',{willReadFrequently:true});
 const layer=document.createElement('div');layer.className='ambient-chat-layer';layer.setAttribute('aria-hidden','true');panel.prepend(layer);
 const immersive=createImmersiveRenderer(layer);
 const button=document.createElement('button');button.type='button';button.className='ambient-chat-toggle secondary';button.textContent='Immersive';button.title='Immersive video background';(panel.querySelector('.panel-title')||panel).append(button);
 let timer=null,key=null,image=null,failed=false,destroyed=false,lastPalette=null,thumbnailReady=false,lastSampleAt=-Infinity;
 const media=navigator.mediaDevices,origin=location.origin;let handle=null,capture=null,captureVideo=null,captureTrack=null,captureGeneration=0,capturePending=false;
 const live=document.createElement('button');live.type='button';live.className='ambient-chat-toggle secondary';live.textContent='Live colors';live.title='Use colors from this Xparty tab';
 const note=document.createElement('p');note.className='ambient-chat-note';note.setAttribute('role','status');note.hidden=true;panel.append(note);
 try{if(media?.getDisplayMedia&&media?.setCaptureHandleConfig&&typeof MediaStreamTrack!=='undefined'&&MediaStreamTrack.prototype.getCaptureHandle){handle=crypto.randomUUID();media.setCaptureHandleConfig({handle,exposeOrigin:true,permittedOrigins:[origin]});(panel.querySelector('.panel-title')||panel).append(live);}}catch{handle=null;}
 function status(text){note.textContent=text;note.hidden=!text;}
 function stopCapture(message=''){captureGeneration++;capturePending=false;const old=capture;capture=null;captureTrack=null;if(captureVideo){captureVideo.pause();captureVideo.srcObject=null;captureVideo=null;}stopAmbientStream(old);live.disabled=false;live.textContent='Live colors';live.setAttribute('aria-pressed','false');lastPalette=null;failed=false;canvas.width=12;if(message)status(message);}
 function verifyCapture(){if(captureTrack&&!validAmbientCapture(captureTrack,handle,origin)){stopCapture('Choose this Xparty tab.');return false;}return !!captureTrack;}
 async function startCapture(){
  if(capture){stopCapture('Using thumbnail colors.');update();return;}
  const source=getContext()?.room?.source;if(!handle||!enabled||source?.type!=='youtube'||capturePending)return;
  status('Choose this Xparty tab. Colors stay on this device.');capturePending=true;live.disabled=true;const generation=++captureGeneration;
  let stream;
  try{stream=await media.getDisplayMedia({video:{frameRate:{ideal:4,max:4},displaySurface:'browser'},audio:false,preferCurrentTab:true,selfBrowserSurface:'include',surfaceSwitching:'exclude',monitorTypeSurfaces:'exclude',systemAudio:'exclude'});
   if(destroyed||generation!==captureGeneration||!enabled||getContext()?.room?.source?.type!=='youtube'){stopAmbientStream(stream);return;}
   const track=stream.getVideoTracks()[0];if(!validAmbientCapture(track,handle,origin)){stopAmbientStream(stream);status('Choose this Xparty tab.');return;}
   capture=stream;captureTrack=track;captureVideo=document.createElement('video');captureVideo.muted=true;captureVideo.playsInline=true;captureVideo.srcObject=stream;
   track.addEventListener('capturehandlechange',()=>{verifyCapture();update();});track.addEventListener('ended',()=>{if(capture===stream){stopCapture('Using thumbnail colors.');update();}},{once:true});
   await captureVideo.play();if(generation!==captureGeneration)return;failed=false;canvas.width=12;lastPalette=null;live.textContent='Stop live colors';live.setAttribute('aria-pressed','true');status('Live colors from this Xparty tab. Colors stay on this device.');update();
  }catch{stopAmbientStream(stream);if(generation===captureGeneration){stopCapture();status('Live colors unavailable. Using thumbnail colors.');}}
  finally{if(generation===captureGeneration){capturePending=false;live.disabled=false;}}
 }
 live.addEventListener('click',startCapture);
 function paint(palette){lastPalette=palette;palette.forEach((rgb,i)=>panel.style.setProperty('--ambient-'+i,`rgb(${rgb.join(' ')})`));}
 function sample(media,crop=null){if(!ctx||Date.now()-lastSampleAt<250)return;lastSampleAt=Date.now();try{immersive.render(media,crop);if(crop)ctx.drawImage(media,crop.x,crop.y,crop.width,crop.height,0,0,12,8);else ctx.drawImage(media,0,0,12,8);paint(colorsFromPixels(ctx.getImageData(0,0,12,8).data,12,8));}catch{failed=true;}}
 function clear(){immersive.clear();key=null;image=null;failed=false;thumbnailReady=false;lastPalette=null;panel.classList.remove('ambient-chat-on','ambient-chat-moving');for(let i=0;i<3;i++)panel.style.removeProperty('--ambient-'+i);}
 function update(){
  clearTimeout(timer);timer=null;if(destroyed)return;const c=getContext()||{},r=c.room,source=r?.source;
  button.setAttribute('aria-pressed',String(enabled));
  if(!enabled||!source){stopCapture();status('');clear();return;}
  live.hidden=source.type!=='youtube';if(source.type!=='youtube'&&(capture||capturePending)){stopCapture();status('');}
  const nextKey=source.id||source.videoId||source.name;
  if(key!==nextKey){clear();key=nextKey;canvas.width=12; // Reset any previous cross-origin canvas taint.
   if(source.type==='youtube'&&source.videoId){image=new Image();image.crossOrigin='anonymous';const expected=key;image.onload=()=>{if(destroyed||key!==expected)return;thumbnailReady=true;update();};image.onerror=()=>{if(key===expected){if(!capture)failed=true;image=null;}};image.src='https://i.ytimg.com/vi/'+encodeURIComponent(source.videoId)+'/mqdefault.jpg';}
  }
  const visible=panel.getClientRects().length>0,playing=c.playing??(r.playback?.playing&&!r.playback?.waitingFor?.length),active=ambientActive({enabled,hidden:document.hidden,reduced:motion.matches,room:r,playing,visible});
  panel.classList.toggle('ambient-chat-on',enabled&&visible);panel.classList.toggle('ambient-chat-moving',active);
  if(!document.hidden&&visible&&playing&&!failed){if(source.type==='file'){const video=c.video||document.getElementById('file-player');if(video&&!video.paused&&!video.ended&&video.readyState>=2&&(active||!lastPalette))sample(video);}else if(capture&&verifyCapture()&&captureVideo?.readyState>=2&&(active||!lastPalette)){const stage=document.getElementById('watch-stage'),crop=captureCrop(stage?.getBoundingClientRect(),innerWidth,innerHeight,captureVideo.videoWidth,captureVideo.videoHeight);if(crop)sample(captureVideo,crop);}else if(thumbnailReady&&!lastPalette)sample(image);}
  // Dormant checks only read playback state. Frame sampling is capped at four per second.
  if(!document.hidden)timer=setTimeout(update,active?250:1000);
 }
 function toggle(){enabled=!enabled;try{localStorage.setItem(preference,enabled?'on':'off');}catch{}update();}
 const onPageHide=()=>{stopCapture();clearTimeout(timer);timer=null;};window.addEventListener('pagehide',onPageHide);const visibility=()=>update();button.addEventListener('click',toggle);document.addEventListener('visibilitychange',visibility);motion.addEventListener('change',visibility);update();
 return {update,destroy(){if(destroyed)return;destroyed=true;stopCapture();clearTimeout(timer);image=null;window.removeEventListener('pagehide',onPageHide);live.removeEventListener('click',startCapture);live.remove();note.remove();button.removeEventListener('click',toggle);document.removeEventListener('visibilitychange',visibility);motion.removeEventListener('change',visibility);button.remove();clear();immersive.destroy();layer.remove();}};
}
