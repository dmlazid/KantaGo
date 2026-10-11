'use strict';
const $=id=>document.getElementById(id),host=typeof Native!=='undefined',fragment=new URLSearchParams(location.hash.slice(1));
let token=fragment.get('token')||sessionStorage.getItem('kantago-token')||'',songs=[],state={},language='All',limit=60,revision=-1,activePage=host?'home':'remoteControl',lastBg='',lastVideo='',tagId='',lastNotice='',offline=false,visibleSongs=[],seeking=false;
if(token){sessionStorage.setItem('kantago-token',token);history.replaceState(null,'',location.pathname);}if(!host)document.body.classList.add('remote');
function notify(text){if(!text)return;$('banner').textContent=text;$('banner').classList.remove('hidden');clearTimeout(notify.timer);notify.timer=setTimeout(()=>$('banner').classList.add('hidden'),5500);}
async function api(path,body){const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),5000);try{const r=await fetch('/api/'+path,{method:body?'POST':'GET',headers:{'Content-Type':'application/json','Authorization':'Bearer '+token},body:body?JSON.stringify(body):undefined,signal:controller.signal});let d=await r.json();if(r.status===403){token='';sessionStorage.removeItem('kantago-token');showPair();}if(!r.ok)throw Error(d.error||'Connection failed');return d;}finally{clearTimeout(timer);}}
async function command(action,data={}){try{state=await api('command',{action,...data});renderState();}catch(e){notify(e.message);}}
function page(p){if(!host&&(p==='home'||p==='player'))p='remoteControl';if(!host&&p==='settings')p='remoteSettings';activePage=p;document.querySelectorAll('.page').forEach(e=>e.classList.toggle('hidden',e.id!==p));document.querySelectorAll('nav button').forEach(e=>e.classList.toggle('active',e.dataset.page===p));if(p==='songs')renderSongs();window.scrollTo(0,0);}
window.goHome=()=>page(host?'home':'remoteControl');document.querySelectorAll('[data-page]').forEach(b=>b.onclick=()=>page(b.dataset.page));$('start').onclick=()=>page('player');$('connect').onclick=()=>$('connectDialog').showModal();$('connectSubmit').onclick=()=>{if(host){Native.connect($('hostAddress').value);$('connectDialog').close();}};
$('pairTop').onclick=$('pairSettings').onclick=()=>host&&Native.pairing();function showPair(){if(!$('pairDialog').open)$('pairDialog').showModal();}
$('pairSubmit').onclick=async()=>{try{const r=await fetch('/api/pair',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({pin:$('pin').value})}),d=await r.json();if(!r.ok)throw Error(d.error);token=d.token;sessionStorage.setItem('kantago-token',token);$('pairDialog').close();$('pairError').textContent='';revision=-1;}catch(e){$('pairError').textContent=e.message;}};
$('pairDialog').addEventListener('cancel',e=>e.preventDefault());
function importDialog(){if(host)$('importDialog').showModal();else notify('Import songs on the main videoke device.');}['importHome','importSongs','importSettings'].forEach(id=>$(id).onclick=importDialog);
function pick(folder){let year=$('importYear').value;if(year&&(Number(year)<1900||Number(year)>2100)){notify('Enter a year between 1900 and 2100.');return;}Native.importFiles($('importLang').value,year,folder);$('importDialog').close();}
$('pickFiles').onclick=()=>pick(false);$('pickFolder').onclick=()=>pick(true);$('background').onclick=()=>host&&Native.background();
$('lyricSize').value=localStorage.getItem('lyricSize')||34;function size(){document.documentElement.style.setProperty('--lyric',$('lyricSize').value+'px');localStorage.setItem('lyricSize',$('lyricSize').value);}size();$('lyricSize').oninput=size;
$('sortSongs').value=localStorage.getItem('kantago-sort')||'number';
$('sortSongs').onchange=()=>{localStorage.setItem('kantago-sort',$('sortSongs').value);limit=60;renderSongs();};
$('fileType').onchange=()=>{limit=60;renderSongs();};
$('search').oninput=()=>{limit=60;renderSongs();};$('era').onchange=$('favorites').onchange=()=>{limit=60;renderSongs();};document.querySelectorAll('[data-lang]').forEach(b=>b.onclick=()=>{language=b.dataset.lang;limit=60;document.querySelectorAll('[data-lang]').forEach(x=>x.classList.toggle('selected',x===b));renderSongs();});document.querySelectorAll('[data-era]').forEach(b=>b.onclick=()=>{$('era').value=b.dataset.era;page('songs');});$('more').onclick=()=>{limit+=60;renderSongs();};
function el(tag,cls,text){let e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=text;return e;}
function btn(text,fn,label){let e=el('button','',text);e.onclick=fn;if(label)e.setAttribute('aria-label',label);return e;}
function filteredSongs(){
 const term=$('search').value.toLocaleLowerCase().trim(),era=$('era').value,type=$('fileType').value;
 const result=songs.filter(s=>{
  const y=Number(s.year)||0,f=(s.format||'').toLowerCase();
  if(language!=='All'&&s.language!==language)return false;
  if($('favorites').checked&&!s.favorite)return false;
  if(![s.id,s.title,s.artist].join(' ').toLocaleLowerCase().includes(term))return false;
  if(type==='midi'&&!['mid','kar'].includes(f))return false;
  if(type==='audio'&&!['mp3','wav','ogg','m4a'].includes(f))return false;
  if(type==='video'&&f!=='mp4')return false;
  if(era==='new'&&y!==new Date().getFullYear())return false;
  if(era==='classic'&&!(y>0&&y<2010))return false;
  if(era==='older'&&!(y>0&&y<1970))return false;
  if(!['all','new','classic','older'].includes(era)&&!(y>=Number(era)&&y<Number(era)+10))return false;
  return true;
 });
 const byText=(a,b)=>(a||'').localeCompare(b||'',undefined,{sensitivity:'base'});
 const sort=$('sortSongs').value;
 result.sort((a,b)=>sort==='title'?byText(a.title,b.title)||byText(a.id,b.id):sort==='artist'?byText(a.artist,b.artist)||byText(a.title,b.title):sort==='newest'?(Number(b.year)||0)-(Number(a.year)||0)||byText(a.title,b.title):sort==='oldest'?(Number(a.year)||9999)-(Number(b.year)||9999)||byText(a.title,b.title):Number(a.id)-Number(b.id));
 return result;
}
function renderStats(){
 const owned=songs.length,counts={OPM:0,Bisaya:0,English:0,Other:0};
 songs.forEach(s=>{if(s.language in counts)counts[s.language]++;});
 $('libraryStats').textContent=owned+' songs available · '+counts.OPM+' OPM · '+counts.Bisaya+' Bisaya · '+counts.English+' English · '+counts.Other+' Foreign / Other';
}
function renderSongs(){
 visibleSongs=filteredSongs();
 $('count').textContent=visibleSongs.length+' of '+songs.length+' songs';
 const list=$('songList');list.replaceChildren();
 if(!visibleSongs.length)list.append(el('div','empty',songs.length<=1?'Only the original demo is available. Import more songs on the main KantaGo device.':'No matching songs. Try clearing your filters.'));
 visibleSongs.slice(0,limit).forEach(s=>{
  const row=el('div','song'),info=el('div','songInfo');
  info.append(el('div','songNumber',s.id+(s.format?' · '+s.format.toUpperCase():'')),el('div','songTitle',s.title),el('div','songMeta',s.artist+' · '+s.language+(s.year?' · '+s.year:'')));
  if(!host){
   row.classList.add('remoteSong');
   info.tabIndex=0;info.setAttribute('role','button');info.setAttribute('aria-label','Select song '+s.id+' '+s.title);
   const select=()=>{setRemoteDigits(s.id);page('remoteControl');notify('Selected '+s.title);};
   info.onclick=select;info.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();select();}};
   row.append(info,btn('＋',()=>command('reserve',{id:s.id}),'Reserve '+s.title),btn('▶',()=>{command('play',{id:s.id});page('remoteControl');},'Play '+s.title));
  }else{
   row.append(info,btn(s.favorite?'★':'☆',()=>command('favorite',{id:s.id}),'Toggle favorite'),
   btn('＋',()=>command('reserve',{id:s.id}).then(()=>notify('Reserved '+s.title)),'Reserve song'),
   btn('▶',()=>{command('play',{id:s.id});page('player');},'Play song'),
   btn('⋯',()=>{tagId=s.id;$('tagLang').value=s.language;$('tagYear').value=s.year||'';$('tagDialog').showModal();},'Edit tags'));
  }
  list.append(row);
 });
 $('more').classList.toggle('hidden',visibleSongs.length<=limit);
}
$('randomSong').onclick=()=>{const pool=filteredSongs();if(!pool.length){notify('No songs match your filters.');return;}const song=pool[Math.floor(Math.random()*pool.length)];command('play',{id:song.id});page('player');notify('Now playing: '+song.title);};
$('clearFilters').onclick=()=>{$('search').value='';$('era').value='all';$('favorites').checked=false;$('fileType').value='all';language='All';document.querySelectorAll('[data-lang]').forEach(b=>b.classList.toggle('selected',b.dataset.lang==='All'));limit=60;renderSongs();};
$('saveTag').onclick=async()=>{await command('tag',{id:tagId,language:$('tagLang').value,year:Number($('tagYear').value)||0});$('tagDialog').close();};
for(const k of ['1','2','3','4','5','6','7','8','9','CLR','0','⌫'])$('keypad').append(btn(k,()=>{let n=$('number');if(k==='CLR')n.value='';else if(k==='⌫')n.value=n.value.slice(0,-1);else n.value=(n.value+k).slice(0,6);}));$('number').oninput=()=>$('number').value=$('number').value.replace(/\D/g,'').slice(0,6);
function numberAction(action){let n=$('number').value;if(!n){notify('Enter a song number first.');return;}command(action,{id:n.padStart(6,'0')});}$('reserve').onclick=()=>numberAction('reserve');$('playNow').onclick=()=>numberAction('play');document.querySelectorAll('[data-action]').forEach(b=>b.onclick=()=>command(b.dataset.action));document.querySelectorAll('[data-adjust]').forEach(b=>b.onclick=()=>{let [k,v]=b.dataset.adjust.split(':');command(k,{value:(Number(state[k])||0)+Number(v)});});$('volume').onchange=()=>command('volume',{value:Number($('volume').value)/100});$('shuffleQueue').onclick=()=>command('shuffleQueue');
$('clearQueue').onclick=()=>{if((state.queue||[]).length&&confirm('Clear all reserved songs?'))command('clearQueue');};
$('lyricMinus').onclick=()=>command('lyricOffset',{value:(Number(state.lyricOffset)||0)-500});
$('lyricPlus').onclick=()=>command('lyricOffset',{value:(Number(state.lyricOffset)||0)+500});
$('lyricReset').onclick=()=>command('lyricOffset',{value:0});
$('seek').addEventListener('pointerdown',()=>{seeking=true;});
$('seek').addEventListener('pointerup',()=>{seeking=false;});
$('seek').addEventListener('pointercancel',()=>{seeking=false;});
$('seek').onchange=()=>{seeking=false;command('seek',{value:Number($('seek').value)});};
$('fullscreen').onclick=()=>{if($('stage').requestFullscreen)$('stage').requestFullscreen().catch(()=>notify('Fullscreen is unavailable on this device.'));};

let remoteInput='';
function setRemoteDigits(raw){
 remoteInput=String(raw||'').replace(/\D/g,'').slice(-6).replace(/^0+/,'');
 $('remoteDigits').textContent=remoteInput.padStart(6,'0');
}
for(const digit of ['1','2','3','4','5','6','7','8','9','CLR','0','⌫']){
 const b=btn(digit,()=>{if(digit==='CLR')setRemoteDigits('');else if(digit==='⌫')setRemoteDigits(remoteInput.slice(0,-1));else setRemoteDigits(remoteInput+digit);},'Keypad '+digit);
 $('remoteKeypad').append(b);
}
function remoteSongAction(action){
 if(!remoteInput){notify('Enter or select a song number first.');return;}
 command(action,{id:remoteInput.padStart(6,'0')});
}
$('remotePlayNow').onclick=()=>remoteSongAction('play');
$('remoteReserve').onclick=()=>remoteSongAction('reserve');
$('remoteFirst').onclick=()=>remoteSongAction('reserveFirst');
$('remoteCancel').onclick=()=>remoteSongAction('cancelReservation');
$('remotePair').onclick=$('remotePairAgain').onclick=showPair;
$('remoteShowSongs').onclick=()=>page('songs');
$('remoteShowQueue').onclick=()=>page('queue');
document.querySelectorAll('[data-rcommand]').forEach(b=>b.onclick=()=>command(b.dataset.rcommand));
$('remoteLyricsFull').onclick=()=>{if($('remoteLyricStage').requestFullscreen)$('remoteLyricStage').requestFullscreen().catch(()=>notify('Fullscreen is unavailable.'));};
$('remoteLyricMinus').onclick=()=>command('lyricOffset',{value:(Number(state.lyricOffset)||0)-500});
$('remoteLyricPlus').onclick=()=>command('lyricOffset',{value:(Number(state.lyricOffset)||0)+500});
$('remoteLyricReset').onclick=()=>command('lyricOffset',{value:0});

function time(ms){let n=Math.max(0,Math.floor(ms/1000));return Math.floor(n/60)+':'+String(n%60).padStart(2,'0');}
let queueSignature='';function renderState(){const s=state,c=s.current;$('toggle').textContent=s.playing?'Ⅱ':'▶';$('nowTitle').textContent=c?c.title:'Select a song';$('nowId').textContent=c?c.id+' · '+c.artist:'READY TO SING';$('clock').textContent=time(s.position||0)+' / '+time(s.duration||0);$('progress').value=s.duration?s.position/s.duration:0;$('lyricBefore').textContent=s.before||'';$('lyricAfter').textContent=s.after||'';$('nextLyric').textContent=s.nextLyric||(s.loading?'Preparing audio…':c&&!s.before&&!s.after?'Instrumental · no timed lyrics in this file':'');$('keyValue').textContent=(s.key>0?'+':'')+s.key;$('tempoValue').textContent=Math.round(s.tempo*100)+'%';if(document.activeElement!==$('volume'))$('volume').value=Math.round(s.volume*100);$('melody').textContent='Melody channel 1: '+(s.melody?'on':'off');$('lyricOffsetValue').textContent=((Number(s.lyricOffset)||0)/1000).toFixed(1)+'s';
 const seek=$('seek');seek.disabled=!s.seekable;seek.max=Math.max(1,Math.round(s.duration||1));if(!seeking)seek.value=Math.min(Number(seek.max),Math.round(s.position||0));$('seekHint').textContent=s.seekable?'Drag to seek in this audio/video song':'Seeking available for audio/video songs';
 $('lyricBefore').classList.toggle('lineFill',Number(s.lyricProgress)>=0);$('lyricBefore').style.setProperty('--filled',Math.round(Math.max(0,Math.min(1,Number(s.lyricProgress)||0))*100)+'%');$('upNext').textContent=s.queue?.length?'Next: '+s.queue[0].title:'No reservations yet';
 if(!host){
 $('remoteCurrent').textContent=c?c.title+' · '+c.artist:'Ready to sing · Choose a song';
 $('remoteHost').textContent=location.host||'Wi-Fi pairing';
 $('remoteConnectionDetails').textContent='Connected to '+(location.host||'KantaGo')+' on the same Wi-Fi / hotspot.';
 $('remoteToggle').querySelector('span').textContent=s.playing?'Ⅱ':'▶';
 $('remoteKeyValue').textContent=(s.key>0?'+':'')+s.key;
 $('remoteTempoValue').textContent=Math.round(s.tempo*100)+'%';
 $('remoteVolumeValue').textContent=Math.round(s.volume*100)+'%';
 $('remoteMelody').textContent='♫ Melody '+(s.melody?'on':'off');
 $('remoteQueueCount').textContent=String((s.queue||[]).length);
 $('remoteLyricTitle').textContent=c?c.title:'Select a song';
 $('remoteLyricLine').textContent=(s.before||'')+(s.after||'')||'Your stage is ready';
 $('remoteLyricNext').textContent=s.nextLyric||(c?'Enjoy singing!':'Kanta anytime. Saya anywhere.');
 $('remoteLyricValue').textContent=((Number(s.lyricOffset)||0)/1000).toFixed(1)+'s';
 }
 const signature=JSON.stringify(s.queue||[]);if(signature!==queueSignature){queueSignature=signature;$('queueList').replaceChildren();if(!s.queue?.length)$('queueList').append(el('div','empty','Your queue is empty. Reserve a song to get started.'));(s.queue||[]).forEach((song,i)=>{let row=el('div','song'),info=el('div','songInfo');info.append(el('div','songTitle',(i+1)+'. '+song.title),el('div','songMeta',song.id+' · '+song.artist));row.append(info,btn('↑',()=>command('move',{index:i,direction:-1}),'Move up'),btn('↓',()=>command('move',{index:i,direction:1}),'Move down'),btn('×',()=>command('remove',{index:i}),'Remove reservation'));$('queueList').append(row);});}
 if(host){let bgkey=s.backgroundRevision+'-'+s.backgroundType;if(bgkey!==lastBg){lastBg=bgkey;$('bgVideo').pause();$('bgVideo').removeAttribute('src');$('bgVideo').classList.add('hidden');$('bgImage').classList.remove('hidden');if(s.background&&s.backgroundType==='video'){$('bgVideo').src='/background?token='+token;$('bgVideo').classList.remove('hidden');$('bgVideo').play().catch(()=>{});}else $('bgImage').src=s.background?'/background?token='+token:'background.svg';}
 let videoKey=s.isVideo&&c?c.id:'';if(videoKey!==lastVideo){lastVideo=videoKey;let v=$('songVideo');v.pause();v.classList.toggle('hidden',!videoKey);if(videoKey)v.src='/media?token='+token;else{v.removeAttribute('src');v.load();}}
 let v=$('songVideo');if(videoKey){if(Number.isFinite(v.duration)&&Math.abs(v.currentTime-(s.position||0)/1000)>.7)v.currentTime=(s.position||0)/1000;v.playbackRate=s.tempo||1;if(s.playing)v.play().catch(()=>{});else v.pause();}
 }
 const msg=s.notice||s.importStatus;if(msg&&msg!==lastNotice){lastNotice=msg;notify(msg);}
}
async function poll(){try{if(!token){showPair();return;}state=await api('state');$('connection').textContent=host?'Offline ready':'Connected';$('connection').style.color='';offline=false;renderState();if(state.revision!==revision){songs=await api('songs');revision=state.revision;renderStats();if(activePage==='songs')renderSongs();}}catch(e){$('connection').textContent='Disconnected';$('connection').style.color='#ff998f';if(!offline){offline=true;notify('Connection lost. Keep the main device open and both phones on the same network.');}}finally{setTimeout(poll,700);}}
page(activePage);poll();
