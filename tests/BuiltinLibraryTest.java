package com.kantago.app;
import java.nio.file.*;
import java.util.*;
import java.io.*;

/** Ensures APK generation does not ship fake song rows or unplayable MIDI files. */
public class BuiltinLibraryTest {
 private static void check(boolean condition,String description){
  if(!condition)throw new AssertionError(description);
 }
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new Exception("Pass generated built-in asset folder");
  Path folder=Paths.get(args[0]);
  String catalog=Files.readString(folder.resolve("catalog.json"));
  int rows=0;
  for(int pos=0;(pos=catalog.indexOf("\"source\":\"builtin\"",pos))>=0;pos+=12)rows++;
  check(rows==48,"Expected 48 built-in karaoke song catalog entries, got "+rows);
  List<Path> songs=new ArrayList<>();
  try(java.util.stream.Stream<Path> files=Files.list(folder)){
   files.filter(f->f.getFileName().toString().matches("kb-[0-9]{6}\\.kar")).forEach(songs::add);
  }
  check(songs.size()==48,"Expected 48 physical MIDI assets, got "+songs.size());
  Set<Integer> distinctNotes=new HashSet<>();
  long bytes=0;
  for(Path file:songs){
   byte[] content=Files.readAllBytes(file);
   bytes+=content.length;
   MidiSong parsed=MidiSong.parse(content);
   check(parsed.duration>=12000,"Song too short: "+file.getFileName());
   check(parsed.lyrics.size()>=4,"Missing timed karaoke lyrics: "+file.getFileName());
   long realNotes=parsed.events.stream().filter(e->e.kind==0x90&&e.b>0).count();
   check(realNotes>=10,"No playable note track: "+file.getFileName());
   distinctNotes.add((int)(parsed.duration/100));
  }
  check(distinctNotes.size()>=6,"Every composition should not be the same length");
  System.out.println("PASS: 48 playable bundled karaoke songs with MIDI arrangements and timed lyrics ("+bytes+" bytes)");
 }
}
