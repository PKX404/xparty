// Independent browser implementation of a separable Gaussian video background.
// Android SurfaceTexture inputs are replaced with an ordinary browser canvas.
export function createImmersiveRenderer(host){
 const canvas=document.createElement('canvas');canvas.className='immersive-frame';canvas.hidden=true;canvas.width=256;canvas.height=144;canvas.setAttribute('aria-hidden','true');
 let gl;try{gl=canvas.getContext('webgl',{alpha:false,depth:false,antialias:false,preserveDrawingBuffer:false});if(!gl?.createShader)return {render(){},clear(){},destroy(){}};}catch{return {render(){},clear(){},destroy(){}};}
 const shaders=[],textures=[],buffers=[],programs=[],framebuffers=[];
 let lost=false,ready=false;
 const input=document.createElement('canvas');input.width=256;input.height=144;const ctx=input.getContext('2d');
 function release(){for(const x of textures)gl.deleteTexture(x);for(const x of buffers)gl.deleteBuffer(x);for(const x of shaders)gl.deleteShader(x);for(const x of programs)gl.deleteProgram(x);for(const x of framebuffers)gl.deleteFramebuffer(x);}
 try{
  const compile=(type,source)=>{const s=gl.createShader(type);shaders.push(s);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error('Shader unavailable');return s;};
  const p=gl.createProgram();programs.push(p);
  gl.attachShader(p,compile(gl.VERTEX_SHADER,'attribute vec2 position;varying vec2 uv;void main(){uv=position*.5+.5;gl_Position=vec4(position,0.,1.);}'));
  gl.attachShader(p,compile(gl.FRAGMENT_SHADER,`precision mediump float;
varying vec2 uv;uniform sampler2D frame;uniform vec2 direction;uniform float grade;
void main(){vec3 color=vec3(0.);float total=0.;
 for(int i=-12;i<=12;i++){float x=float(i);float weight=exp(-.5*x*x/64.);color+=texture2D(frame,clamp(uv+direction*x,vec2(0.),vec2(1.))).rgb*weight;total+=weight;}
 color/=total;
 if(grade>.5){float luma=dot(color,vec3(.213,.715,.072));color=mix(vec3(luma),color,1.5);float light=dot(color,vec3(.213,.715,.072));color*=min(1.,.75/max(light,.0001));}
 gl_FragColor=vec4(color,1.);}`));
  gl.linkProgram(p);if(!gl.getProgramParameter(p,gl.LINK_STATUS))throw Error('Shader unavailable');gl.useProgram(p);
  const b=gl.createBuffer();buffers.push(b);gl.bindBuffer(gl.ARRAY_BUFFER,b);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,1,1]),gl.STATIC_DRAW);
  const position=gl.getAttribLocation(p,'position');gl.enableVertexAttribArray(position);gl.vertexAttribPointer(position,2,gl.FLOAT,false,0,0);
  const texture=()=>{const t=gl.createTexture();textures.push(t);gl.bindTexture(gl.TEXTURE_2D,t);for(const axis of [gl.TEXTURE_WRAP_S,gl.TEXTURE_WRAP_T])gl.texParameteri(gl.TEXTURE_2D,axis,gl.CLAMP_TO_EDGE);for(const filter of [gl.TEXTURE_MIN_FILTER,gl.TEXTURE_MAG_FILTER])gl.texParameteri(gl.TEXTURE_2D,filter,gl.LINEAR);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,256,144,0,gl.RGBA,gl.UNSIGNED_BYTE,null);return t;};
  const source=texture(),intermediate=texture(),fbo=gl.createFramebuffer();framebuffers.push(fbo);gl.bindFramebuffer(gl.FRAMEBUFFER,fbo);gl.framebufferTexture2D(gl.FRAMEBUFFER,gl.COLOR_ATTACHMENT0,gl.TEXTURE_2D,intermediate,0);if(gl.checkFramebufferStatus(gl.FRAMEBUFFER)!==gl.FRAMEBUFFER_COMPLETE)throw Error('Framebuffer unavailable');
  const direction=gl.getUniformLocation(p,'direction'),grade=gl.getUniformLocation(p,'grade');gl.uniform1i(gl.getUniformLocation(p,'frame'),0);gl.viewport(0,0,256,144);
  canvas.addEventListener('webglcontextlost',()=>{lost=true;canvas.hidden=true;});
  host.append(canvas);
  return {
   render(media,crop){if(lost||!ctx)return;try{
    ctx.clearRect(0,0,256,144);if(crop)ctx.drawImage(media,crop.x,crop.y,crop.width,crop.height,0,0,256,144);else ctx.drawImage(media,0,0,256,144);
    gl.bindTexture(gl.TEXTURE_2D,source);gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL,true);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,input);
    gl.bindFramebuffer(gl.FRAMEBUFFER,fbo);gl.uniform2f(direction,1/256,0);gl.uniform1f(grade,0);gl.drawArrays(gl.TRIANGLE_STRIP,0,4);
    gl.bindFramebuffer(gl.FRAMEBUFFER,null);gl.bindTexture(gl.TEXTURE_2D,intermediate);gl.uniform2f(direction,0,1/144);gl.uniform1f(grade,1);gl.drawArrays(gl.TRIANGLE_STRIP,0,4);canvas.hidden=false;ready=true;
   }catch{canvas.hidden=true;if(ready){input.width=256;ready=false;}}},
   clear(){canvas.hidden=true;input.width=256;ready=false;},
   destroy(){lost=true;release();canvas.remove();}
  };
 }catch{release();return {render(){},clear(){},destroy(){}};}
}
