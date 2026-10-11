package com.kantago.app;
import java.nio.file.*;import java.util.*;import java.io.*;
public class MidiTest {
 public static void main(String[] args)throws Exception{
  byte[] b=Files.readAllBytes(Path.of("app/src/main/assets/demo.mid"));MidiSong s=MidiSong.parse(b);
  if(s.lyrics.size()!=32||Math.abs(s.duration-17000)>1)throw new AssertionError("Demo timeline/lyrics");
  if(Math.abs(s.lyrics.get(8).ms-4000)>1)throw new AssertionError("Lyric synchronization");
  for(int n:new int[]{0,10,30,b.length-5})try{MidiSong.parse(Arrays.copyOf(b,n));throw new AssertionError("Truncated data accepted");}catch(IOException good){}
  byte[] smpte=b.clone();smpte[12]=(byte)0x80;try{MidiSong.parse(smpte);throw new AssertionError("SMPTE accepted");}catch(IOException good){}
  System.out.println("PASS: MIDI timeline, 32 lyric cues, truncated data and unsupported timing");
 }
}
