const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const assets=path.join(__dirname,'../app/src/main/assets/client');
const bridge=fs.readFileSync(path.join(assets,'native-auth.js'),'utf8');
test('native storage RPC and cold callback buffering; ignores child-frame ports',async()=>{
 let message;const values=new Map();
 const window={addEventListener:(name,fn)=>message=fn};
 const context={window,navigator:{userAgent:'XpartyAndroid/2.1.1'},setTimeout,clearTimeout,Map,Promise,JSON,Error};
 vm.runInNewContext(bridge,context);
 let compromised=false;message({data:'xparty-auth-port',source:{},ports:[{start(){compromised=true;}}]});assert.equal(compromised,false);
 const port={start(){},postMessage(raw){const r=JSON.parse(raw);if(r.op==='set')values.set(r.key,r.value);if(r.op==='remove')values.delete(r.key);queueMicrotask(()=>port.onmessage({data:JSON.stringify({id:r.id,value:values.get(r.key)??null})}));}};
 message({data:'xparty-auth-port',source:null,ports:[port]});
 await window.XpartyNativeAuth.storage.setItem('key','value');assert.equal(await window.XpartyNativeAuth.storage.getItem('key'),'value');
 await window.XpartyNativeAuth.storage.removeItem('key');assert.equal(await window.XpartyNativeAuth.storage.getItem('key'),null);
 port.onmessage({data:JSON.stringify({event:'callback',url:'xparty://login-callback?code=one'})});
 let callback;window.XpartyNativeAuth.onCallback(v=>callback=v);assert.equal(callback,'xparty://login-callback?code=one');
});
test('normal browser gets no native bridge',()=>{const window={};vm.runInNewContext(bridge,{window,navigator:{userAgent:'Chrome'}});assert.equal(window.XpartyNativeAuth,undefined);});
const code=fs.readFileSync(path.join(assets,'accounts.js'),'utf8');
const flows=code.slice(code.indexOf('let googleBusy=false;'),code.indexOf('async function googleFetch'));
function setup(){
 const values=new Map(),calls=[],elements={'privacy-consent':{checked:true},'google-login':{disabled:false}};
 const native={storage:{getItem:async k=>values.get(k),setItem:async(k,v)=>values.set(k,v),removeItem:async k=>values.delete(k)},open:async url=>calls.push(['open',url])};
 const context={window:{XpartyNativeAuth:native},navigator:{onLine:true},URL,Date,Error,Number,String,location:{origin:'https://pkxparty.onrender.com',pathname:'/'},authReady:true,session:null,$:id=>elements[id],status:()=>{},refreshProfile:async()=>{},sessionStorage:{setItem(){}},check:r=>{if(r.error)throw r.error;return r.data;},client:{auth:{signInWithOAuth:async opts=>{calls.push(['oauth',opts]);return {data:{url:'https://vgnzdvfrzervpirpirze.supabase.co/auth/v1/authorize'}};},exchangeCodeForSession:async v=>{calls.push(['exchange',v]);return {data:{session:{user:{id:'existing-user'}}}};}}}};
 vm.createContext(context);vm.runInContext(flows,context);return {context,values,calls,elements};
}
test('Google starts PKCE browser flow and preserves existing user identity on return',async()=>{
 const {context:c,values,calls}=setup();await c.google();assert.equal(calls[0][1].options.redirectTo,'xparty://login-callback');assert.equal(calls[0][1].options.skipBrowserRedirect,true);assert.ok(values.get('xparty-oauth-pending'));
 await c.completeGoogle('xparty://login-callback?code=test-code');assert.equal(c.session.user.id,'existing-user');assert.equal(values.has('xparty-oauth-pending'),false);
 await assert.rejects(c.completeGoogle('xparty://login-callback?code=test-code'),/expired/);assert.equal(calls.filter(v=>v[0]==='exchange').length,1);
});
test('denied, stale and unsolicited callbacks never exchange tokens',async()=>{
 const {context:c,values,calls}=setup();await assert.rejects(c.completeGoogle('xparty://login-callback?code=bad'),/expired/);
 values.set('xparty-oauth-pending',String(Date.now()-16*60*1000));await assert.rejects(c.completeGoogle('xparty://login-callback?code=bad'),/expired/);
 values.set('xparty-oauth-pending',String(Date.now()));await assert.rejects(c.completeGoogle('xparty://login-callback?error=access_denied'),/cancelled/);assert.equal(calls.length,0);
});
test('privacy consent and offline state block OAuth, errors restore the button',async()=>{
 const {context:c,elements}=setup();elements['privacy-consent'].checked=false;await assert.rejects(c.google(),/privacy/);
 elements['privacy-consent'].checked=true;c.navigator.onLine=false;await assert.rejects(c.google(),/internet/);
 c.navigator.onLine=true;c.client.auth.signInWithOAuth=async()=>({error:Error('Network failure')});await assert.rejects(c.google(),/Network/);assert.equal(elements['google-login'].disabled,false);
});
