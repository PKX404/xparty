import {icon} from './icons.js';
export function mountPremium({screen}){
 const $=id=>document.getElementById(id),body=document.body;body.classList.add('premium-app');
 const skin=document.createElement('link');skin.rel='stylesheet';skin.href='premium.css';document.head.append(skin);
 const makeButton=(id,text,action)=>{const b=document.createElement('button');b.type='button';b.id=id;b.textContent=text;b.onclick=action;return b;};
 const welcome=document.createElement('section');welcome.id='welcome-screen';welcome.innerHTML='<div class="welcome-orbit" aria-hidden="true"><i></i><i></i><span>▶</span></div><div class="eyebrow">YOUR PEOPLE. YOUR MOMENT.</div><h1>Closer.<br>Even from <em>here.</em></h1><p>A movie night, a familiar voice.<br>Watch, talk and feel together.</p><div class="welcome-actions"></div><p class="welcome-foot">Made4Love <span>♥</span><small>INSPIRED BY AANCHAL</small></p>';
 $('lobby').before(welcome);let entered=false;try{entered=sessionStorage.getItem('xparty-entered')==='yes';}catch{}
 function enter(){entered=true;try{sessionStorage.setItem('xparty-entered','yes');}catch{}state();}
 const openAccount=mode=>{window.dispatchEvent(new CustomEvent('xparty:auth-mode',{detail:mode}));$('account-dialog').showModal();};
 welcome.querySelector('.welcome-actions').append(makeButton('welcome-signup','Create account',()=>openAccount('signup')),makeButton('welcome-signin','Sign in',()=>openAccount('signin')),makeButton('welcome-guest','Continue as guest',enter));
 $('account-status').after($('account-guest'));$('account-guest').addEventListener('click',enter);window.addEventListener('xparty:account-session',e=>{if(e.detail.signedIn)enter();});
 const intro=document.querySelector('.intro');intro.querySelector('h1').innerHTML='Different places.<br><em>Same moment.</em>';intro.querySelector('h1+p').textContent='Choose something you love. Bring someone you miss.';
 const homeAccount=document.createElement('div');homeAccount.className='home-account-row';homeAccount.append(makeButton('home-signin','Sign in',()=>openAccount('signin')),makeButton('home-signup','Create account',()=>openAccount('signup')));intro.querySelector('.home-actions').before(homeAccount);
 const art=document.createElement('div');art.className='home-art';art.setAttribute('aria-hidden','true');art.innerHTML='<span>▶</span><i>together</i>';intro.querySelector('h1').before(art);
 document.querySelector('.entry-card .muted').textContent='Start a private room, or join with a code.';document.querySelector('.entry-card h2').textContent='Make tonight a plan.';
 // Move existing controls, preserving their handlers and host permissions.
 const menu=document.createElement('dialog');menu.id='app-menu';menu.className='premium-sheet';menu.innerHTML='<div class="sheet-heading"><h2>Your Xparty</h2><button type="button" aria-label="Close menu">×</button></div><div class="app-menu-grid"></div>';
 menu.querySelector('button').onclick=()=>menu.close();body.append(menu);
 const menuButton=makeButton('app-menu-button','',()=>menu.showModal());icon(menuButton,'more','App menu');document.querySelector('.topbar').append(menuButton);
 for(const [id,label,shape]of[['nav-home','Home','home'],['open-profile','Profile','person'],['nav-friends','Friends','people'],['nav-messages','Messages','message'],['nav-settings','Settings','settings'],['nav-about','About','info'],['new-room','New room','plus'],['return-room','Return to room','room']]){
  const b=$(id);if(!b)continue;icon(b,shape,label);b.replaceChildren();const s=document.createElement('span');s.textContent=label;b.append(s);menu.querySelector('.app-menu-grid').append(b);b.addEventListener('click',()=>menu.close(),true);
 }
 const mix=document.createElement('dialog');mix.id='sound-mix';mix.className='premium-sheet';mix.innerHTML='<div class="sheet-heading"><h2>Find your balance</h2><button type="button" aria-label="Close sound settings">×</button></div><p class="small">Your call. Your movie. Your volume.</p>';mix.append(document.querySelector('.mixer'));mix.querySelector('button').onclick=()=>mix.close();body.append(mix);
 const sound=makeButton('sound-mix-button','',()=>mix.showModal());icon(sound,'volume','Volume and sound');$('mobile-tools').append(sound);
 const roomIntro=document.createElement('div');roomIntro.className='room-intro';roomIntro.innerHTML='<span class="eyebrow">TOGETHER, RIGHT HERE</span><h2>Stay for the moment.</h2>';$('mobile-tools').before(roomIntro);
 const extra=document.createElement('details');extra.id='playback-more';extra.innerHTML='<summary aria-label="More playback controls"></summary><div class="playback-extra"></div>';icon(extra.querySelector('summary'),'more','More playback controls');
 const bar=document.querySelector('.player-bar');for(const b of [...bar.querySelectorAll(':scope > button')])if(!['play','resync','browse','theater'].includes(b.id)){const label=b.getAttribute('aria-label')||b.textContent;b.dataset.label=label;extra.lastChild.append(b);}bar.append(extra);
 const callMore=document.createElement('details');callMore.id='call-more';callMore.innerHTML='<summary aria-label="More call controls"></summary><div></div>';icon(callMore.querySelector('summary'),'more','More call controls');
 const seat=document.querySelector('.call-seat-row');for(const b of [...seat.querySelectorAll(':scope > button')])if(!['call-seat','speaker'].includes(b.id))callMore.lastChild.append(b);seat.append(callMore);
 for(const d of[extra,callMore])d.addEventListener('click',e=>{if(e.target.closest('button'))d.open=false;});
 const heading=document.createElement('div');heading.className='browse-heading';heading.innerHTML='<span class="eyebrow">FIND YOUR NEXT MOMENT</span><h2>What are we watching?</h2>';$('source-panel').prepend(heading);$('youtube-input').placeholder='Search YouTube or paste a link';
 $('empty-player').querySelector('h2').textContent='Set the scene.';$('empty-player').querySelector('p').textContent='One favourite video. One shared moment.';$('empty-player').append(makeButton('pick-first-video','Find a video',()=>screen('browse')));
 function state(){const inRoom=!$('room').hidden;welcome.hidden=entered||inRoom;body.classList.toggle('show-welcome',!welcome.hidden);body.classList.toggle('premium-in-room',inRoom);}
 new MutationObserver(state).observe($('room'),{attributes:true,attributeFilter:['hidden']});state();
}
