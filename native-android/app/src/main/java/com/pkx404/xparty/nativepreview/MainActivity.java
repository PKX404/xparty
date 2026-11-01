package com.pkx404.xparty.nativepreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.net.Uri;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.*;
import android.text.TextUtils;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(17,11,25), PANEL = Color.rgb(37,24,48), WHITE = Color.WHITE;
    private FrameLayout root, playerFrame;
    private LinearLayout room;
    private ScrollView messages;
    private LinearLayout messageList;
    private EditText messageInput;
    private FrameLayout browserOverlay;
    private WebView youtubeBrowser, youtubePlayer;
    private TextView status;
    private String currentYoutubeId;
    private String playingId;

    private int dp(float p) { return (int)(getResources().getDisplayMetrics().density*p+0.5f); }
    private TextView label(String s, int sp) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(WHITE);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private Button button(String s, View.OnClickListener action) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(s); b.setTextSize(12);
        b.setOnClickListener(action); return b;
    }
    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private LinearLayout.LayoutParams w(int weight, int h) {
        return new LinearLayout.LayoutParams(0,h,weight);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(PANEL);
        getWindow().setNavigationBarColor(BG);
        root = new FrameLayout(this); root.setBackgroundColor(BG);
        setContentView(root); buildRoom();
    }
    private void buildRoom() {
        room=col(); root.addView(room, new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=row(); header.setBackgroundColor(PANEL); header.setPadding(dp(12),0,dp(12),0);
        TextView logo=label("XPARTY",23); logo.setGravity(Gravity.CENTER); logo.setTypeface(null,1);
        header.addView(button("Home",v->showInfo()),new LinearLayout.LayoutParams(dp(76),dp(50)));
        header.addView(logo,w(1,dp(56)));
        header.addView(button("Sources",v->chooseSource()),new LinearLayout.LayoutParams(dp(100),dp(50)));
        room.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));

        playerFrame=new FrameLayout(this); playerFrame.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams playerLp=new LinearLayout.LayoutParams(-1,dp(230));
        room.addView(playerFrame,playerLp);
        showNoMedia();

        LinearLayout tabs=row(); tabs.setBackgroundColor(PANEL);
        tabs.addView(button("Video call",v->Toast.makeText(this,"Calling is not connected in this preview.",Toast.LENGTH_SHORT).show()),w(1,dp(48)));
        tabs.addView(button("Chat",v->{}),w(1,dp(48)));
        tabs.addView(button("YouTube",v->openYouTube()),w(1,dp(48)));
        room.addView(tabs,new LinearLayout.LayoutParams(-1,dp(48)));

        TextView notice=label("Android native preview · local chat only · no live room sync",11);
        notice.setTextColor(0xffc3b5cf); notice.setPadding(dp(12),dp(7),0,dp(7));
        room.addView(notice);

        messages=new ScrollView(this); messages.setFillViewport(true);
        messageList=col(); messageList.setPadding(dp(14),dp(8),dp(14),dp(8));
        messages.addView(messageList);
        room.addView(messages,new LinearLayout.LayoutParams(-1,0,1));
        addMessage("XParty","This is a standalone native Android preview, not the live Test 17 watch party.",false);
        addMessage("XParty","Choose YouTube to browse inside XParty, then select a supported video to watch.",false);

        LinearLayout compose=row(); compose.setBackgroundColor(PANEL);compose.setPadding(dp(8),dp(4),dp(8),dp(4));
        compose.addView(label(" @ ",18),new LinearLayout.LayoutParams(dp(36),dp(50)));
        messageInput=new EditText(this);messageInput.setSingleLine(true);
        messageInput.setTextColor(WHITE);messageInput.setHintTextColor(0xffaaa0b3);
        messageInput.setHint("Message here");messageInput.setTextSize(14);
        compose.addView(messageInput,w(1,dp(50)));
        compose.addView(button("Send",v->sendMessage()),new LinearLayout.LayoutParams(dp(70),dp(50)));
        room.addView(compose,new LinearLayout.LayoutParams(-1,dp(60)));
    }
    private void showInfo() {
        new AlertDialog.Builder(this).setTitle("XParty native preview")
        .setMessage("This installable prototype uses Android native layouts. Multiplayer chat, calls, profiles and backend synchronization are NOT connected. Your previous XParty app is not changed.")
        .setPositiveButton("OK",null).show();
    }
    private void addMessage(String sender,String body,boolean mine) {
        LinearLayout line=row();line.setPadding(0,dp(6),0,dp(6));
        line.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);
        TextView txt=label(sender+": "+body,14);
        txt.setMaxWidth(getResources().getDisplayMetrics().widthPixels-dp(90));
        line.addView(txt);messageList.addView(line);
        messages.post(()->messages.fullScroll(View.FOCUS_DOWN));
    }
    private void sendMessage() {
        String s=messageInput.getText().toString().trim();
        if(s.isEmpty())return;
        addMessage("You",s,true);messageInput.setText("");
    }
    private void showNoMedia() {
        playerFrame.removeAllViews();
        TextView t=label("No video selected\n\nChoose Sources to start",15);
        t.setGravity(Gravity.CENTER); t.setTextColor(0xffccc2d7);
        playerFrame.addView(t,new FrameLayout.LayoutParams(-1,-1));
        status=null;
    }
    private void chooseSource() {
        String[] items={"YouTube — browse inside XParty","Authorized HTTPS video URL","Clear player"};
        new AlertDialog.Builder(this).setTitle("Select source").setItems(items,(d,n)->{
            if(n==0)openYouTube();
            if(n==1)enterVideoUrl();
            if(n==2){playingId=null;showNoMedia();}
        }).show();
    }
    private void enterVideoUrl() {
        EditText input=new EditText(this);input.setHint("https://example.com/video.mp4");
        new AlertDialog.Builder(this).setTitle("HTTPS media URL")
            .setView(input).setPositiveButton("Play",(d,n)->{
                String u=input.getText().toString().trim();
                if(u.startsWith("https://"))playDirect(u);
                else Toast.makeText(this,"Only HTTPS media sources are supported.",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel",null).show();
    }
    private void playDirect(String uri) {
        playingId=null;playerFrame.removeAllViews();
        VideoView v=new VideoView(this);
        playerFrame.addView(v,new FrameLayout.LayoutParams(-1,-1));
        v.setVideoURI(Uri.parse(uri));
        v.setOnPreparedListener(mp->{mp.setLooping(false);v.start();});
        v.setOnErrorListener((mp,what,extra)->{
            Toast.makeText(this,"This source cannot play on this device.",Toast.LENGTH_LONG).show();return true;
        });
        MediaController controls=new MediaController(this);controls.setAnchorView(v);v.setMediaController(controls);
        v.requestFocus();
    }
    private static String youtubeId(Uri uri){
        if(uri==null || uri.getHost()==null) return null;
        String host=uri.getHost().toLowerCase();
        if(!(host.equals("youtube.com")||host.equals("www.youtube.com")||host.equals("m.youtube.com")||host.equals("youtu.be")))return null;
        String id=null;
        if(host.equals("youtu.be") && uri.getPathSegments().size()>0)id=uri.getPathSegments().get(0);
        else if("/watch".equals(uri.getPath()))id=uri.getQueryParameter("v");
        else if(uri.getPathSegments().size()>1 && ("shorts".equals(uri.getPathSegments().get(0))||"live".equals(uri.getPathSegments().get(0))))id=uri.getPathSegments().get(1);
        if(id!=null && id.matches("[A-Za-z0-9_-]{11}"))return id;
        return null;
    }
    private static boolean permittedYoutube(Uri u){
        if(u==null || u.getHost()==null)return false;
        String host=u.getHost().toLowerCase();
        return "https".equalsIgnoreCase(u.getScheme())&&
            (host.equals("youtube.com")||host.equals("m.youtube.com")||host.equals("www.youtube.com")||host.equals("youtu.be")||host.equals("accounts.google.com"));
    }
    private void openYouTube(){
        if(browserOverlay!=null)return;
        currentYoutubeId=null;
        browserOverlay=new FrameLayout(this);browserOverlay.setBackgroundColor(BG);
        root.addView(browserOverlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout browserRoot=col();
        browserOverlay.addView(browserRoot,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout toolbar=row();toolbar.setBackgroundColor(PANEL);
        toolbar.addView(button("← Room",v->closeBrowser()),new LinearLayout.LayoutParams(dp(94),dp(55)));
        TextView text=label("YouTube",18);toolbar.addView(text,w(1,dp(55)));
        Button watch=button("Watch in XParty",v->{
            if(currentYoutubeId!=null){String id=currentYoutubeId;closeBrowser();playYoutube(id);}
        });
        watch.setEnabled(false);
        toolbar.addView(watch,new LinearLayout.LayoutParams(dp(145),dp(55)));
        browserRoot.addView(toolbar);
        youtubeBrowser=new WebView(this);
        WebSettings cfg=youtubeBrowser.getSettings(); cfg.setJavaScriptEnabled(true);cfg.setDomStorageEnabled(true);
        cfg.setMediaPlaybackRequiresUserGesture(true);cfg.setAllowFileAccess(false);
        cfg.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        youtubeBrowser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();
                if(!request.isForMainFrame())return false;
                if(!permittedYoutube(u)){
                    if("https".equalsIgnoreCase(u.getScheme()))try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
                    return true;
                }
                String id=youtubeId(u);
                if(id!=null){currentYoutubeId=id;watch.setEnabled(true);}
                return false;
            }
            @Override public void onPageFinished(WebView v,String url){
                String id=youtubeId(Uri.parse(url));
                if(id!=null){currentYoutubeId=id;watch.setEnabled(true);}
            }
            @Override public void doUpdateVisitedHistory(WebView view,String url,boolean reload){
                String id=youtubeId(Uri.parse(url));if(id!=null){currentYoutubeId=id;watch.setEnabled(true);}
            }
        });
        browserRoot.addView(youtubeBrowser,new LinearLayout.LayoutParams(-1,0,1));
        youtubeBrowser.loadUrl("https://m.youtube.com");
    }
    private void closeBrowser(){
        if(youtubeBrowser!=null){
            youtubeBrowser.stopLoading();youtubeBrowser.loadUrl("about:blank");
            browserOverlay.removeViewAt(0);youtubeBrowser.destroy();youtubeBrowser=null;
        }
        if(browserOverlay!=null){root.removeView(browserOverlay);browserOverlay=null;}
    }
    private void playYoutube(String videoId){
        if(!videoId.matches("[A-Za-z0-9_-]{11}"))return;
        playingId=videoId;playerFrame.removeAllViews();
        youtubePlayer=new WebView(this);
        WebSettings settings=youtubePlayer.getSettings();
        settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        youtubePlayer.setBackgroundColor(Color.BLACK);
        playerFrame.addView(youtubePlayer,new FrameLayout.LayoutParams(-1,-1));
        // Official YouTube embed; avoids extracting protected streams.
        String url="https://www.youtube-nocookie.com/embed/"+videoId+"?playsinline=1&rel=0";
        youtubePlayer.loadUrl(url);
    }
    @Override public void onBackPressed(){
        if(browserOverlay!=null){
            if(youtubeBrowser!=null && youtubeBrowser.canGoBack())youtubeBrowser.goBack();else closeBrowser();
            return;
        }
        super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(youtubeBrowser!=null){youtubeBrowser.destroy();youtubeBrowser=null;}
        if(youtubePlayer!=null){youtubePlayer.destroy();youtubePlayer=null;}
        super.onDestroy();
    }
}
