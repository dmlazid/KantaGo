#!/usr/bin/env node
'use strict';
// Regression coverage for Android WebView karaoke text becoming dark/transparent.
const fs=require('fs'),assert=require('assert'),vm=require('vm');
const js=fs.readFileSync('app/src/main/assets/app.js','utf8');
const css=fs.readFileSync('app/src/main/assets/style.css','utf8');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');

assert(html.includes('id="lyricBefore"')&&html.includes('id="lyricAfter"'), 'Current line must be split for singing progress');
assert(html.includes('id="remoteLiveLyrics"'), 'Paired remote stage needs high contrast captions');
assert(css.includes('#stage .lyrics #lyricBefore')&&css.includes('#stage .lyrics #lyricAfter'), 'Both sung and waiting lyrics need solid fill');
assert(css.includes('-webkit-text-fill-color:#ffe654!important'), 'Sung lyrics should be opaque, bright gold');
assert(css.includes('-webkit-text-fill-color:#ffffff!important'), 'Upcoming lyrics should be opaque white');
assert(css.includes('rgba(1,5,16,.88)'), 'Lyrics should have a strong dark backdrop');
assert(js.includes("$('lyricBefore').classList.remove('lineFill')"), 'Old transparent gradient effect must not be active');

const start=js.indexOf('function updateAnimatedKaraoke(s){');
const end=js.indexOf('\nlet queueSignature=',start);
assert(start>=0&&end>start, 'Find animated karaoke state updater');
const code=js.slice(start,end);
const makeVideo=()=>({
  paused:true,
  attrs:{},
  classList:{toggle(){}},
  getAttribute(k){return this.attrs[k]||null;},
  setAttribute(k,v){this.attrs[k]=v;},
  load(){},
  play(){this.paused=false;return Promise.resolve();},
  pause(){this.paused=true;}
});
const hostScene=makeVideo(),remoteScene=makeVideo();
const elements={
  stage:{dataset:{scene:'auto'}},stageSceneVideo:hostScene,remoteStageVideo:remoteScene,
  lyricFull:{textContent:'old gradient text'},
  remoteLiveLyrics:{dataset:{},replaceChildren(...children){this.children=children;}}
};
const context={
  host:true,$:id=>elements[id],
  document:{createElement(){return {className:'',textContent:''};}},
  console
};
vm.createContext(context);
vm.runInContext(code,context);
const state={
 current:{id:'700008',title:'Starlight Parade'},background:false,isVideo:false,
 playing:true,before:'Follow ',after:'little stars',lyricProgress:0.25
};
vm.runInContext('updateAnimatedKaraoke',context)(state);
assert.strictEqual(elements.lyricFull.textContent,'','Remove gradient-clipped main lyric render');
assert.strictEqual(elements.remoteLiveLyrics.children[0].textContent,'Follow ','Highlighted spoken words');
assert.strictEqual(elements.remoteLiveLyrics.children[1].textContent,'little stars','Waiting words');
assert.strictEqual(elements.remoteLiveLyrics.children[0].className,'karaokeSung');
assert.strictEqual(elements.remoteLiveLyrics.children[1].className,'karaokeUpcoming');
assert.strictEqual(hostScene.attrs.src,'/scenes/disco.mp4','Video remains as karaoke stage');
state.before='Follow little ';state.after='stars';
vm.runInContext('updateAnimatedKaraoke',context)(state);
assert.strictEqual(elements.remoteLiveLyrics.children[0].textContent,'Follow little ');
assert.strictEqual(elements.remoteLiveLyrics.children[1].textContent,'stars');
console.log('PASS: visible karaoke split-color lyric highlights on host and remote, scene retained');
