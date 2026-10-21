import {youtubeId} from './sync.js';
export const isEmbedded=source=>['youtube','vimeo'].includes(source?.type);
export const isVideoElement=source=>['file','direct'].includes(source?.type);
export function validProviderId(type,id){return typeof id==='string'&&(type==='youtube'?/^[\w-]{11}$/.test(id):type==='vimeo'?/^[1-9][0-9]{5,11}$/.test(id):false);}
export function publicVideoURL(value){
 try{const u=new URL(value);if(u.protocol!=='https:'||u.username||u.password||u.port||u.hash||!u.hostname.includes('.')||/(^|\.)(localhost|local|internal|test|invalid)$/.test(u.hostname)||/^\d+(\.\d+){3}$/.test(u.hostname)||u.hostname.includes(':')||! /\.(mp4|webm)$/i.test(u.pathname)||value.length>2048)return null;return u.href;}catch{return null;}
}
export function parseProviderLink(input){
 const id=youtubeId(input);if(id)return {type:'youtube',videoId:id,title:'YouTube video'};
 try{const u=new URL(input);if(u.protocol!=='https:'||u.username||u.password)return null;
 if(['vimeo.com','www.vimeo.com','player.vimeo.com'].includes(u.hostname)){const id=u.pathname.match(/^\/(?:video\/)?([1-9][0-9]{5,11})\/?$/)?.[1];if(id)return {type:'vimeo',videoId:id,title:'Vimeo video'};}
 const url=publicVideoURL(input);if(url)return {type:'direct',url,title:decodeURIComponent(u.pathname.split('/').pop()).slice(0,150)};
 }catch{}return null;
}
let sdk;
export async function createVimeoPlayer(host,id,onState,onError){
 const mount=document.createElement('div');host.append(mount);
 if(!sdk)sdk=new Promise((resolve,reject)=>{if(window.Vimeo?.Player)return resolve(window.Vimeo);const s=document.createElement('script');s.src='https://player.vimeo.com/api/player.js';const timeout=setTimeout(()=>reject(Error('Vimeo timed out')),20000);s.onload=()=>{clearTimeout(timeout);window.Vimeo?.Player?resolve(window.Vimeo):reject(Error('Vimeo unavailable'));};s.onerror=()=>{clearTimeout(timeout);reject(Error('Vimeo unavailable'));};document.head.append(s);}).catch(e=>{sdk=null;throw e;});
 const Vimeo=await sdk;const player=new Vimeo.Player(mount,{id:Number(id),autoplay:false,responsive:true,playsinline:true});let time=0,duration=0,state=2,disposed=false;
 const run=p=>Promise.resolve(p).catch(e=>{if(!disposed)onError(e);});
 player.on('timeupdate',e=>{time=e.seconds;duration=e.duration;});player.on('durationchange',e=>{duration=e.duration;});
 for(const [event,value]of [['play',1],['pause',2],['ended',0],['bufferstart',3]])player.on(event,()=>{if(!disposed){state=value;onState({data:value});}});
 player.on('bufferend',()=>run(player.getPaused().then(paused=>{if(!disposed){state=paused?2:1;onState({data:state});}})));
 player.on('error',onError);
 let readyTimeout;try{await Promise.race([player.ready(),new Promise((_,reject)=>{readyTimeout=setTimeout(()=>reject(Error('Vimeo video timed out')),20000);})]);duration=await player.getDuration();}catch(e){disposed=true;run(player.destroy());mount.remove();throw e;}finally{clearTimeout(readyTimeout);}
 return {getCurrentTime:()=>time,getDuration:()=>duration,getPlayerState:()=>state,
 playVideo:()=>run(player.play()),pauseVideo:()=>run(player.pause()),stopVideo:()=>run(player.pause()),seekTo:t=>{time=t;run(player.setCurrentTime(t));},setVolume:v=>run(player.setVolume(Math.max(0,Math.min(1,v/100)))),unMute:()=>run(player.setMuted(false)),
 destroy(){disposed=true;run(player.destroy());mount.remove();}};
}
