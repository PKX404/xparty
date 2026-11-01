package com.pkx404.xparty.nativepreview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Native adapter for XParty v24's WebSocket /ws and REST /api/search. */
public final class RoomConnection {
  public interface Listener {void onEvent(JSONObject e);void onStatus(String s);void onError(String s);}
  public interface SearchResults {void receive(JSONArray results);}
  private final Listener listener;
  private final SharedPreferences preferences;
  private final Handler main=new Handler(Looper.getMainLooper());
  private final OkHttpClient client=new OkHttpClient.Builder().pingInterval(20,TimeUnit.SECONDS)
      .connectTimeout(30,TimeUnit.SECONDS).readTimeout(0,TimeUnit.SECONDS).build();
  private WebSocket ws;
  private String base,token="",name="Guest",code="";
  private JSONObject pendingAction;
  private int generation=0,attempts=0;
  private boolean intentional=true,connected=false;
  RoomConnection(Context context,Listener listener,String initialBase){
    this.listener=listener;
    this.preferences=context.getSharedPreferences("native-v24-session",Context.MODE_PRIVATE);
    this.base=preferences.getString("base",initialBase);
  }
  public boolean isConnected(){return connected&&ws!=null;}
  public void setBase(String next){if(!next.startsWith("https://"))return;
    base=next.replaceAll("/+$","");
    preferences.edit().putString("base",base).apply();
  }
  public void create(String user){name=user;code="";pendingAction=json("create");try{pendingAction.put("name",name);pendingAction.put("capacity",JSONObject.NULL);}catch(Exception ignored){}connect();}
  public void join(String user,String roomCode){name=user;code=roomCode;
    pendingAction=json("join");try{pendingAction.put("name",name);pendingAction.put("code",code);}catch(Exception ignored){}connect();}
  private JSONObject json(String type){JSONObject o=new JSONObject();try{o.put("type",type);}catch(Exception ignored){}return o;}
  private void connect(){
    intentional=false;connected=false;generation++;
    int gen=generation;
    if(ws!=null){ws.close(1000,"Reconnect");ws=null;}
    listener.onStatus("Connecting…");
    Request request=new Request.Builder().url(base.replaceFirst("^https://","wss://")+"/ws").build();
    ws=client.newWebSocket(request,new WebSocketListener(){
      @Override public void onOpen(WebSocket socket,Response response){
        if(gen!=generation){socket.close(1000,"Stale");return;}
        attempts=0;listener.onStatus("Joining…");
        JSONObject request=pendingAction;
        if(request==null){request=json("resume");try{request.put("token",token);}catch(Exception ignored){}}
        socket.send(request.toString());JSONObject ping=json("ping");
        try{ping.put("sent",System.currentTimeMillis());}catch(Exception ignored){}
        socket.send(ping.toString());
      }
      @Override public void onMessage(WebSocket socket,String string){
        if(gen!=generation)return;
        try{
          JSONObject m=new JSONObject(string);
          if(m.optString("type").equals("welcome")){
            connected=true;attempts=0;token=m.optString("token",token);
            JSONObject room=m.optJSONObject("room");if(room!=null)code=room.optString("code",code);
            preferences.edit().putString("token",token).putString("code",code).apply();
            listener.onStatus("Connected");
            pendingAction=null;
          }
          listener.onEvent(m);
        }catch(Exception e){listener.onError("Invalid room update.");}
      }
      @Override public void onFailure(WebSocket s,Throwable t,Response response){
        if(gen!=generation)return;connected=false;listener.onStatus("Offline");
        if(!intentional){listener.onError("Room connection unavailable.");scheduleReconnect(gen);}
      }
      @Override public void onClosed(WebSocket s,int code,String reason){
        if(gen!=generation)return;connected=false;listener.onStatus("Disconnected");
        if(!intentional)scheduleReconnect(gen);
      }
    });
  }
  private void scheduleReconnect(int gen){
    if(intentional||gen!=generation)return;
    int delay=Math.min(16000,1000*(1<<Math.min(4,attempts++)));
    main.postDelayed(()->{
      if(intentional||gen!=generation)return;
      if(token.length()>0 && pendingAction==null){
        pendingAction=json("resume");
        try{pendingAction.put("token",token);}catch(Exception ignored){}
      }
      connect();
    },delay);
  }
  public void close(){intentional=true;connected=false;generation++;pendingAction=null;
    main.removeCallbacksAndMessages(null);
    if(ws!=null){ws.close(1000,"Leave room");ws=null;}
  }
  public boolean sendJson(String type,JSONObject payload){
    if(!isConnected())return false;
    JSONObject o=json(type);
    try{
      java.util.Iterator<String> iter=payload.keys();
      while(iter.hasNext()){String key=iter.next();o.put(key,payload.opt(key));}
      if(type.equals("source")||type.equals("playback"))o.put("eventId",UUID.randomUUID().toString());
    }catch(Exception ignored){}
    return ws.send(o.toString());
  }
  public boolean send(String type,JSONObject source){
    JSONObject p=new JSONObject();try{p.put("source",source);}catch(Exception ignored){}
    return sendJson(type,p);
  }
  public boolean sendChat(String text,String clientId,String replyTo){
    JSONObject p=new JSONObject();
    try{p.put("clientId",clientId);p.put("text",text);if(replyTo!=null)p.put("replyTo",replyTo);}catch(Exception ignored){}
    return sendJson("chat",p);
  }
  public void sendReady(String sourceId){
    JSONObject p=new JSONObject();try{p.put("sourceId",sourceId);p.put("ready",true);p.put("status","Android player opened");}catch(Exception ignored){}
    sendJson("ready",p);
  }
  public void sendEmoji(String emoji){JSONObject p=new JSONObject();try{p.put("emoji",emoji);}catch(Exception ignored){}sendJson("reaction",p);}
  public void react(String id,String emoji){JSONObject p=new JSONObject();try{p.put("id",id);p.put("emoji",emoji);p.put("active",true);}catch(Exception ignored){}sendJson("chat-react",p);}
  public void deleteMessage(String id){JSONObject p=new JSONObject();try{p.put("id",id);}catch(Exception ignored){}sendJson("message-delete",p);}
  public void search(String q,SearchResults done){
    if(q.length()<2){listener.onError("Enter at least two characters.");return;}
    Request request=new Request.Builder().url(base+"/api/search?q="+android.net.Uri.encode(q))
      .header("Authorization","Bearer "+token).build();
    client.newCall(request).enqueue(new Callback(){
      @Override public void onFailure(Call call,IOException e){listener.onError("Search unavailable.");}
      @Override public void onResponse(Call call,Response response) throws IOException{
        try(response){String body=response.body()==null?"{}":response.body().string();
          JSONObject parsed=new JSONObject(body);
          JSONArray items=parsed.optJSONArray("items");
          if(items==null)items=new JSONArray();
          JSONArray finalItems=items;main.post(()->done.receive(finalItems));
        }catch(Exception error){listener.onError("Unable to load video results.");}
      }
    });
  }
}