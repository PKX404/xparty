package com.pkx404.xparty.nativepreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Native XParty v24 port: native Android screens, WebSocket room state and provider playback. */
public final class MainActivity extends Activity implements RoomConnection.Listener {
  private static final int BG=0xff100c19, PANEL=0xff241a30, CARD=0xff32253f, LIGHT=0xffddd4e8, ACCENT=0xffcdb7ff;
  private static final int PICK_LOCAL=735;
  private final Handler ui=new Handler(Looper.getMainLooper());
  private final Map<String,View> messageViews=new HashMap<>();
  private final LinkedHashSet<String> seen=new LinkedHashSet<>();
  private RoomConnection network;
  private FrameLayout root,stage,roomSurface, browserLayer;
  private LinearLayout roomLayout, chatLines, playerControls, messageComposer, homeSurface;
  private ScrollView chatScroll;
  private EditText compose, joinCode, displayName;
  private TextView roomStatus, headerCode, stageTitle, memberCount, syncBadge, welcome;
  private WebView youtubeBrowser, youtubePlayback;
  private ExoPlayer exo;
  private PlayerView nativePlayer;
  private SeekBar seek;
  private JSONObject currentRoom, selectedSource;
  private String myId="", roomCode="", activeSourceId="", youtubeSelection="", state="home";
  private String replyTo=null;
  private int pingMs=-1;
  private boolean browserOpen=false, pausedLocally=false, draggingSeek=false;
  private final Runnable progressTick=new Runnable(){public void run(){updatePlaybackControls();ui.postDelayed(this,800);}};

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(PANEL);
    getWindow().setNavigationBarColor(BG);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    root=new FrameLayout(this);
    root.setBackgroundColor(BG);
    setContentView(root);
    network=new RoomConnection(this,this,"https://xp404.onrender.com");
    showHome();
    ui.post(progressTick);
  }
  private int dp(float px){return (int)(px*getResources().getDisplayMetrics().density+.5f);}
  private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  private LinearLayout.LayoutParams weight(float w,int height){return new LinearLayout.LayoutParams(0,height,w);}
  private GradientDrawable bg(int fill,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));return d;}
  private GradientDrawable gradient(int a,int b,int radius){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
    d.setCornerRadius(dp(radius));return d;
  }
  private TextView text(String s,int size,boolean strong){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(LIGHT);t.setGravity(Gravity.CENTER_VERTICAL);
    if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    return t;
  }
  private TextView center(String s,int size,boolean strong){
    TextView t=text(s,size,strong);t.setGravity(Gravity.CENTER);return t;
  }
  private Button btn(String label,View.OnClickListener click){
    Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);
    b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(5),0,dp(5),0);b.setBackground(bg(CARD,12));
    b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);click.onClick(v);});
    return b;
  }
  private void spacer(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
  private void margin(LinearLayout l,View child,int h,int mx,int my){
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.setMargins(dp(mx),dp(my),dp(mx),dp(my));l.addView(child,p);
  }
  private EditText input(String hint){
    EditText i=new EditText(this);i.setSingleLine(true);i.setTextSize(14);i.setTextColor(Color.WHITE);
    i.setHintTextColor(0xffab9eb7);i.setHint(hint);i.setPadding(dp(13),0,dp(12),0);
    i.setBackground(bg(CARD,14));return i;
  }
  private void clearSurface(){
    if(browserOpen)closeBrowser();
    root.removeAllViews();
    roomSurface=null;homeSurface=null;
  }
  private void showHome(){
    state="home";clearSurface();
    homeSurface=col();homeSurface.setPadding(dp(18),dp(14),dp(18),dp(16));
    homeSurface.setBackground(gradient(0xff24142e,0xff0a0c17,0));
    root.addView(homeSurface,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout top=row();
    TextView logo=text("XPARTY",26,true);logo.setTextColor(Color.WHITE);
    top.addView(logo,weight(1,dp(58)));
    top.addView(btn("PROFILE",v->profile()),new LinearLayout.LayoutParams(dp(95),dp(43)));
    homeSurface.addView(top);
    spacer(homeSurface,10);
    TextView hero=text("YOUR WORLD.\nYOUR PEOPLE.",30,true);
    hero.setTextColor(Color.WHITE);hero.setLineSpacing(dp(3),1.0f);
    margin(homeSurface,hero,115,2,7);
    TextView summary=text("Watch together • Chat • Live TV • YouTube",13,false);
    summary.setTextColor(0xffc8bcd2);homeSurface.addView(summary);
    spacer(homeSurface,22);
    FrameLayout stagePreview=new FrameLayout(this);
    stagePreview.setBackground(gradient(0xff38214c,0xff0f2030,24));
    TextView deco=center("◉\nEXPLORE TOGETHER",24,true);deco.setTextColor(ACCENT);
    stagePreview.addView(deco,new FrameLayout.LayoutParams(-1,-1));
    margin(homeSurface,stagePreview,170,0,0);
    spacer(homeSurface,15);
    LinearLayout discovery=row();
    discovery.addView(btn("▶  YouTube",v->openYouTube()),weight(1,dp(53)));
    discovery.addView(btn("◉  Live TV",v->openSourcePicker("live")),weight(1,dp(53)));
    discovery.addView(btn("♫  Radio",v->openSourcePicker("radio")),weight(1,dp(53)));
    homeSurface.addView(discovery);
    spacer(homeSurface,20);
    TextView note=text("Native Android preview • compatible with the v24 room protocol",11,false);
    note.setTextColor(0xffa99ab9);homeSurface.addView(note);
    homeSurface.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
    displayName=input("Your display name");
    SharedPreferences prefs=getPreferences(0);
    displayName.setText(prefs.getString("name","Guest"));
    margin(homeSurface,displayName,52,0,4);
    spacer(homeSurface,9);
    Button create=btn("＋  CREATE ROOM",v->startRoom("create"));
    create.setBackground(gradient(0xff6747a0,0xff8252a6,14));
    margin(homeSurface,create,52,0,2);
    spacer(homeSurface,9);
    LinearLayout join=row();
    joinCode=input("7-character room code");joinCode.setSingleLine(true);joinCode.setAllCaps(true);
    join.addView(joinCode,weight(1,dp(52)));
    LinearLayout.LayoutParams jbp=new LinearLayout.LayoutParams(dp(92),dp(52));jbp.leftMargin=dp(8);
    join.addView(btn("JOIN",v->startRoom("join")),jbp);
    homeSurface.addView(join);
    spacer(homeSurface,10);
  }
  private void startRoom(String action){
    String name=displayName.getText().toString().trim();
    if(name.isEmpty())name="Guest";
    if(name.length()>35)name=name.substring(0,35);
    getPreferences(0).edit().putString("name",name).apply();
    if(action.equals("join")){
      String code=joinCode.getText().toString().trim().toUpperCase();
      if(!code.matches("[A-Z2-9]{7}")){toast("Enter the seven-character room code.");return;}
      roomCode=code;network.join(name,code);
    }else network.create(name);
    toast("Connecting to XParty room…");
  }
  private void showRoom(){
    state="room";clearSurface();messageViews.clear();seen.clear();
    roomSurface=new FrameLayout(this);root.addView(roomSurface,new FrameLayout.LayoutParams(-1,-1));
    roomLayout=col();roomLayout.setBackgroundColor(BG);roomSurface.addView(roomLayout,new FrameLayout.LayoutParams(-1,-1));
    FrameLayout header=new FrameLayout(this);header.setBackgroundColor(PANEL);
    LinearLayout left=row();
    Button exit=btn("✕",v->confirmLeave());
    left.addView(exit,new LinearLayout.LayoutParams(dp(42),dp(42)));
    left.addView(btn("⚙",v->roomSettings()),new LinearLayout.LayoutParams(dp(45),dp(42)));
    FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,dp(56),Gravity.START|Gravity.CENTER_VERTICAL);
    hp.leftMargin=dp(8);header.addView(left,hp);
    TextView logo=center("XPARTY",22,true);logo.setTextColor(Color.WHITE);
    FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(138),dp(56),Gravity.CENTER);
    header.addView(logo,lp);
    LinearLayout right=row();
    right.addView(btn("⌕",v->openSourcePicker("youtube")),new LinearLayout.LayoutParams(dp(46),dp(42)));
    memberCount=btn("♙",v->showMembers());
    right.addView(memberCount,new LinearLayout.LayoutParams(dp(48),dp(42)));
    FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-2,dp(56),Gravity.END|Gravity.CENTER_VERTICAL);
    rp.rightMargin=dp(8);header.addView(right,rp);
    roomLayout.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));

    LinearLayout roomCodeRow=row();roomCodeRow.setPadding(dp(12),0,dp(12),0);
    headerCode=text("Room • "+roomCode,12,true);headerCode.setTextColor(ACCENT);
    roomCodeRow.addView(headerCode,weight(1,dp(38)));
    roomStatus=text("Connecting",11,false);roomStatus.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    roomCodeRow.addView(roomStatus,weight(1,dp(38)));
    roomLayout.addView(roomCodeRow,new LinearLayout.LayoutParams(-1,dp(38)));

    stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);
    roomLayout.addView(stage,new LinearLayout.LayoutParams(-1,dp(230)));
    showEmptyStage();
    playerControls=row();playerControls.setGravity(Gravity.CENTER_VERTICAL);
    playerControls.setBackgroundColor(PANEL);playerControls.setPadding(dp(6),0,dp(6),0);
    playerControls.addView(btn("⟲",v->resync()),new LinearLayout.LayoutParams(dp(40),dp(40)));
    playerControls.addView(btn("−10",v->jump(-10000)),new LinearLayout.LayoutParams(dp(39),dp(40)));
    playerControls.addView(btn("⏯",v->togglePlayback()),new LinearLayout.LayoutParams(dp(44),dp(40)));
    seek=new SeekBar(this);seek.setMax(1000);
    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar b,int n,boolean fromUser){}
      public void onStartTrackingTouch(SeekBar b){draggingSeek=true;}
      public void onStopTrackingTouch(SeekBar b){draggingSeek=false;long len=exo==null?0:exo.getDuration();
        if(len>0&&len!=androidx.media3.common.C.TIME_UNSET)seekTo((long)len*b.getProgress()/1000);}
    });
    playerControls.addView(seek,weight(1,dp(42)));
    playerControls.addView(btn("+10",v->jump(10000)),new LinearLayout.LayoutParams(dp(39),dp(40)));
    playerControls.addView(btn("↗",v->toggleFullScreen()),new LinearLayout.LayoutParams(dp(42),dp(40)));
    roomLayout.addView(playerControls,new LinearLayout.LayoutParams(-1,dp(48)));
    playerControls.setVisibility(View.GONE);

    LinearLayout tools=row();tools.setBackgroundColor(PANEL);tools.setPadding(dp(6),0,dp(6),0);
    tools.addView(btn("＋ SOURCE",v->openSourcePicker("youtube")),weight(1,dp(46)));
    tools.addView(btn("♡",v->sendReaction()),weight(1,dp(46)));
    tools.addView(btn("♙ PEOPLE",v->showMembers()),weight(1,dp(46)));
    tools.addView(btn("♩ CALL",v->toast("Native voice/video calls are not connected yet.")),weight(1,dp(46)));
    roomLayout.addView(tools,new LinearLayout.LayoutParams(-1,dp(48)));

    chatScroll=new ScrollView(this);chatScroll.setFillViewport(true);
    chatScroll.setVerticalScrollBarEnabled(false);
    chatLines=col();chatLines.setPadding(dp(13),dp(8),dp(12),dp(10));
    chatScroll.addView(chatLines);
    roomLayout.addView(chatScroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView joined=text("Welcome to your room • chat appears here",12,false);
    joined.setTextColor(0xff91849d);chatLines.addView(joined);

    messageComposer=row();messageComposer.setPadding(dp(5),dp(5),dp(5),dp(5));
    messageComposer.setBackgroundColor(PANEL);
    messageComposer.addView(btn("🎤",v->toast("Native microphone is planned for the next integration.")),new LinearLayout.LayoutParams(dp(42),dp(46)));
    compose=input("Message here");compose.setSingleLine(true);compose.setImeOptions(4);
    messageComposer.addView(compose,weight(1,dp(46)));
    messageComposer.addView(btn("@",v->showMembers()),new LinearLayout.LayoutParams(dp(31),dp(44)));
    messageComposer.addView(btn("☺",v->emojiPicker()),new LinearLayout.LayoutParams(dp(35),dp(44)));
    messageComposer.addView(btn("+",v->openSourcePicker("local")),new LinearLayout.LayoutParams(dp(32),dp(44)));
    messageComposer.addView(btn("↑",v->sendChat()),new LinearLayout.LayoutParams(dp(38),dp(44)));
    roomLayout.addView(messageComposer,new LinearLayout.LayoutParams(-1,dp(56)));
    refreshRoomInfo();
  }
  private void showEmptyStage(){
    stage.removeAllViews();
    TextView choose=center("▶\n\nChoose a video\nYouTube, Live TV or local media",17,true);
    choose.setTextColor(LIGHT);choose.setOnClickListener(v->openSourcePicker("youtube"));
    stage.addView(choose,new FrameLayout.LayoutParams(-1,-1));
    playerControlsSafe(false);
  }
  private void playerControlsSafe(boolean show){if(playerControls!=null)playerControls.setVisibility(show?View.VISIBLE:View.GONE);}
  private void attachStageControls(){
    syncBadge=text("Playing together",11,true);
    syncBadge.setPadding(dp(8),0,dp(8),0);
    syncBadge.setBackground(bg(0x99271d31,16));
    FrameLayout.LayoutParams s=new FrameLayout.LayoutParams(-2,dp(30),Gravity.TOP|Gravity.END);
    s.setMargins(0,dp(12),dp(55),0);stage.addView(syncBadge,s);
    Button full=btn("⛶",v->toggleFullScreen());full.setBackgroundColor(Color.TRANSPARENT);
    FrameLayout.LayoutParams f=new FrameLayout.LayoutParams(dp(42),dp(36),Gravity.TOP|Gravity.END);
    f.setMargins(0,dp(9),dp(7),0);stage.addView(full,f);
  }
  private void openSourcePicker(String tab){
    if(state.equals("home")&&!tab.equals("youtube")){toast("Create or join a room to share sources.");return;}
    String[] labels={"YouTube browser","Search YouTube","Live TV / IPTV URL","Local video file","Direct video URL","Queue (soon)"};
    new AlertDialog.Builder(this).setTitle("XParty • Sources")
      .setItems(labels,(d,n)->{
        if(n==0)openYouTube();
        if(n==1)sourceSearch();
        if(n==2)videoUrl(true);
        if(n==3)chooseLocal();
        if(n==4)videoUrl(false);
        if(n==5)toast("Queue controls will follow room sync integration.");
      }).setNegativeButton("Close",null).show();
  }
  private void sourceSearch(){
    EditText q=input("Search YouTube videos");
    q.setTextSize(16);q.setPadding(dp(12),dp(5),dp(12),dp(5));
    new AlertDialog.Builder(this).setTitle("YouTube search").setView(q)
      .setPositiveButton("Search",(d,n)->network.search(q.getText().toString().trim(),items->{
        if(items.length()==0){toast("No results available; try YouTube browser.");return;}
        final String[] labels=new String[items.length()];
        for(int i=0;i<items.length();i++)labels[i]=items.optJSONObject(i).optString("title","Video");
        new AlertDialog.Builder(this).setTitle("Choose a video").setItems(labels,(dialog,j)->{
          JSONObject item=items.optJSONObject(j);
          if(item!=null)pickYoutube(item.optString("id"),item.optString("title","YouTube video"));
        }).show();
      }));
  }
  private void videoUrl(boolean live){
    EditText entry=input(live?"HTTPS stream (.m3u8 or .mp4)":"HTTPS MP4 / HLS link");
    new AlertDialog.Builder(this).setTitle(live?"Live stream":"Video URL").setView(entry)
      .setPositiveButton("Play",(d,n)->{
        String url=entry.getText().toString().trim();
        if(!url.startsWith("https://")){toast("Use an authorized HTTPS media URL.");return;}
        JSONObject source=new JSONObject();
        try{source.put("type",live?"iptv":"url");source.put("url",url);source.put("title",live?"Live TV":"Video");}catch(Exception ignored){}
        pickSource(source);
      }).setNegativeButton("Cancel",null).show();
  }
  private void chooseLocal(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("video/*");
    try{startActivityForResult(i,PICK_LOCAL);}catch(Exception e){toast("File chooser unavailable.");}
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode==PICK_LOCAL&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
      Uri uri=data.getData();
      try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
      JSONObject source=new JSONObject();
      try{source.put("type","local");source.put("url",uri.toString());source.put("title","Local video (this device only)");}catch(Exception ignored){}
      showMedia(source,false);
      toast("Local file is on this device only; it is not uploaded to other guests.");
    }
  }
  private void pickYoutube(String id,String title){
    if(!id.matches("[A-Za-z0-9_-]{11}")){toast("Unsupported YouTube video link.");return;}
    JSONObject source=new JSONObject();
    try{source.put("type","youtube");source.put("videoId",id);source.put("title",title);}catch(Exception ignored){}
    pickSource(source);
  }
  private void pickSource(JSONObject src){
    if(state.equals("room")&&network.isConnected()){
      network.send("source",src);
      toast("Sharing source with room…");
    }else{
      showMedia(src,false);
      toast("Local preview only • join a room to synchronize.");
    }
  }
  private String parseYouTube(Uri uri){
    if(uri==null||uri.getHost()==null)return null;
    String host=uri.getHost().toLowerCase();
    if(!host.equals("m.youtube.com")&&!host.equals("youtube.com")&&!host.equals("www.youtube.com")&&!host.equals("youtu.be")&&!host.equals("www.youtube-nocookie.com"))return null;
    String id=null;
    if(host.endsWith("youtu.be")&&uri.getPathSegments().size()>0)id=uri.getPathSegments().get(0);
    if("/watch".equals(uri.getPath()))id=uri.getQueryParameter("v");
    if(uri.getPathSegments().size()>1 && ("shorts".equals(uri.getPathSegments().get(0))||"live".equals(uri.getPathSegments().get(0))||"embed".equals(uri.getPathSegments().get(0))))id=uri.getPathSegments().get(1);
    return id!=null&&id.matches("[A-Za-z0-9_-]{11}")?id:null;
  }
  private void openYouTube(){
    if(browserOpen)return;
    browserOpen=true;browserLayer=new FrameLayout(this);browserLayer.setBackgroundColor(BG);
    root.addView(browserLayer,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout layout=col();browserLayer.addView(layout,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout bar=row();bar.setPadding(dp(7),0,dp(7),0);bar.setBackgroundColor(PANEL);
    bar.addView(btn("‹ Room",v->closeBrowser()),new LinearLayout.LayoutParams(dp(90),dp(51)));
    TextView title=center("YouTube",18,true);bar.addView(title,weight(1,dp(51)));
    Button choose=btn("WATCH",v->{if(youtubeSelection.length()==11){
      String id=youtubeSelection;closeBrowser();pickYoutube(id,"YouTube • "+id);
    }else toast("Open a specific video on YouTube first.");});
    choose.setEnabled(false);
    bar.addView(choose,new LinearLayout.LayoutParams(dp(88),dp(49)));
    layout.addView(bar);
    ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    p.setMax(100);layout.addView(p,new LinearLayout.LayoutParams(-1,dp(3)));
    youtubeBrowser=new WebView(this);
    WebSettings cfg=youtubeBrowser.getSettings();cfg.setJavaScriptEnabled(true);
    cfg.setDomStorageEnabled(true);cfg.setMediaPlaybackRequiresUserGesture(true);
    cfg.setAllowFileAccess(false);cfg.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    youtubeBrowser.setWebViewClient(new WebViewClient(){
      void track(String url){
        String id=parseYouTube(Uri.parse(url));if(id!=null){youtubeSelection=id;choose.setEnabled(true);}
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
        Uri u=request.getUrl();if(!request.isForMainFrame())return false;
        String host=u.getHost()==null?"":u.getHost().toLowerCase();
        if("https".equals(u.getScheme()) && (host.endsWith(".youtube.com")||host.equals("youtube.com")||host.equals("youtu.be")||host.equals("accounts.google.com"))){
          track(u.toString());return false;
        }
        toast("External navigation is not supported in this XParty preview.");return true;
      }
      @Override public void doUpdateVisitedHistory(WebView v,String url,boolean reload){track(url);}
      @Override public void onPageFinished(WebView v,String url){track(url);}
    });
    youtubeBrowser.setWebChromeClient(new WebChromeClient(){
      @Override public void onProgressChanged(WebView v,int value){p.setProgress(value);p.setVisibility(value>=100?View.GONE:View.VISIBLE);}
    });
    layout.addView(youtubeBrowser,new LinearLayout.LayoutParams(-1,0,1));
    youtubeBrowser.loadUrl("https://m.youtube.com");
  }
  private void closeBrowser(){
    browserOpen=false;
    if(youtubeBrowser!=null){youtubeBrowser.stopLoading();if(browserLayer!=null)browserLayer.removeView(youtubeBrowser);youtubeBrowser.destroy();youtubeBrowser=null;}
    if(browserLayer!=null){root.removeView(browserLayer);browserLayer=null;}
  }
  private void showMedia(JSONObject source,boolean fromRoom){
    if(source==null){selectedSource=null;activeSourceId="";releasePlayers();if(stage!=null)showEmptyStage();return;}
    String type=source.optString("type");
    if(fromRoom&&source.optString("id","").equals(activeSourceId)&&selectedSource!=null)return;
    selectedSource=source;activeSourceId=source.optString("id","");
    releasePlayers();
    if(stage==null)return;
    stage.removeAllViews();
    if(type.equals("youtube")){
      String id=source.optString("videoId","");
      if(!id.matches("[A-Za-z0-9_-]{11}")){showEmptyStage();return;}
      youtubePlayback=new WebView(this);
      WebSettings p=youtubePlayback.getSettings();p.setJavaScriptEnabled(true);p.setDomStorageEnabled(true);
      p.setMediaPlaybackRequiresUserGesture(false);p.setAllowFileAccess(false);p.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
      youtubePlayback.setBackgroundColor(Color.BLACK);
      youtubePlayback.setWebChromeClient(new WebChromeClient());
      stage.addView(youtubePlayback,new FrameLayout.LayoutParams(-1,-1));
      // Use official YouTube embedded player; do not extract or download streams.
      youtubePlayback.loadUrl("https://www.youtube-nocookie.com/embed/"+id+"?playsinline=1&enablejsapi=1&rel=0");
    }else if(type.equals("iptv")||type.equals("url")||type.equals("local")||type.equals("file")){
      String mediaUrl=source.optString("url","");
      if(type.equals("file") && mediaUrl.length()==0){
        TextView pending=center("Waiting for a shared file from the room host",14,true);
        stage.addView(pending,new FrameLayout.LayoutParams(-1,-1));
      }else if(mediaUrl.startsWith("https://")||mediaUrl.startsWith("content://")){
        exo=new ExoPlayer.Builder(this).build();
        nativePlayer=new PlayerView(this);nativePlayer.setPlayer(exo);nativePlayer.setUseController(false);
        stage.addView(nativePlayer,new FrameLayout.LayoutParams(-1,-1));
        exo.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)));
        exo.prepare();exo.setPlayWhenReady(true);
        exo.addListener(new Player.Listener(){
          @Override public void onPlayerError(androidx.media3.common.PlaybackException error){toast("Video playback failed: "+error.getErrorCodeName());}
        });
      }else{TextView message=center("Media URL not available in native preview.",15,false);stage.addView(message,new FrameLayout.LayoutParams(-1,-1));}
    }else{
      TextView message=center("This provider needs a separate native adapter.",14,false);
      stage.addView(message,new FrameLayout.LayoutParams(-1,-1));
    }
    attachStageControls();playerControlsSafe(true);
    if(stageTitle!=null)stageTitle.setText(source.optString("title","Watching together"));
    if(fromRoom && network.isConnected()&&!activeSourceId.isEmpty())network.sendReady(activeSourceId);
  }
  private void releasePlayers(){
    if(exo!=null){exo.release();exo=null;}
    nativePlayer=null;
    if(youtubePlayback!=null){youtubePlayback.stopLoading();youtubePlayback.destroy();youtubePlayback=null;}
  }
  private void togglePlayback(){
    if(exo!=null){
      if(network.isConnected()&&selectedSource!=null&&currentRoom!=null){
        JSONObject p=new JSONObject();try{p.put("sourceId",activeSourceId);p.put("playing",!exo.isPlaying());p.put("position",exo.getCurrentPosition()/1000.0);p.put("intent",exo.isPlaying()?"pause":"play");}catch(Exception ignored){}
        network.sendJson("playback",p);
      }else{if(exo.isPlaying())exo.pause();else exo.play();}
      return;
    }
    if(youtubePlayback!=null){
      String js="(function(){var f=document.querySelector('iframe');if(f)f.contentWindow.postMessage(JSON.stringify({event:'command',func:'playVideo',args:[]}), '*')})()";
      youtubePlayback.evaluateJavascript(js,null);
    }
  }
  private void jump(long delta){if(exo==null)return;seekTo(Math.max(0,exo.getCurrentPosition()+delta));}
  private void seekTo(long position){
    if(exo==null)return;
    exo.seekTo(position);
    if(network.isConnected()&&!activeSourceId.isEmpty()){
      JSONObject p=new JSONObject();
      try{p.put("sourceId",activeSourceId);p.put("playing",exo.isPlaying());p.put("position",position/1000.0);p.put("intent","seek");}catch(Exception ignored){}
      network.sendJson("playback",p);
    }
  }
  private void resync(){if(currentRoom==null){toast("No room selected.");return;}
    JSONObject playback=currentRoom.optJSONObject("playback");
    if(exo!=null&&playback!=null){
      exo.seekTo((long)(playback.optDouble("position",0)*1000));
      if(playback.optBoolean("playing",false))exo.play();else exo.pause();
      toast("Resynced to room playback.");
    }else toast("Waiting for source playback information.");
  }
  private void updatePlaybackControls(){
    if(exo!=null&&seek!=null&&!draggingSeek){
      long len=exo.getDuration();if(len>0&&len!=androidx.media3.common.C.TIME_UNSET)seek.setProgress((int)Math.min(1000,(exo.getCurrentPosition()*1000)/len));
    }
    if(syncBadge!=null&&selectedSource!=null)syncBadge.setText((exo!=null&&!exo.isPlaying()?"Paused":"Playing")+" together"+(pingMs>=0?" · "+pingMs+" ms":""));
  }
  private void toggleFullScreen(){
    if(stage==null)return;
    boolean full=(getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_FULLSCREEN)!=0;
    getWindow().getDecorView().setSystemUiVisibility(full?0:View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    toast(full?"Normal view":"Fullscreen video");
  }
  private void sendReaction(){network.sendEmoji("❤️");}
  private void sendChat(){
    String content=compose.getText().toString().trim();
    if(content.isEmpty())return;
    if(!network.isConnected()){toast("Room is not connected. Message retained.");return;}
    String id=UUID.randomUUID().toString();
    showChatLine("You",content,true,"local-"+id);
    if(!network.sendChat(content,id,replyTo)){toast("Unable to send. Retry after reconnect.");return;}
    compose.setText("");replyTo=null;
  }
  private void addFromServer(JSONObject msg){
    if(msg==null)return;
    String id=msg.optString("id",UUID.randomUUID().toString());
    if(seen.contains(id))return;
    String cid=msg.optString("clientId");
    if(!cid.isEmpty() && messageViews.containsKey("local-"+cid)){
      View old=messageViews.remove("local-"+cid);
      chatLines.removeView(old);seen.remove("local-"+cid);
    }
    boolean mine=msg.optString("from").equals(myId);
    showChatLine(msg.optString("name",mine?"You":"Guest"),msg.optString("text",""),mine,id);
  }
  private void showChatLine(String author,String body,boolean mine,String id){
    if(chatLines==null||seen.contains(id))return;
    if(seen.size()>180){seen.clear();messageViews.clear();chatLines.removeAllViews();}
    seen.add(id);
    LinearLayout wrap=row();wrap.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);
    wrap.setPadding(0,dp(5),0,dp(5));
    TextView av=center(author.isEmpty()?"?":author.substring(0,1).toUpperCase(),16,true);
    av.setBackground(bg(0xff493d59,28));av.setTextColor(ACCENT);
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(dp(34),dp(34));a.setMargins(dp(4),0,dp(7),0);
    LinearLayout content=col();content.setPadding(dp(3),0,dp(3),0);
    TextView message=text(author+": "+body,14,false);message.setMaxWidth(getResources().getDisplayMetrics().widthPixels-dp(83));
    content.addView(message);
    if(mine){wrap.addView(content);wrap.addView(av,a);}else{wrap.addView(av,a);wrap.addView(content);}
    wrap.setOnLongClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);messageActions(id,body,author);return true;});
    av.setOnLongClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);profileFor(author);return true;});
    chatLines.addView(wrap);messageViews.put(id,wrap);
    chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
  }
  private void messageActions(String id,String body,String author){
    String[] actions={"Reply","Copy message","❤️ React","Delete (if permitted)","Cancel"};
    new AlertDialog.Builder(this).setTitle(author).setItems(actions,(d,n)->{
      if(n==0){replyTo=id;compose.setHint("Reply to "+author);compose.requestFocus();}
      if(n==1){android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        c.setPrimaryClip(android.content.ClipData.newPlainText("XParty message",body));toast("Message copied.");}
      if(n==2)network.react(id,"❤️");
      if(n==3)network.deleteMessage(id);
    }).show();
  }
  private void emojiPicker(){
    String[] emojis={"❤️","😂","🔥","👏","😍","👍","🥳","✨"};
    new AlertDialog.Builder(this).setTitle("Emoji").setItems(emojis,(d,n)->compose.getText().insert(compose.getSelectionStart(),emojis[n])).show();
  }
  private void refreshRoomInfo(){
    if(currentRoom==null)return;
    roomCode=currentRoom.optString("code",roomCode);
    if(headerCode!=null)headerCode.setText("Room • "+roomCode);
    JSONArray people=currentRoom.optJSONArray("people");int count=people==null?0:people.length();
    if(memberCount!=null)memberCount.setText("♙ "+count);
    JSONObject next=currentRoom.optJSONObject("source");
    String id=next==null?"":next.optString("id","");
    if(next==null){if(selectedSource!=null)showMedia(null,true);}
    else if(selectedSource==null||!id.equals(activeSourceId))showMedia(next,true);
  }
  private void showMembers(){
    JSONArray people=currentRoom==null?null:currentRoom.optJSONArray("people");
    if(people==null||people.length()==0){toast("No participant list yet.");return;}
    String[] names=new String[people.length()];
    for(int i=0;i<names.length;i++){
      JSONObject person=people.optJSONObject(i);if(person==null)continue;
      names[i]=person.optString("name","Guest")+(person.optString("id").equals(currentRoom.optString("hostId"))?" ♛":"");
    }
    new AlertDialog.Builder(this).setTitle("Participants • "+names.length).setItems(names,(d,n)->profileFor(names[n])).show();
  }
  private void profileFor(String name){
    String[] items={"Profile","Add friend","Mute","Block","Report"};
    new AlertDialog.Builder(this).setTitle(name).setItems(items,(d,n)->toast("Profile action requires native account integration.")).show();
  }
  private void profile(){new AlertDialog.Builder(this).setTitle("XParty account")
    .setMessage("Guest mode is available. Account login and profile storage need native authentication integration.")
    .setPositiveButton("Continue as guest",(d,w)->{}).show();}
  private void roomSettings(){
    String[] items={"Share room code","Change playback source","Resync playback","Switch backend server","Leave room"};
    new AlertDialog.Builder(this).setTitle("Room settings").setItems(items,(d,n)->{
      if(n==0){android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        c.setPrimaryClip(android.content.ClipData.newPlainText("XParty code",roomCode));toast("Copied room code.");}
      if(n==1)openSourcePicker("youtube");
      if(n==2)resync();
      if(n==3)backendSetting();
      if(n==4)confirmLeave();
    }).show();
  }
  private void backendSetting(){
    EditText text=input("https://xp404.onrender.com");
    text.setText(getPreferences(0).getString("base","https://xp404.onrender.com"));
    new AlertDialog.Builder(this).setTitle("XParty backend URL").setView(text)
      .setPositiveButton("Save",(d,n)->{String b=text.getText().toString().trim();if(b.startsWith("https://")){
        getPreferences(0).edit().putString("base",b).apply();network.setBase(b);
        toast("New server saved. Reconnect to apply.");}})
      .setNegativeButton("Cancel",null).show();
  }
  private void confirmLeave(){
    new AlertDialog.Builder(this).setTitle("Leave XParty room?").setMessage("Exit this watch party and return home?")
      .setPositiveButton("Leave",(d,n)->{network.close();currentRoom=null;selectedSource=null;releasePlayers();showHome();})
      .setNegativeButton("Stay",null).show();
  }
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override public void onEvent(JSONObject e){
    ui.post(()->{
      String t=e.optString("type");
      if(t.equals("welcome")){
        myId=e.optString("id");currentRoom=e.optJSONObject("room");
        if(currentRoom==null){toast("Invalid room response.");return;}
        roomCode=currentRoom.optString("code");
        if(!state.equals("room"))showRoom();
        refreshRoomInfo();
        JSONArray history=e.optJSONArray("history");
        if(history!=null)for(int i=0;i<history.length();i++)addFromServer(history.optJSONObject(i));
        toast("Connected to "+roomCode);
      }else if(t.equals("state")){
        currentRoom=e.optJSONObject("room");refreshRoomInfo();
      }else if(t.equals("chat")){
        addFromServer(e);
      }else if(t.equals("message-update")){
        addFromServer(e.optJSONObject("message"));
      }else if(t.equals("pong")){
        pingMs=(int)Math.max(0,System.currentTimeMillis()-e.optLong("sent",System.currentTimeMillis()));
        if(roomStatus!=null)roomStatus.setText(pingMs+" ms");
      }else if(t.equals("playback")||t.equals("anchor")){
        if(currentRoom!=null){
          JSONObject p=e.optJSONObject("playback");
          if(p!=null){try{currentRoom.put("playback",p);}catch(Exception ignored){}}
          if(p!=null&&exo!=null&&e.optString("sourceId").equals(activeSourceId)){
            long target=(long)(p.optDouble("position",0)*1000);
            if(Math.abs(exo.getCurrentPosition()-target)>2200 && target>=0)exo.seekTo(target);
            if(p.optBoolean("playing",false))exo.play();else exo.pause();
          }
        }
      }else if(t.equals("error")||t.equals("chat-error")){
        toast(e.optString("message","Room request was rejected."));
      }else if(t.equals("ended")||t.equals("kicked")||t.equals("resume-failed")){
        toast(e.optString("message","Room is no longer available."));
        currentRoom=null;network.close();showHome();
      }
    });
  }
  @Override public void onStatus(String text){ui.post(()->{
    if(roomStatus!=null)roomStatus.setText(text);
  });}
  @Override public void onError(String message){ui.post(()->toast(message));}
  @Override public void onBackPressed(){
    if(browserOpen){if(youtubeBrowser!=null&&youtubeBrowser.canGoBack())youtubeBrowser.goBack();else closeBrowser();return;}
    if(state.equals("room")){confirmLeave();return;}
    super.onBackPressed();
  }
  @Override protected void onDestroy(){
    ui.removeCallbacksAndMessages(null);
    if(network!=null)network.close();
    releasePlayers();if(youtubeBrowser!=null)youtubeBrowser.destroy();
    super.onDestroy();
  }
}