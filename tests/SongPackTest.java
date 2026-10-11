package com.kantago.app;
import java.io.*;
import java.util.zip.*;
public final class SongPackTest {
 static void ok(boolean pass,String reason){if(!pass)throw new AssertionError(reason);}
 static byte[] zip()throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(ZipOutputStream out=new ZipOutputStream(bytes)){
   for(String name:new String[]{"EN/Artist - Title.MP3","EN/Artist - Title.LRC","BI/Song.kar","../../escape.mp3","README.txt","dup/Artist - Title.mp3"}){
    out.putNextEntry(new ZipEntry(name));out.write("demo".getBytes("UTF-8"));out.closeEntry();
   }
  }
  return bytes.toByteArray();
 }
 public static void main(String[] args)throws Exception {
  File dir=new File(System.getProperty("java.io.tmpdir"),"kantago-zip-"+System.nanoTime());
  ok(dir.mkdirs(),"mkdir");
  try {
   SongPack.Result result=SongPack.importZip(new ByteArrayInputStream(zip()),dir,null);
   ok(result.files.size()==2,"two new songs");
   ok(result.lyricFiles==1,"matching LRC");
   ok(result.skipped==3,"reject unsafe paths, duplicates and unsupported entries");
   ok(new File(dir,"Artist - Title.mp3").exists(),"extension normalization");
   ok(new File(dir,"Artist - Title.lrc").exists(),"lyric normalization");
   ok(new File(dir,"Song.kar").exists(),"KAR import");
   ok(!new File(dir,"escape.mp3").exists(),"path traversal blocked");
   ok(SongPack.importZip(new ByteArrayInputStream(zip()),dir,null).files.isEmpty(),"existing files not overwritten");
   System.out.println("PASS: safe ZIP extraction, nested folders, media, LRC, duplicates");
  } finally {
   File[] files=dir.listFiles();if(files!=null)for(File file:files)file.delete();dir.delete();
  }
 }
}
