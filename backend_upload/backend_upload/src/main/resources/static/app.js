/* Shared shell: sidebar, top-right controls, icons. Used by index.html and modules.html. */
const IC={
home:'<path d="M3 11l9-8 9 8"/><path d="M5 10v10h5v-6h4v6h5V10"/>',
bolt:'<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
bars:'<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
bell:'<path d="M6 8a6 6 0 0112 0c0 7 3 8 3 8H3s3-1 3-8"/><path d="M10 20a2 2 0 004 0"/>',
gear:'<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.9l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.9-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.9.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.9 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.9l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.9.3h0a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5h0a1.7 1.7 0 001.9-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.9v0a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z"/>',
user:'<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/>',
cal:'<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
full:'<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>',
chart:'<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
bulb:'<path d="M9 18h6M10 21h4"/><path d="M12 3a6 6 0 00-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0012 3z"/>',
factory:'<path d="M3 21V10l6 4v-4l6 4V6h3v15z"/><path d="M7 21v-3M12 21v-3M17 21v-3"/>',
fan:'<circle cx="12" cy="12" r="2"/><path d="M12 10c0-4 1-7 4-7 2 0 3 2 2 4s-3 3-6 3zM14 12c4 0 7 1 7 4 0 2-2 3-4 2s-3-3-3-6zM12 14c0 4-1 7-4 7-2 0-3-2-2-4s3-3 6-3zM10 12c-4 0-7-1-7-4 0-2 2-3 4-2s3 3 3 6z"/>',
plug:'<path d="M9 2v6M15 2v6M6 8h12v4a6 6 0 01-12 0zM12 18v4"/>',
build:'<rect x="5" y="3" width="14" height="18" rx="1"/><path d="M9 7h2M13 7h2M9 11h2M13 11h2M9 15h2M13 15h2"/>',
house:'<path d="M3 11l9-7 9 7v10H3z"/><path d="M9 21v-7h6v7"/>',
rupee:'<path d="M6 4h12M6 9h12M9 4c6 0 6 9 0 9H6l9 8"/>',
leaf:'<path d="M20 4C9 4 4 10 4 16c0 2 .5 3 1 4 1-4 4-8 9-10-4 3-6 6-7 10 8 1 13-6 13-16z"/>',
co2:'<path d="M7 17a4 4 0 010-8 5 5 0 019.5 1A3.5 3.5 0 0117 17z"/>',
plus:'<path d="M12 5v14M5 12h14"/>',
arrow:'<path d="M5 12h14M13 6l6 6-6 6"/>',
bulbtip:'<path d="M9 18h6M10 21h4"/><path d="M12 3a6 6 0 00-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0012 3z"/>'
};
const svgi=(n,cls)=>`<svg viewBox="0 0 24 24"${cls?` class="${cls}"`:''}>${IC[n]||''}</svg>`;

const LOGO=`<img src="/img/logo.jpg" alt="Datamint Aarvix" class="brand-logo">`;
const BOLT=`<img src="/img/logo2.jpg" alt="Smart Energy" class="logo-bolt">`;
const CITY=`<svg viewBox="0 0 220 100" preserveAspectRatio="none"><rect y="78" width="220" height="22" fill="#2f8a3e"/><g fill="#7fb4e8"><rect x="22" y="40" width="16" height="40"/><rect x="42" y="30" width="18" height="50"/><rect x="64" y="46" width="14" height="34"/><rect x="150" y="36" width="18" height="44"/><rect x="172" y="48" width="16" height="32"/></g><g fill="#cfe6ff" opacity=".7"><rect x="26" y="46" width="4" height="4"/><rect x="46" y="38" width="4" height="4"/><rect x="52" y="48" width="4" height="4"/><rect x="154" y="44" width="4" height="4"/><rect x="176" y="56" width="4" height="4"/></g><g stroke="#fff" stroke-width="2" stroke-linecap="round"><path d="M104 80V44M104 44l-12-6M104 44l10-10M104 44l2 14"/><path d="M200 82V54M200 54l-9-4M200 54l8-8M200 54l1 11"/></g><g fill="#2fa04a"><circle cx="12" cy="76" r="9"/><circle cx="84" cy="78" r="8"/><circle cx="130" cy="77" r="9"/><circle cx="212" cy="77" r="8"/></g></svg>`;
const PLANT=`<svg class="plant" viewBox="0 0 120 150"><ellipse cx="60" cy="140" rx="46" ry="8" fill="#14301c"/><path d="M60 140V80" stroke="#3fbf5a" stroke-width="4" stroke-linecap="round"/><path d="M60 100C30 100 18 78 20 56c26 0 42 14 40 44z" fill="#34c25a"/><path d="M60 82C60 52 78 32 106 28c4 28-14 52-46 54z" fill="#52e07a"/><path d="M60 100C48 90 34 76 24 62" stroke="#1d7a38" stroke-width="2" fill="none"/></svg>`;

const NAV=[['dashboard','Dashboard','home','index.html'],['monitor','Energy Monitor','bolt','modules.html#monitor'],['reports','Reports','bars','modules.html#reports'],
['alerts','Alerts','bell','modules.html#alerts'],['optimization','Optimization','bolt','modules.html#optimization'],['settings','Settings','gear','modules.html#settings'],['profile','Profile','user','modules.html#profile']];

const Shell={
  token:localStorage.getItem('token'),
  headers(){return {Authorization:'Bearer '+this.token,'Content-Type':'application/json'}},
  guard(){if(!this.token)location.href='login.html'},
  init(active){
    this.guard();
    document.getElementById('side').innerHTML=
      `<div class="brand">${LOGO}<div><b>DATAMINT AARVIX</b><small>Innovate · Analyze · Transform</small></div></div>`+
      `<nav>`+NAV.map(n=>`<a data-k="${n[0]}" href="${n[3]}" class="${n[0]==active?'on':''}">${svgi(n[2])}${n[1]}${n[0]=='alerts'?'<b id="nb"></b>':''}</a>`).join('')+`</nav>`+
      `<div class="user" id="usr"><i>${svgi('user')}</i><div>${localStorage.getItem('fullName')||'User'}<small>${localStorage.getItem('role')||''}</small></div></div>`+
      `<div class="green"><div class="t"><svg viewBox="0 0 24 24" fill="#3cf08a">${IC.leaf.replace('<path','<path fill="#3cf08a" stroke="none"')}</svg><div><b>Go Green<br>Save Energy</b><small>Build a Sustainable Future</small></div></div>${CITY}</div>`;
    document.getElementById('usr').onclick=()=>location.href='modules.html#profile';
    const rt=document.getElementById('rt');
    if(rt){rt.innerHTML=`<span class="live">Live</span><span class="vr"></span><div class="clock">${svgi('cal')}<span id="clk"></span></div>`+
      `<a class="ibtn" href="modules.html#alerts" aria-label="Alerts">${svgi('bell')}<b id="bb"></b></a><button class="ibtn" id="fs" aria-label="Full screen">${svgi('full')}</button>`;
      const tick=()=>{const d=new Date();document.getElementById('clk').textContent=d.toLocaleDateString('en-US',{weekday:'short',month:'short',day:'2-digit',year:'numeric'})+'  '+d.toLocaleTimeString('en-US',{hour:'2-digit',minute:'2-digit'})};
      tick();setInterval(tick,30000);
      document.getElementById('fs').onclick=()=>{document.fullscreenElement?document.exitFullscreen():document.documentElement.requestFullscreen()}}
  },
  setAlerts(n){['nb','bb'].forEach(i=>{const e=document.getElementById(i);if(e)e.textContent=n>0?n:''})},
  setActive(k){document.querySelectorAll('nav a').forEach(a=>a.classList.toggle('on',a.dataset.k==k))}
};
