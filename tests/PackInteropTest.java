package com.kantago.app;
import java.io.*;
import java.nio.file.Files;

/** Verifies the independently produced Python song bundle on the JVM. */
public final class PackInteropTest {
 static void check(boolean v,String info){if(!v)throw new AssertionError(info);}
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("pass a .kgpack filename");
  File temp=Files.createTempDirectory("kantago-interop").toFile();
  File dest=new File(temp,"library");
  try{
   File verified;
   try(InputStream in=new FileInputStream(args[0])){
    verified=ActivatedPack.decrypt(in,temp,"KANTA-START-2026".toCharArray(),null);
   }
   SongPack.Result result;
   try(InputStream zip=new FileInputStream(verified)){
    result=SongPack.importZip(zip,dest,null);
   }
   check(result.files.size()==8,"eight original practice songs must import successfully");
   for(String song:result.files){
    File file=new File(dest,song);
    check(file.isFile()&&file.length()>100,"song must exist: "+song);
    byte[] data=Files.readAllBytes(file.toPath());
    MidiSong midi=MidiSong.parse(data);
    check(!midi.lyrics.isEmpty(),"timed lyrics required: "+song);
    check(midi.duration>10000,"song must be playable: "+song);
   }
   System.out.println("PASS: Python-written activation pack unlocks in Java; eight playable MIDI songs with lyrics");
  }finally{
   File[] songFiles=dest.listFiles();if(songFiles!=null)for(File f:songFiles)f.delete();
   dest.delete();
   File[] tempFiles=temp.listFiles();if(tempFiles!=null)for(File f:tempFiles)f.delete();
   temp.delete();
  }
 }
}
