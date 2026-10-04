// Native-only, origin-scoped channel. The YouTube iframe receives no bridge.
(()=>{
 if(!/XpartyAndroid\/2\.1\.1/.test(navigator.userAgent))return;
 let port,sequence=0,resolveReady,callback,queued;
 const pending=new Map(),ready=new Promise(resolve=>resolveReady=resolve);
 function request(op,fields={}){return ready.then(()=>new Promise((resolve,reject)=>{
  const id=++sequence,timer=setTimeout(()=>{pending.delete(id);reject(Error('Secure sign-in timed out. Please try again.'));},15000);
  pending.set(id,{resolve,reject,timer});port.postMessage(JSON.stringify({id,op,...fields}));
 }));}
 window.addEventListener('message',event=>{
  if(event.data!=='xparty-auth-port'||!event.ports?.[0])return;
  // Native postWebMessage delivers this port to this top-level document only.
  if(event.source&&event.source!==window)return;
  port=event.ports[0];port.onmessage=e=>{
   let data;try{data=JSON.parse(e.data);}catch{return;}
   if(data.event==='callback'){if(callback)callback(data.url);else queued=data.url;return;}
   const task=pending.get(data.id);if(!task)return;clearTimeout(task.timer);pending.delete(data.id);data.error?task.reject(Error(data.error)):task.resolve(data.value);
  };
  port.start();resolveReady();request('ready').catch(()=>{});
 });
 window.XpartyNativeAuth={
  storage:{getItem:key=>request('get',{key}),setItem:(key,value)=>request('set',{key,value}),removeItem:key=>request('remove',{key})},
  open:url=>request('open',{url}),
  onCallback(fn){callback=fn;if(queued){const value=queued;queued=null;fn(value);}}
 };
})();
