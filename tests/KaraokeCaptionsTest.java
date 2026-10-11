package com.kantago.app;
import java.util.*;

public final class KaraokeCaptionsTest {
 static void ok(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] args) {
  List<MidiSong.Event> events=new ArrayList<>();
  for(int i=0;i<8;i++){
   MidiSong.Event start=new MidiSong.Event(0,5,0,0,0);
   start.ms=i*1500.0;start.text="/Verse "+(i+1)+" ";
   events.add(start);
   MidiSong.Event second=new MidiSong.Event(0,5,0,0,0);
   second.ms=i*1500.0+500;second.text="now";
   events.add(second);
  }
  KaraokeCaptions.Line[] first=KaraokeCaptions.midi(events,500);
  ok(first.length==4,"exactly four lines in the window");
  ok(first[0].active,"first line is active");
  ok(first[0].sung.contains("Verse 1")&&first[0].sung.contains("now"),"first words sung");
  ok(first[1].text.equals("Verse 2 now"),"next line shown");
  ok(first[3].text.equals("Verse 4 now"),"fourth line shown");
  KaraokeCaptions.Line[] second=KaraokeCaptions.midi(events,1700);
  ok(second[1].active,"second line becomes active");
  ok(second[1].sung.contains("Verse 2")&&!second[1].sung.contains("now"),"current line highlights only completed fragments");
  ok(second[1].pending.contains("now"),"future word remains white");
  KaraokeCaptions.Line[] third=KaraokeCaptions.midi(events,6400);
  ok(third[0].text.equals("Verse 5 now")&&third[0].active,"page moves to fifth verse");
  ok(third[3].text.equals("Verse 8 now"),"window holds all four next verses");
  List<double[]> times=Arrays.asList(new double[]{0},new double[]{1000},new double[]{2000},new double[]{3000},new double[]{4000});
  List<String> words=Arrays.asList("Sing with me now","Second lyric line","Third lyric line","Fourth lyric line","Fifth lyric line");
  KaraokeCaptions.Line[] lrc=KaraokeCaptions.lrc(times,words,2500);
  ok(lrc[2].active,"LRC line timestamp chooses current line");
  ok(lrc[2].sung.length()>0&&lrc[2].pending.length()>0,"LRC fallback estimates word progress");
  KaraokeCaptions.Line[] empty=KaraokeCaptions.midi(Collections.emptyList(),0);
  ok(empty.length==4&&empty[0].text.isEmpty(),"instrumental songs have no invented words");
  System.out.println("PASS: four caption lines, stanza switching, timed word coloring and LRC fallback");
 }
}
