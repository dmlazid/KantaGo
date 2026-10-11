package com.kantago.app;
import android.content.res.AssetManager;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.SecureRandom;import java.util.*;import java.util.concurrent.*;
import org.json.*;
/** Small local-only HTTP server. No cloud, directory serving, or unauthenticated control. */
public final class LocalServer {
 public interface Api {String call(String path,String body)throws Exception; File background(); File mediaFile();}
 private final AssetManager assets;private final Api api;private ServerSocket server;
 private final ThreadPoolExecutor pool=new ThreadPoolExecutor(2,6,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(20));
 public final String token;public final String pin;public int port;private volatile boolean running=true;
 private final Map<String,long[]> attempts=new HashMap<>();
 public LocalServer(AssetManager a,Api api)throws IOException{this.assets=a;this.api=api;SecureRandom r=new SecureRandom();byte[] b=new byte[24];r.nextBytes(b);token=android.util.Base64.encodeToString(b,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE);pin=String.format(Locale.US,"%06d",r.nextInt(1000000));server=new ServerSocket(0);port=server.getLocalPort();new Thread(this::accept,"KantaGo local remote").start();}
 private void accept(){while(running)try{Socket s=server.accept();s.setSoTimeout(5000);try{pool.execute(()->handle(s));}catch(RejectedExecutionException e){s.close();}}catch(IOException e){if(!running)return;}}
 private String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')break;if(c!='\r')b.write(c);if(b.size()>8192)throw new IOException("Header too large");}return b.toString("UTF-8");}
 private synchronized boolean allowPair(String ip){long now=System.currentTimeMillis();long[] a=attempts.get(ip);if(a==null||now-a[0]>60000){if(attempts.size()>256)attempts.clear();a=new long[]{now,0};attempts.put(ip,a);}return ++a[1]<=5;}
 private void handle(Socket socket){try(Socket s=socket){InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();String[] first=line(in).split(" ");if(first.length!=3)return;String method=first[0];URI uri=new URI(first[1]);String path=uri.getPath();Map<String,String> h=new HashMap<>();for(int i=0;i<40;i++){String l=line(in);if(l.isEmpty())break;int p=l.indexOf(':');if(p>0)h.put(l.substring(0,p).toLowerCase(Locale.ROOT),l.substring(p+1).trim());if(i==39)throw new IOException("Too many headers");}
 int n=Integer.parseInt(h.getOrDefault("content-length","0"));if(n<0||n>16384){reply(out,413,"text/plain","Request too large".getBytes());return;}
 byte[] body=new byte[n];new DataInputStream(in).readFully(body);String json=new String(body,StandardCharsets.UTF_8);
 if(path.equals("/api/pair")){if(!method.equals("POST")){reply(out,405,"text/plain",new byte[0]);return;}if(!allowPair(s.getInetAddress().getHostAddress())){reply(out,429,"application/json","{\"error\":\"Too many attempts. Wait one minute.\"}".getBytes());return;}String p=new JSONObject(json).optString("pin");if(!pin.equals(p)){reply(out,403,"application/json","{\"error\":\"Wrong pairing code\"}".getBytes());return;}reply(out,200,"application/json",new JSONObject().put("token",token).toString().getBytes());return;}
 if(path.startsWith("/api/")||(path.equals("/background")||path.equals("/media"))){
  String auth=h.getOrDefault("authorization","");String q=uri.getRawQuery();boolean bg=(path.equals("/background")||path.equals("/media"))&&q!=null&&q.equals("token="+token);
  if(!auth.equals("Bearer "+token)&&!bg){reply(out,403,"application/json","{\"error\":\"Pair this phone first\"}".getBytes());return;}
  if((path.equals("/background")||path.equals("/media"))){File f=api.background();if(f==null){reply(out,404,"text/plain",new byte[0]);return;}sendFile(out,h,f);return;}
  if((path.equals("/api/command")&&!method.equals("POST"))||(!path.equals("/api/command")&&!method.equals("GET"))){reply(out,405,"text/plain",new byte[0]);return;}
  if(!Arrays.asList("/api/state","/api/songs","/api/command").contains(path)){reply(out,404,"text/plain",new byte[0]);return;}
  try{reply(out,200,"application/json",api.call(path,json).getBytes(StandardCharsets.UTF_8));}catch(Exception e){reply(out,400,"application/json",new JSONObject().put("error",e.getMessage()==null?"Command failed":e.getMessage()).toString().getBytes());}return;
 }
 if(!method.equals("GET")){reply(out,405,"text/plain",new byte[0]);return;}
 String file=path.equals("/")?"index.html":path.substring(1);if(!Arrays.asList("index.html","app.js","style.css","logo.svg","background.svg").contains(file)){reply(out,404,"text/plain",new byte[0]);return;}
 try(InputStream asset=assets.open(file)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int got;while((got=asset.read(buf))!=-1)b.write(buf,0,got);String type=file.endsWith("js")?"text/javascript":file.endsWith("css")?"text/css":file.endsWith("svg")?"image/svg+xml":file.endsWith("jpg")?"image/jpeg":"text/html";reply(out,200,type,b.toByteArray());}
 }catch(Exception ignored){} }
 private void reply(OutputStream o,int status,String type,byte[] b)throws IOException{String head="HTTP/1.1 "+status+" Response\r\nContent-Type: "+type+"; charset=utf-8\r\nContent-Length: "+b.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nConnection: close\r\n\r\n";o.write(head.getBytes(StandardCharsets.US_ASCII));o.write(b);}
 private void sendFile(OutputStream o,Map<String,String> h,File f)throws IOException{long start=0,end=f.length()-1;boolean partial=false;String range=h.get("range");if(range!=null&&range.matches("bytes=[0-9]+-[0-9]*")){String[] p=range.substring(6).split("-",-1);start=Long.parseLong(p[0]);if(!p[1].isEmpty())end=Math.min(end,Long.parseLong(p[1]));partial=true;}if(start>end||start>=f.length()){reply(o,416,"text/plain",new byte[0]);return;}String type=f.getName().endsWith("mp4")?"video/mp4":f.getName().endsWith("png")?"image/png":"image/jpeg";String head="HTTP/1.1 "+(partial?"206 Partial Content":"200 OK")+"\r\nContent-Type: "+type+"\r\nAccept-Ranges: bytes\r\nContent-Length: "+(end-start+1)+"\r\n"+(partial?"Content-Range: bytes "+start+"-"+end+"/"+f.length()+"\r\n":"")+"Connection: close\r\n\r\n";o.write(head.getBytes(StandardCharsets.US_ASCII));try(RandomAccessFile r=new RandomAccessFile(f,"r")){r.seek(start);byte[] b=new byte[32768];long left=end-start+1;while(left>0){int n=r.read(b,0,(int)Math.min(b.length,left));if(n<0)break;o.write(b,0,n);left-=n;}}}
 public void close(){running=false;try{server.close();}catch(IOException ignored){}pool.shutdownNow();}
}
