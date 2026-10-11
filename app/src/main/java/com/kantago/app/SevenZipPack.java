package com.kantago.app;

import java.io.*;
import java.util.*;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;

/** Stream archive entries to private storage with size bounds and no overwrite. */
public final class SevenZipPack {
 private SevenZipPack(){}

 public static SongPack.Result importSevenZ(File archive,File folder,SongPack.Progress progress)throws Exception {
  if(!archive.isFile())throw new IOException("Unable to open .7z archive");
  if(archive.length()>SongPack.MAX_COMPRESSED_BYTES)throw new IOException("7z pack is too large (768 MB limit)");
  if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create songs folder");
  SongPack.Result result=new SongPack.Result();
  Set<String> known=new HashSet<>();
  File[] old=folder.listFiles();
  if(old!=null)for(File file:old)known.add(file.getName().toLowerCase(Locale.ROOT));
  List<File> saved=new ArrayList<>(),pending=new ArrayList<>();
  byte[] buffer=new byte[32768];
  long total=0;
  try(SevenZFile seven=new SevenZFile(archive)){
   SevenZArchiveEntry entry;
   while((entry=seven.getNextEntry())!=null){
    if(++result.entries>SongPack.MAX_ENTRIES)throw new IOException("Too many entries in 7z pack");
    String name=entry.isDirectory()?null:SongPack.cleanName(entry.getName());
    String lower=name==null?"":name.toLowerCase(Locale.ROOT);
    boolean valid=name!=null&&!known.contains(lower);
    File target=valid?new File(folder,name):null;
    File part=valid?new File(folder,name+".7z-part"):null;
    if(part!=null)pending.add(part);
    long cap=name==null?500L*1024*1024:SongPack.maxSize(name);
    if(entry.getSize()>cap)throw new IOException("Archive song exceeds size limit");
    long entrySize=0;
    try(OutputStream output=valid?new FileOutputStream(part):new ByteArrayOutputStream(0)){
     int read;
     while((read=seven.read(buffer))!=-1){
      entrySize+=read;total+=read;
      if(entrySize>cap||total>SongPack.MAX_TOTAL_BYTES)throw new IOException("7z pack exceeds the 1 GB extraction limit");
      if(valid)output.write(buffer,0,read);
     }
    }
    if(valid){
     if(!part.renameTo(target))throw new IOException("Could not save "+name);
     pending.remove(part);
     saved.add(target);known.add(lower);
     if(lower.endsWith(".lrc"))result.lyricFiles++;
     else result.files.add(name);
    } else if(!entry.isDirectory())result.skipped++;
    if(progress!=null&&result.entries%10==0)progress.update(result.entries,result.files.size(),result.skipped);
   }
  }catch(Exception e){
   for(File file:pending)file.delete();
   for(File file:saved)file.delete();
   throw e;
  }
  if(progress!=null)progress.update(result.entries,result.files.size(),result.skipped);
  return result;
 }
}
