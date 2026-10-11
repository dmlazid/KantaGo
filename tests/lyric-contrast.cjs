#!/usr/bin/env node
'use strict';
const fs=require('fs'),assert=require('assert'),vm=require('vm');
const js=fs.readFileSync('app/src/main/assets/app.js','utf8');
const css=fs.readFileSync('app/src/main/assets/style.css','utf8');
const html=fs.readFileSync('app/src/main/assets/index.html','utf8');

assert(html.includes('id="karaokeFourLines"'),'Main player must have a 4-line lyric window');
assert(html.includes('id="remoteFourLines"'),'Remote must have a 4-line window');
assert(css.includes('.karaokeFourLines .captionSung'),'Highlighted lyric style required');
assert(css.includes('-webkit-text-fill-color:#244afc!important'),'Sung words must be bright opaque blue');
assert(css.includes('.karaokeFourLines .captionWaiting'),'Upcoming text must stay readable white');
assert(js.includes("$('lyricBefore').classList.remove('lineFill')"),'Unreliable gradient effect must be disabled');

const start=js.indexOf('function updateCaptionWindow(');
const end=js.indexOf('\nlet queueSignature=',start);
assert(start>=0&&end>start,'Find lyric window and video updater');
const code=js.slice(start,end);
function mockNode(){
 return {
  children:[],dataset:{},style:{},attrs:{},textContent:'',
  classList:{toggle(){}},
  append(...items){this.children.push(...items)},
  replaceChildren(...items){this.children=items},
  getAttribute(k){return this.attrs[k]||null;},
  setAttribute(k,v){this.attrs[k]=v;},
  load(){},paused:true,
  play(){this.paused=false;return Promise.resolve();},
  pause(){this.paused=true;}
 };
}
const nodes={
 stage:mockNode(),stageSceneVideo:mockNode(),remoteStageVideo:mockNode(),
 lyricFull:mockNode(),remoteLiveLyrics:mockNode(),
 karaokeFourLines:mockNode(),remoteFourLines:mockNode()
};
nodes.stage.dataset.scene='auto';
const sandbox={host:true,$:id=>nodes[id],document:{createElement:mockNode},console};
vm.createContext(sandbox);
vm.runInContext(code,sandbox);
const s={
 current:{id:'700008',title:'Starlight Parade'},background:false,isVideo:false,
 playing:true,before:'Follow ',after:'little stars',lyricProgress:.25,
 captions:[
 {text:'Follow little stars',sung:'Follow ',pending:'little stars',active:true},
 {text:'Dancing through the night',active:false},
 {text:'Raise your voice and sing',active:false},
 {text:'Everything is bright',active:false}
 ]
};
sandbox.updateAnimatedKaraoke(s);
assert.strictEqual(nodes.karaokeFourLines.children.length,4,'Exactly four caption lines');
assert.strictEqual(nodes.karaokeFourLines.children[0].children[0].textContent,'Follow ','Active words highlighted');
assert.strictEqual(nodes.karaokeFourLines.children[0].children[1].textContent,'little stars','Not yet sung remains white');
assert.strictEqual(nodes.karaokeFourLines.children[2].children[1].textContent,'Raise your voice and sing','Future verses displayed');
assert.strictEqual(nodes.remoteFourLines.children.length,4,'Paired remote also receives four lines');
assert.strictEqual(nodes.stageSceneVideo.attrs.src,'/scenes/spectrum.mp4','Original abstract video selected for the song');
s.captions[0]={text:'Follow little stars',sung:'Follow little ',pending:'stars',active:true};
s.captions[1]={text:'Dancing through the night',sung:'',pending:'Dancing through the night',active:false};
sandbox.updateAnimatedKaraoke(s);
assert.strictEqual(nodes.karaokeFourLines.children[0].children[0].textContent,'Follow little ','Timed song captions update in place');
console.log('PASS: four synchronized lyric lines, blue/white word splits, remote and animated stage');
