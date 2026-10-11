package com.kantago.app;

import java.util.*;

/** Karaoke songbook-style four-line caption pages. No video/audio assets are copied. */
public final class KaraokeCaptions {
 private KaraokeCaptions(){}

 public static final class Line {
  public final String text, sung, pending;
  public final boolean active;
  Line(String text,String sung,String pending,boolean active) {
   this.text=text;this.sung=sung;this.pending=pending;this.active=active;
  }
 }

 static class Segment {
  double ms;String text;
  Segment(double ms,String text){this.ms=ms;this.text=text;}
 }
 static class Verse {
  double start;
  final List<Segment> words=new ArrayList<>();
  final StringBuilder text=new StringBuilder();
  Verse(double start){this.start=start;}
 }
 private static String cleaned(String raw) {
  return raw.replace('/',' ').replace('\\',' ').replace('\r',' ').replace('\n',' ');
 }
 private static boolean newline(String text) {
  return text.startsWith("/") || text.startsWith("\\") || text.startsWith("\n") || text.startsWith("\r");
 }
 private static Line[] page(List<Verse> verses,double positionMs,boolean approximateWordTimes) {
  Line[] result=new Line[4];
  for(int i=0;i<4;i++)result[i]=new Line("","","",false);
  if(verses.isEmpty())return result;
  int current=0;
  for(int i=0;i<verses.size();i++)if(verses.get(i).start<=positionMs)current=i;
  int first=(current/4)*4;
  for(int slot=0;slot<4;slot++){
   int index=first+slot;
   if(index>=verses.size())break;
   Verse verse=verses.get(index);
   String txt=verse.text.toString().trim();
   if(txt.isEmpty())continue;
   boolean active=index==current && positionMs>=verse.start;
   if(!active){result[slot]=new Line(txt,"",txt,false);continue;}
   StringBuilder sung=new StringBuilder(),waiting=new StringBuilder();
   if(approximateWordTimes){
    // Plain LRC only has line timestamps. This is an estimate, not precise
    // per-word alignment. Enhanced KAR fragments are aligned individually.
    double end=index+1<verses.size()?verses.get(index+1).start:verse.start+5000;
    double progress=Math.max(0,Math.min(1,(positionMs-verse.start)/Math.max(1000,end-verse.start)));
    String[] words=txt.split("\\s+");
    int spoken=(int)Math.floor(progress*words.length);
    for(int k=0;k<words.length;k++){
     if(k<spoken){if(sung.length()>0)sung.append(' ');sung.append(words[k]);}
     else{if(waiting.length()>0||sung.length()>0)waiting.append(' ');waiting.append(words[k]);}
    }
   }else{
    for(Segment segment:verse.words)
     (segment.ms<=positionMs?sung:waiting).append(segment.text);
   }
   String spokenText=sung.toString();
   String remaining=waiting.toString();
   if(spokenText.isEmpty()&&remaining.isEmpty())remaining=txt;
   result[slot]=new Line(txt,spokenText,remaining,true);
  }
  return result;
 }
 public static Line[] midi(List<MidiSong.Event> events,double ms){
  List<Verse> verses=new ArrayList<>();
  Verse verse=null;
  for(MidiSong.Event e:events){
   if(e.text==null||e.text.trim().isEmpty())continue;
   if(verse==null||newline(e.text)){
    verse=new Verse(e.ms);verses.add(verse);
   }
   String word=cleaned(e.text);
   verse.text.append(word);
   verse.words.add(new Segment(e.ms,word));
  }
  return page(verses,ms,false);
 }
 public static Line[] lrc(List<double[]> times,List<String> text,double ms){
  List<Verse> verses=new ArrayList<>();
  for(int i=0;i<times.size()&&i<text.size();i++){
   Verse verse=new Verse(times.get(i)[0]);
   String words=text.get(i);if(words==null||words.trim().isEmpty())continue;
   verse.text.append(words);verse.words.add(new Segment(verse.start,words));
   verses.add(verse);
  }
  return page(verses,ms,true);
 }
}
