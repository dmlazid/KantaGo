package com.kantago.app;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Safe streaming extraction for user-owned karaoke ZIP packs. */
public final class SongPack {
 public static final int MAX_ENTRIES=2500;
 public static final long MAX_TOTAL_BYTES=1024L*1024*1024;
 public static final long MAX_COMPRESSED_BYTES=768L*1024*1024;
 static final Set<String> TYPES=new HashSet<>(Arrays.asList("mid","kar","mp3","mp4","wav","ogg","m4a","lrc"));
 private SongPack(){}
 public interface Progress {void update(int entries,int songs,int skipped);}
 public static final class Result {
  public final List<String> files=new ArrayList<>();
  public int lyricFiles,entries,skipped;
 }
 static final class BoundedInput extends FilterInputStream {
  long count;
  BoundedInput(InputStream s){super(s);}
  void check(int n)throws IOException{if(n>0&&(count+=n)>MAX_COMPRESSED_BYTES)throw new IOException("ZIP compressed data exceeds 768 MB");}
  @Override public int read()throws IOException{int b=in.read();if(b!=-1)check(1);return b;}
  @Override public int read(byte[] b,int o,int len)throws IOException{int n=in.read(b,o,len);check(n);return n;}
 }
 static String cleanName(String raw){
  if(raw==null||raw.length()>600||raw.indexOf('\0')>=0)return null;
  String path=raw.replace('\\','/');
  if(path.startsWith("/")||path.contains(":"))return null;
  String[] parts=path.split("/",-1);
  for(String part:parts)if(part.isEmpty()||part.equals(".")||part.equals(".."))return null;
  String name=parts[parts.length-1];
  if(name.length()>180)return null;
  name=name.replaceAll("[^\\p{L}\\p{N} ._()-]","_");
  int dot=name.lastIndexOf('.');
  if(dot<=0||dot==name.length()-1)return null;
  String ext=name.substring(dot+1).toLowerCase(Locale.ROOT);
  if(!TYPES.contains(ext))return null;
  return name.substring(0,dot)+"."+ext;
 }
 static long maxSize(String name){
  String ext=name.substring(name.lastIndexOf('.')+1);
  return ext.equals("lrc")?4L*1024*1024:
   (ext.equals("mid")||ext.equals("kar"))?16L*1024*1024:500L*1024*1024;
 }
 public static Result importZip(InputStream input,File directory,Progress progress)throws Exception{
  if(input==null)throw new IOException("Cannot read selected ZIP");
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot make song folder");
  Result result=new Result();
  Set<String> known=new HashSet<>();
  File[] old=directory.listFiles();
  if(old!=null)for(File file:old)known.add(file.getName().toLowerCase(Locale.ROOT));
  List<File> extracted=new ArrayList<>(),pending=new ArrayList<>();
  long total=0;
  try(ZipInputStream zip=new ZipInputStream(new BoundedInput(input))){
   ZipEntry entry;
   byte[] buffer=new byte[32768];
   while((entry=zip.getNextEntry())!=null){
    if(++result.entries>MAX_ENTRIES)throw new IOException("Too many ZIP entries; split into smaller packs");
    String name=entry.isDirectory()?null:cleanName(entry.getName());
    String lower=name==null?"":name.toLowerCase(Locale.ROOT);
    boolean allowed=name!=null&&!known.contains(lower);
    File dst=allowed?new File(directory,name):null;
    File tmp=allowed?new File(directory,name+".zip-part"):null;
    if(tmp!=null)pending.add(tmp);
    long size=0,limit=name==null?500L*1024*1024:maxSize(name);
    try(OutputStream out=allowed?new FileOutputStream(tmp):new ByteArrayOutputStream(0)){
     int n;
     while((n=zip.read(buffer))!=-1){
      size+=n;total+=n;
      if(size>limit||total>MAX_TOTAL_BYTES)throw new IOException("ZIP extraction safety limit exceeded");
      if(allowed)out.write(buffer,0,n);
     }
    }
    if(allowed){
     if(!tmp.renameTo(dst))throw new IOException("Could not save extracted file");
     pending.remove(tmp);
     extracted.add(dst);
     known.add(lower);
     if(lower.endsWith(".lrc"))result.lyricFiles++;
     else result.files.add(name);
    }else if(!entry.isDirectory())result.skipped++;
    zip.closeEntry();
    if(progress!=null&&result.entries%10==0)progress.update(result.entries,result.files.size(),result.skipped);
   }
  }catch(Exception error){
   for(File file:pending)file.delete();
   for(File file:extracted)file.delete();
   throw error;
  }
  if(progress!=null)progress.update(result.entries,result.files.size(),result.skipped);
  return result;
 }
}
