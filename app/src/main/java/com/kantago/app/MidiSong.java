package com.kantago.app;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Bounded SMF format 0/1 parser. Timeline converts ticks using the complete tempo map. */
public final class MidiSong {
 public static final class Event {
  public long tick; public double ms; public int kind,ch,a,b; public String text="";
  Event(long t,int k,int c,int a,int b){tick=t;kind=k;ch=c;this.a=a;this.b=b;}
 }
 public final List<Event> events=new ArrayList<>(); public final List<Event> lyrics=new ArrayList<>();
 public double duration; public int division;
 public static MidiSong parse(byte[] bytes) throws IOException {
  if(bytes.length>16*1024*1024)throw new IOException("MIDI file is too large");
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
  if(in.readInt()!=0x4d546864)throw new IOException("Not a MIDI/KAR file");
  int len=in.readInt(); if(len<6||len>1024)throw new IOException("Invalid MIDI header");
  int format=in.readUnsignedShort(),tracks=in.readUnsignedShort(),div=in.readUnsignedShort();
  if(format>1||tracks<1||tracks>256||(div&0x8000)!=0||div==0)throw new IOException("Only MIDI format 0/1 with beat timing is supported");
  in.skipBytes(len-6); MidiSong song=new MidiSong();song.division=div;
  int order=0;
  for(int track=0;track<tracks;track++) {
   if(in.readInt()!=0x4d54726b)throw new IOException("Missing MIDI track");
   int n=in.readInt();if(n<0||n>in.available())throw new IOException("Truncated MIDI track");
   byte[] data=new byte[n];in.readFully(data);DataInputStream t=new DataInputStream(new ByteArrayInputStream(data));
   long tick=0;int running=0;
   while(t.available()>0){
    tick+=vlq(t); if(tick>100000000)throw new IOException("MIDI timeline too long");
    int status=t.readUnsignedByte(),first=-1;
    if(status<128){if(running==0)throw new IOException("Invalid running status");first=status;status=running;}
    if(status==255){
     int type=t.readUnsignedByte(),size=vlq(t);if(size>t.available())throw new IOException("Invalid meta event");byte[] d=new byte[size];t.readFully(d);
     if(type==81&&size==3){int tempo=((d[0]&255)<<16)|((d[1]&255)<<8)|(d[2]&255);if(tempo>0)song.events.add(new Event(tick,81,0,tempo,0));}
     if(type==1||type==5){Event e=new Event(tick,type,0,0,0);e.text=decode(d);song.events.add(e);}
     if(type==47){song.events.add(new Event(tick,47,0,0,0));break;}
    }else if(status==240||status==247){int size=vlq(t);if(size>t.available())throw new IOException("Invalid SysEx");t.skipBytes(size);running=0;
    }else if(status>=128&&status<240){
     running=status;int k=status&240,c=status&15;int a=first<0?t.readUnsignedByte():first;int b=(k==192||k==208)?0:t.readUnsignedByte();
     if(a>127||b>127)throw new IOException("Invalid MIDI data");song.events.add(new Event(tick,k,c,a,b));
    }else throw new IOException("Unsupported MIDI event");
    if(++order>500000)throw new IOException("Too many MIDI events");
   }
  }
  song.events.sort(Comparator.comparingLong(e->e.tick));
  long last=0;double ms=0;int tempo=500000;boolean hasLyrics=song.events.stream().anyMatch(e->e.kind==5&&!e.text.trim().isEmpty());
  for(Event e:song.events){ms+=(e.tick-last)*(double)tempo/div/1000.0;last=e.tick;e.ms=ms;if(e.kind==81)tempo=e.a;
   if((e.kind==5||(!hasLyrics&&e.kind==1))&&!e.text.startsWith("@")&&!e.text.trim().isEmpty())song.lyrics.add(e);
  }
  song.duration=ms+1000;if(song.duration>3600000)throw new IOException("MIDI longer than one hour is not supported");return song;
 }
 static int vlq(DataInputStream in)throws IOException{int v=0;for(int i=0;i<4;i++){int b=in.readUnsignedByte();v=(v<<7)|(b&127);if((b&128)==0)return v;}throw new IOException("Invalid variable-length value");}
 static String decode(byte[] b){String s=new String(b,StandardCharsets.UTF_8);return s.indexOf('\ufffd')>=0?new String(b,StandardCharsets.ISO_8859_1):s;}
}
