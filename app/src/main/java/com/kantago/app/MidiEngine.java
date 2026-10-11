package com.kantago.app;
import android.media.*;
import java.util.*;
/** Lightweight built-in synth; deliberately not a SoundFont or sampled General MIDI engine. */
public final class MidiEngine {
 static final int RATE=22050,BLOCK=512;static final double[][] WAVES=new double[8][2048];
 static{for(int w=0;w<8;w++)for(int i=0;i<2048;i++){double p=i*Math.PI*2/2048;WAVES[w][i]=(Math.sin(p)+Math.sin(2*p)*w/18.0+Math.sin(3*p)*w/30.0)/(1+w/18.0+w/30.0);}}
 static final class Voice{int ch,n;double phase,amp,age;boolean release;Voice(int c,int n,int v){ch=c;this.n=n;amp=v/127.0;}}
 public volatile boolean active,paused;public volatile double position,tempo=1,volume=.7;public volatile int key;public volatile boolean melody=true;
 private Thread thread;private volatile AudioTrack track;
 public void play(MidiSong song,Runnable complete){stop();active=true;paused=false;position=0;
  thread=new Thread(()->render(song,complete),"KantaGo synth");thread.start();
 }
 private void render(MidiSong song,Runnable complete){
  AudioTrack at=null;try{
   int size=Math.max(AudioTrack.getMinBufferSize(RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT),BLOCK*4);
   at=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(size).setTransferMode(AudioTrack.MODE_STREAM).build();track=at;at.play();
   ArrayList<Voice> voices=new ArrayList<>();int[] program=new int[16];double[] gain=new double[16];Arrays.fill(gain,1);boolean[] sustain=new boolean[16];int idx=0;short[] out=new short[BLOCK];Random noise=new Random(7);
   while(active&&position<song.duration){
    if(paused){at.pause();Thread.sleep(25);continue;}if(at.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)at.play();
    for(int i=0;i<BLOCK;i++){
     while(idx<song.events.size()&&song.events.get(idx).ms<=position){MidiSong.Event e=song.events.get(idx++);
      if(e.kind==144&&e.b>0){if(voices.size()>=48)voices.remove(0);voices.add(new Voice(e.ch,e.a,e.b));}
      else if(e.kind==128||(e.kind==144&&e.b==0)){for(Voice v:voices)if(v.ch==e.ch&&v.n==e.a)v.release=true;}
      else if(e.kind==192)program[e.ch]=e.a;
      else if(e.kind==176){if(e.a==7)gain[e.ch]=e.b/127.0;if(e.a==64)sustain[e.ch]=e.b>=64;if(e.a==120||e.a==123)voices.removeIf(v->v.ch==e.ch);}
     }
     double sample=0;for(int j=voices.size()-1;j>=0;j--){Voice v=voices.get(j);v.age+=1.0/RATE;
      double decay=v.ch==9?.998:(v.release&&!sustain[v.ch]?.992:.999995);v.amp*=decay;
      if(v.amp<.002){voices.remove(j);continue;}
      v.phase+=(440*Math.pow(2,(v.n-69+key)/12.0))/RATE;v.phase-=Math.floor(v.phase);
      double sound=v.ch==9?noise.nextDouble()*2-1:WAVES[program[v.ch]/16][(int)(v.phase*2048)%2048];
      if(melody||v.ch!=0)sample+=sound*v.amp*gain[v.ch]*Math.min(1,v.age*150);
     }
     out[i]=(short)(Math.tanh(sample*.16)*volume*28000);position+=1000.0/RATE*tempo;
    }
    int written=at.write(out,0,out.length);if(written<0)throw new IllegalStateException("Audio output failed");
   }
   boolean ended=active;active=false;if(ended)complete.run();
  }catch(Exception e){if(active)MainActivity.audioError=e.getMessage()==null?"MIDI audio failed":e.getMessage();active=false;}
  finally{if(at!=null){try{at.stop();}catch(Exception ignored){}at.release();}track=null;}
 }
 public void stop(){active=false;if(thread!=null){thread.interrupt();try{thread.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}thread=null;}position=0;}
}
