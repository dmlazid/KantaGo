package com.kantago.app;
import android.app.*;import android.os.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.*;import android.view.*;import android.webkit.*;import android.media.*;import android.graphics.Bitmap;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.regex.*;import org.json.*;
import com.google.zxing.*;import com.google.zxing.common.BitMatrix;
public class MainActivity extends Activity implements LocalServer.Api {
 WebView web;LocalServer server;final Handler ui=new Handler(Looper.getMainLooper());final ExecutorService io=Executors.newSingleThreadExecutor();final MidiEngine midi=new MidiEngine();
 JSONArray songs=new JSONArray();final ArrayList<String> queue=new ArrayList<>();JSONObject current;MediaPlayer media;MidiSong score;final List<double[]> lrcTimes=new ArrayList<>();final List<String> lrcText=new ArrayList<>();
 File root,bg;QrCamera qrCamera;int key=0,lyricOffset=0;double tempo=1,volume=.7;boolean paused=false,remote=false,loading=false;String notice="",base,backgroundType="image";volatile String importStatus="";public static volatile String audioError="";long revision=1;int playGeneration=0;boolean destroyed=false;
 AudioManager audio;AudioFocusRequest focus;String importLanguage="Other",importYear="",packActivationKey="";
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);root=new File(getFilesDir(),"songs");root.mkdirs();lyricOffset=getPreferences(0).getInt("lyricOffset",0);
  try{File library=new File(getFilesDir(),"library.json");if(library.exists())songs=new JSONArray(read(library));if(songs.length()==0){File f=new File(root,"demo.mid");copy(getAssets().open("demo.mid"),f,1024*1024);songs.put(new JSONObject().put("id","000001").put("title","KantaGo Sound Check").put("artist","KantaGo original demo").put("language","English").put("year",2026).put("file",f.getName()).put("favorite",false));save();}
   String bgName=getPreferences(0).getString("background","");if(!bgName.isEmpty()){File f=new File(getFilesDir(),bgName);if(f.exists()){bg=f;backgroundType=f.getName().endsWith("mp4")?"video":"image";}}
   try{
    JSONArray previousQueue=new JSONArray(getPreferences(0).getString("savedQueue","[]"));
    for(int i=0;i<previousQueue.length()&&queue.size()<500;i++){
     String number=previousQueue.optString(i);
     try{find(number);queue.add(number);}catch(Exception ignored){}
    }
   }catch(Exception ignored){}
   server=new LocalServer(getAssets(),this);base="http://127.0.0.1:"+server.port;
  }catch(Exception e){new AlertDialog.Builder(this).setMessage("Could not start KantaGo: "+e.getMessage()).setPositiveButton("Close",(d,w)->finish()).show();return;}
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(change->{if(change<0)ui.post(()->{if(current!=null&&!paused)toggle();});}).build();
  web=new WebView(this);web.setBackgroundColor(0xff101827);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMediaPlaybackRequiresUserGesture(false);web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !r.getUrl().toString().startsWith(base+"/");}});web.addJavascriptInterface(new Bridge(),"Native");setContentView(web);home();
 }
 void home(){remote=false;web.removeJavascriptInterface("RemoteShell");web.addJavascriptInterface(new Bridge(),"Native");web.loadUrl(base+"/#token="+server.token+"&host=1");}
 class RemoteShell{
  @JavascriptInterface public void disconnect(){ui.post(()->home());}
 }
 class Bridge {
  @JavascriptInterface public void importFiles(String lang,String year,boolean folder){ui.post(()->{if(remote)return;importLanguage=Arrays.asList("English","OPM","Bisaya","Other").contains(lang)?lang:"Other";importYear=year;Intent i=new Intent(folder?Intent.ACTION_OPEN_DOCUMENT_TREE:Intent.ACTION_OPEN_DOCUMENT);if(!folder){i.setType("*/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);}startActivityForResult(i,folder?11:10);});}
  @JavascriptInterface public void openStarterPack(){ui.post(()->{
   if(remote)return;
   try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/dmlazid/KantaGo/releases")));}
   catch(Exception e){toast("No browser available for GitHub downloads.");}
  });}
  @JavascriptInterface public void importActivatedPack(String key,String lang,String year){
   if(key==null||key.trim().length()<8||key.trim().length()>256){ui.post(()->toast("Enter your song pack activation key"));return;}
   ui.post(()->{
    if(remote)return;
    packActivationKey=key.trim();
    importLanguage=Arrays.asList("English","OPM","Bisaya","Other").contains(lang)?lang:"Other";
    importYear=year;
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(intent,15);
   });
  }
  @JavascriptInterface public void importZip(String lang,String year){ui.post(()->{if(remote)return;importLanguage=Arrays.asList("English","OPM","Bisaya","Other").contains(lang)?lang:"Other";importYear=year;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,13);});}
  @JavascriptInterface public void background(){ui.post(()->{if(remote)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","video/mp4"});startActivityForResult(i,12);});}
  @JavascriptInterface public void pairing(){ui.post(()->showPairing());}
  @JavascriptInterface public void connect(String address){ui.post(()->connectRemote(address));}
  @JavascriptInterface public void scanPair(){ui.post(()->openQrScanner());}
  @JavascriptInterface public String previousRemote(){return getPreferences(0).getString("lastRemoteAddress","");}
  @JavascriptInterface public void exportSongs(){ui.post(()->{
   if(remote)return;
   Intent pick=new Intent(Intent.ACTION_CREATE_DOCUMENT);
   pick.addCategory(Intent.CATEGORY_OPENABLE);
   pick.setType("application/zip");
   pick.putExtra(Intent.EXTRA_TITLE,"KantaGo-song-backup.zip");
   startActivityForResult(pick,14);
  });}
 }
 void openQrScanner(){
  if(remote)return;
  if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{android.Manifest.permission.CAMERA},42);
   return;
  }
  if(qrCamera!=null)qrCamera.close();
  qrCamera=new QrCamera(this,this::connectRemote);
  try{qrCamera.show();}catch(Exception e){toast("Camera unavailable. Enter address manually.");}
 }
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(requestCode,permissions,grants);
  if(requestCode==42){
   if(grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)openQrScanner();
   else toast("Camera permission is optional. Enter the address manually instead.");
  }
 }
 void connectRemote(String address){
  try{
   String url=PairingUrl.normalize(address);
   getPreferences(0).edit().putString("lastRemoteAddress",url.split("#",2)[0]).apply();
   stop();
   remote=true;
   web.removeJavascriptInterface("Native");
   web.addJavascriptInterface(new RemoteShell(),"RemoteShell");
   web.loadUrl(url);
  }catch(Exception e){toast(e.getMessage()==null?"Check the pairing address":e.getMessage());}
 }
 void saveQueue(){getPreferences(0).edit().putString("savedQueue",new JSONArray(queue).toString()).apply();}
 void exportSongPack(Uri destination){
  importStatus="Exporting ZIP backup…";
  io.execute(()->{
   try(OutputStream out=getContentResolver().openOutputStream(destination)){
    if(out==null)throw new IOException("Cannot write ZIP at this location");
    try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(out)){
     LinkedHashSet<String> names=new LinkedHashSet<>();
     for(int i=0;i<songs.length();i++){
      String name=songs.getJSONObject(i).optString("file");
      if(name.isEmpty())continue;
      names.add(name);
      String lyric=name.replaceFirst("\\.[^.]+$","")+".lrc";
      if(new File(root,lyric).isFile())names.add(lyric);
     }
     int written=0;byte[] buffer=new byte[32768];
     File folder=root.getCanonicalFile();
     for(String name:names){
      File source=new File(root,name).getCanonicalFile();
      if(!folder.equals(source.getParentFile())||!source.isFile())continue;
      zip.putNextEntry(new java.util.zip.ZipEntry(source.getName()));
      try(InputStream media=new FileInputStream(source)){
       int n;while((n=media.read(buffer))!=-1)zip.write(buffer,0,n);
      }
      zip.closeEntry();
      importStatus="Backing up "+(++written)+" of "+names.size()+" files…";
     }
     zip.putNextEntry(new java.util.zip.ZipEntry("kantago-catalog.json"));
     zip.write(songs.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
     zip.closeEntry();
     importStatus="ZIP backup saved: "+written+" media/lyric files. Keep it somewhere safe.";
    }
   }catch(Exception e){importStatus="Song backup failed: "+e.getMessage();}
  });
 }
 String address(){try{for(Enumeration<NetworkInterface> n=NetworkInterface.getNetworkInterfaces();n.hasMoreElements();)for(Enumeration<InetAddress> a=n.nextElement().getInetAddresses();a.hasMoreElements();){InetAddress ip=a.nextElement();if(ip instanceof Inet4Address&&!ip.isLoopbackAddress()&&ip.isSiteLocalAddress())return "http://"+ip.getHostAddress()+":"+server.port;}}catch(Exception ignored){}return "Connect to Wi-Fi or enable a hotspot";}
 void showPairing(){String addr=address();android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(android.widget.LinearLayout.VERTICAL);box.setPadding(35,25,35,25);android.widget.TextView label=new android.widget.TextView(this);label.setText("On the other phone, join the same Wi-Fi/hotspot. Scan this QR with its camera, or open the address and enter the code.\n\n"+addr+"\nPairing code: "+server.pin+"\n\nKeep KantaGo open on this device. Only pair trusted phones.");label.setTextSize(17);box.addView(label);
  try{BitMatrix bits=new MultiFormatWriter().encode(addr+"/#token="+server.token,BarcodeFormat.QR_CODE,500,500);Bitmap b=Bitmap.createBitmap(500,500,Bitmap.Config.RGB_565);for(int y=0;y<500;y++)for(int x=0;x<500;x++)b.setPixel(x,y,bits.get(x,y)?0xff101827:0xffffffff);android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(b);box.addView(image,new android.widget.LinearLayout.LayoutParams(-1,500));}catch(Exception ignored){}
 new AlertDialog.Builder(this).setTitle("Pair a remote").setView(box).setPositiveButton("Done",null).setNeutralButton("Copy address",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("KantaGo",addr));toast("Address copied. Pairing code: "+server.pin);}).show();}
 @Override public String call(String path,String body)throws Exception{FutureTask<String> job=new FutureTask<>(()->{if(path.equals("/api/state"))return state().toString();if(path.equals("/api/songs"))return publicSongs().toString();return command(new JSONObject(body)).toString();});ui.post(job);return job.get(8,TimeUnit.SECONDS);}
 JSONArray publicSongs()throws Exception{JSONArray a=new JSONArray();for(int i=0;i<songs.length();i++){JSONObject s=new JSONObject(songs.getJSONObject(i).toString());String filename=s.optString("file");int dot=filename.lastIndexOf('.');s.put("format",dot<0?"":filename.substring(dot+1).toLowerCase(Locale.ROOT));s.remove("file");a.put(s);}return a;}
 JSONObject find(String id)throws Exception{for(int i=0;i<songs.length();i++)if(songs.getJSONObject(i).getString("id").equals(id))return songs.getJSONObject(i);throw new Exception("Song number not found");}
 JSONObject state()throws Exception{double pos=score!=null?midi.position:media!=null?safePosition():0;double dur=score!=null?score.duration:media!=null?safeDuration():0;
  double lyricPos=Math.max(0,pos+lyricOffset),lyricProgress=-1;
  StringBuilder before=new StringBuilder(),after=new StringBuilder();String next="";
  if(score!=null&&!score.lyrics.isEmpty()){
   List<MidiSong.Event> lyrics=score.lyrics;int start=0;for(int i=0;i<lyrics.size()&&lyrics.get(i).ms<=lyricPos;i++)if(newLine(lyrics.get(i).text))start=i;
   boolean ended=false;for(int i=start;i<lyrics.size();i++){MidiSong.Event e=lyrics.get(i);if(i>start&&newLine(e.text)){ended=true;for(int j=i;j<lyrics.size()&&(j==i||!newLine(lyrics.get(j).text));j++)next+=clean(lyrics.get(j).text);break;}if(e.ms<=lyricPos)before.append(clean(e.text));else after.append(clean(e.text));}
  }else if(!lrcTimes.isEmpty()){int index=-1;for(int i=0;i<lrcTimes.size();i++)if(lrcTimes.get(i)[0]<=lyricPos)index=i;before.append(index>=0?lrcText.get(index):"");if(index+1<lrcText.size())next=lrcText.get(index+1);if(index>=0){double start=lrcTimes.get(index)[0];double end=index+1<lrcTimes.size()?lrcTimes.get(index+1)[0]:Math.max(start+3000,dur);lyricProgress=Math.max(0,Math.min(1,(lyricPos-start)/Math.max(1,end-start)));}}
  JSONObject c=current==null?null:new JSONObject(current.toString());if(c!=null)c.remove("file");
  JSONArray q=new JSONArray();for(String id:queue){JSONObject s=new JSONObject(find(id).toString());s.remove("file");q.put(s);}
  if(!audioError.isEmpty()){notice=audioError;audioError="";paused=true;}
  return new JSONObject().put("current",c==null?JSONObject.NULL:c).put("queue",q).put("playing",current!=null&&!paused&&!loading).put("isVideo",current!=null&&current.optString("file").endsWith(".mp4")).put("loading",loading).put("position",pos).put("duration",dur).put("key",key).put("tempo",tempo).put("volume",volume).put("melody",midi.melody).put("before",before.toString()).put("after",after.toString()).put("nextLyric",next).put("lyricProgress",lyricProgress).put("lyricOffset",lyricOffset).put("seekable",media!=null&&!loading).put("notice",notice).put("importStatus",importStatus).put("revision",revision).put("background",bg!=null).put("backgroundType",backgroundType).put("backgroundRevision",getPreferences(0).getLong("bgRevision",0));
 }
 static boolean newLine(String s){return s.startsWith("/")||s.startsWith("\\")||s.startsWith("\r")||s.startsWith("\n");}static String clean(String s){return s.replace('/',' ').replace('\\',' ').replace('\r',' ').replace('\n',' ');}
 int safePosition(){try{return media.getCurrentPosition();}catch(Exception e){return 0;}}int safeDuration(){try{return media.getDuration();}catch(Exception e){return 0;}}
 JSONObject command(JSONObject p)throws Exception{String a=p.getString("action"),id=p.optString("id");notice="";switch(a){
  case "play":play(find(id));break;case "reserve":find(id);if(queue.size()>=500)throw new Exception("Queue is full");queue.add(id);break;
  case "reserveFirst":find(id);if(queue.size()>=500)throw new Exception("Queue is full");queue.add(0,id);break;
  case "cancelReservation":find(id);if(!queue.remove(id))throw new Exception("This song is not reserved");break;
  case "remove":int i=p.getInt("index");if(i>=0&&i<queue.size())queue.remove(i);break;
  case "move":int from=p.getInt("index"),to=from+p.getInt("direction");if(from>=0&&from<queue.size()&&to>=0&&to<queue.size())Collections.swap(queue,from,to);break;
  case "clearQueue":queue.clear();break;
  case "shuffleQueue":Collections.shuffle(queue);break;
  case "next":next();break;case "stop":stop();break;case "toggle":toggle();break;case "replay":if(current!=null)play(current);break;
  case "key":key=Math.max(-6,Math.min(6,p.getInt("value")));applyAudio();break;
  case "tempo":tempo=Math.max(.5,Math.min(1.5,p.getDouble("value")));applyAudio();break;
  case "volume":volume=Math.max(0,Math.min(1,p.getDouble("value")));applyAudio();break;
  case "melody":midi.melody=!midi.melody;break;
  case "seek":if(media==null||loading)throw new Exception("Seeking works for audio and video songs only");media.seekTo((int)Math.max(0,Math.min(safeDuration(),p.getDouble("value"))));break;
  case "lyricOffset":lyricOffset=Math.max(-10000,Math.min(10000,p.getInt("value")));getPreferences(0).edit().putInt("lyricOffset",lyricOffset).apply();break;
  case "favorite":JSONObject s=find(id);s.put("favorite",!s.optBoolean("favorite"));save();revision++;break;
  case "tag":JSONObject t=find(id);String lang=p.optString("language","Other");if(!Arrays.asList("English","OPM","Bisaya","Other").contains(lang))throw new Exception("Invalid language");int year=p.optInt("year",0);if(year!=0&&(year<1900||year>2100))throw new Exception("Use a year from 1900 to 2100");t.put("language",lang).put("year",year);save();revision++;break;
  default:throw new Exception("Unknown command");}
  if(Arrays.asList("reserve","reserveFirst","cancelReservation","remove","move","clearQueue","shuffleQueue").contains(a))saveQueue();
  return state();}
 void play(JSONObject song)throws Exception{stop();current=song;paused=false;loading=true;int generation=++playGeneration;File f=new File(root,song.getString("file"));audio.requestAudioFocus(focus);
  if(f.getName().endsWith(".mid")||f.getName().endsWith(".kar")){io.execute(()->{try{MidiSong parsed=MidiSong.parse(readBytes(f,16*1024*1024));ui.post(()->{if(destroyed||generation!=playGeneration)return;score=parsed;loading=false;applyAudio();midi.play(parsed,()->ui.post(()->{if(generation==playGeneration)next();}));});}catch(Exception e){ui.post(()->{if(generation==playGeneration){stop();notice="Cannot play MIDI: "+e.getMessage();}});}});
  }else {loadLrc(f);media=new MediaPlayer();media.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build());media.setDataSource(f.getAbsolutePath());media.setOnPreparedListener(m->{if(generation!=playGeneration)return;loading=false;applyAudio();m.start();});media.setOnCompletionListener(m->{if(generation==playGeneration)next();});media.setOnErrorListener((m,w,e)->{if(generation==playGeneration){stop();notice="This media file could not be played.";}return true;});media.prepareAsync();}
 }
 void stop(){playGeneration++;midi.stop();if(media!=null){media.release();media=null;}score=null;current=null;loading=false;paused=false;lrcTimes.clear();lrcText.clear();if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);}
 void next(){stop();if(!queue.isEmpty())try{String id=queue.remove(0);saveQueue();play(find(id));}catch(Exception e){notice=e.getMessage();}}
 void toggle(){if(current==null){next();return;}if(loading)return;paused=!paused;midi.paused=paused;if(media!=null){if(paused)media.pause();else{audio.requestAudioFocus(focus);media.start();}}}
 void applyAudio(){midi.key=key;midi.tempo=tempo;midi.volume=volume;if(media!=null&&!loading)try{media.setVolume((float)volume,(float)volume);media.setPlaybackParams(new PlaybackParams().setSpeed((float)tempo).setPitch((float)Math.pow(2,key/12.0)));if(paused)media.pause();}catch(Exception e){notice="Pitch/tempo unavailable for this media format.";}}
 void loadLrc(File f){File l=new File(root,f.getName().replaceFirst("\\.[^.]+$","")+".lrc");if(!l.exists())return;try{Pattern pattern=Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");TreeMap<Double,String> lines=new TreeMap<>();for(String line:read(l).split("\n")){Matcher m=pattern.matcher(line);String text=line.replaceAll("\\[[^]]*\\]","").trim();while(m.find())lines.put(Integer.parseInt(m.group(1))*60000+Double.parseDouble(m.group(2))*1000,text);}for(Map.Entry<Double,String> e:lines.entrySet()){lrcTimes.add(new double[]{e.getKey()});lrcText.add(e.getValue());}}catch(Exception e){notice="Could not read the matching LRC lyrics.";}}
 @Override public File background(){return bg;}
 @Override public File mediaFile(){try{return current==null?null:new File(root,current.getString("file"));}catch(Exception e){return null;}}
 @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null){if(req==15)packActivationKey="";return;}
  if(req==14){Uri file=data.getData();if(file!=null)exportSongPack(file);return;}
  final String language=importLanguage,year=importYear;
  final String activation=packActivationKey;packActivationKey="";importStatus="Importing…";io.execute(()->{try{if(req==12){Uri u=data.getData();String name=displayName(u).toLowerCase(Locale.ROOT);String ext=name.endsWith(".mp4")?"mp4":name.endsWith(".png")?"png":"jpg";File dest=new File(getFilesDir(),"custom-background."+ext);copy(getContentResolver().openInputStream(u),dest,500L*1024*1024);ui.post(()->{bg=dest;backgroundType=ext.equals("mp4")?"video":"image";getPreferences(0).edit().putString("background",dest.getName()).putLong("bgRevision",System.currentTimeMillis()).apply();importStatus="Background ready";});return;}
 if(req==15){
  Uri uri=data.getData();
  if(uri==null||!displayName(uri).toLowerCase(Locale.ROOT).endsWith(".kgpack"))throw new IOException("Select a .kgpack song bundle downloaded from KantaGo");
  importStatus="Checking activation key and pack integrity…";
  File verified=ActivatedPack.decrypt(getContentResolver().openInputStream(uri),getCacheDir(),activation.toCharArray(),
   (done,total)->importStatus="Unlocking songs: "+(100*done/Math.max(1,total))+"%…");
  try{
   SongPack.Result pack;
   try(FileInputStream zip=new FileInputStream(verified)){
    pack=SongPack.importZip(zip,root,(entries,count,skipped)->importStatus="Adding karaoke songs: "+count+"…");
   }
   registerPackSongs(pack,language,year);
   importStatus="Activated pack imported: "+pack.files.size()+" new karaoke songs, "+pack.lyricFiles+" lyric files; "+pack.skipped+" skipped.";
  }finally{verified.delete();}
  return;
 }
 if(req==13){
  Uri uri=data.getData();
  String archiveName=uri==null?"":displayName(uri).toLowerCase(Locale.ROOT);
  if(!archiveName.endsWith(".zip")&&!archiveName.endsWith(".7z"))throw new IOException("Select a ZIP or 7z karaoke pack");
  SongPack.Result extracted;
  if(archiveName.endsWith(".7z")){
   File temporary=new File(getCacheDir(),"kantago-import-"+System.currentTimeMillis()+".7z");
   try{
    importStatus="Preparing 7z song pack…";
    copy(getContentResolver().openInputStream(uri),temporary,SongPack.MAX_COMPRESSED_BYTES);
    extracted=SevenZipPack.importSevenZ(temporary,root,(entries,added,skipped)->importStatus="7z: "+entries+" entries, "+added+" songs, "+skipped+" skipped…");
   }finally{temporary.delete();}
  }else{
   importStatus="Reading ZIP song pack…";
   extracted=SongPack.importZip(getContentResolver().openInputStream(uri),root,(entries,added,skipped)->importStatus="ZIP: "+entries+" entries, "+added+" songs, "+skipped+" skipped…");
  }
  registerPackSongs(extracted,language,year);
  importStatus="ZIP complete: "+extracted.files.size()+" songs, "+extracted.lyricFiles+" lyrics; "+extracted.skipped+" skipped.";
  return;
 }
 List<Uri> files=new ArrayList<>();if(req==11)walk(data.getData(),DocumentsContract.getTreeDocumentId(data.getData()),files,0);else if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)files.add(data.getClipData().getItemAt(i).getUri());}else files.add(data.getData());
 int added=0,skipped=0;for(Uri u:files){String name=displayName(u).replaceAll("[^\\p{L}\\p{N} ._()-]","_");String ext=name.substring(Math.max(0,name.lastIndexOf('.')+1)).toLowerCase(Locale.ROOT);if(!Arrays.asList("mid","kar","mp3","mp4","wav","ogg","m4a","lrc").contains(ext)){skipped++;continue;}String normalized=name.substring(0,name.length()-ext.length())+ext;File dest=new File(root,normalized);if(dest.exists()){skipped++;continue;}try{copy(getContentResolver().openInputStream(u),dest,ext.equals("mid")||ext.equals("kar")?16L*1024*1024:500L*1024*1024);if(!ext.equals("lrc")){String title=name.replaceFirst("\\.[^.]+$","");String[] parts=title.split(" - ",2);final String artist=parts.length>1?parts[0]:"Unknown artist",songTitle=parts.length>1?parts[1]:title;FutureTask<Void> task=new FutureTask<>(()->{String id=String.format(Locale.US,"%06d",songs.length()+1);int y=0;try{y=Integer.parseInt(year);}catch(Exception ignored){}songs.put(new JSONObject().put("id",id).put("title",songTitle).put("artist",artist).put("language",language).put("year",y>=1900&&y<=2100?y:0).put("file",normalized).put("favorite",false));save();revision++;return null;});ui.post(task);task.get(10,TimeUnit.SECONDS);added++;}}catch(Exception e){dest.delete();skipped++;}importStatus="Imported "+added+" songs…";}
 importStatus="Import complete: "+added+" songs; "+skipped+" skipped (duplicates or unsupported files).";
 }catch(Exception e){importStatus="Import failed: "+e.getMessage();}});}
 void registerPackSongs(SongPack.Result extracted,String language,String year)throws Exception{
  if(extracted.files.isEmpty())return;
  FutureTask<Integer> task=new FutureTask<>(()->{
   int y=0;try{y=Integer.parseInt(year);}catch(Exception ignored){}
   int releaseYear=y>=1900&&y<=2100?y:0;
   JSONArray updated=new JSONArray(songs.toString());
   for(String filename:extracted.files){
    String label=filename.substring(0,filename.lastIndexOf('.'));
    String[] parts=label.split(" - ",2);
    String artist=parts.length>1?parts[0]:"Unknown artist";
    String title=parts.length>1?parts[1]:label;
    String id=String.format(Locale.US,"%06d",updated.length()+1);
    updated.put(new JSONObject().put("id",id).put("title",title).put("artist",artist)
      .put("language",language).put("year",releaseYear).put("file",filename).put("favorite",false));
   }
   JSONArray before=songs;
   songs=updated;
   try{save();}catch(Exception e){songs=before;throw e;}
   revision++;
   return extracted.files.size();
  });
  ui.post(task);
  task.get(45,TimeUnit.SECONDS);
 }
 void walk(Uri tree,String id,List<Uri> result,int depth)throws Exception{if(depth>12||result.size()>20000)throw new Exception("Folder too large; import smaller folders");Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(c==null)return;while(c.moveToNext()){String child=c.getString(0);if(DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)))walk(tree,child,result,depth+1);else{result.add(DocumentsContract.buildDocumentUriUsingTree(tree,child));if(result.size()>20000)throw new Exception("Import smaller folders");}}}}
 String displayName(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}return "song.mid";}
 static void copy(InputStream in,File f,long max)throws Exception{if(in==null)throw new IOException("Cannot read selected file");File tmp=new File(f.getPath()+".part");try(InputStream s=in;OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[32768];int n;long size=0;while((n=s.read(b))!=-1){size+=n;if(size>max)throw new IOException("File too large");out.write(b,0,n);}}catch(Exception e){tmp.delete();throw e;}if(!tmp.renameTo(f))throw new IOException("Cannot save imported file");}
 static byte[] readBytes(File f,int max)throws Exception{if(f.length()>max)throw new IOException("File too large");try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 static String read(File f)throws Exception{return new String(readBytes(f,20*1024*1024),java.nio.charset.StandardCharsets.UTF_8);}
 void save()throws Exception{android.util.AtomicFile f=new android.util.AtomicFile(new File(getFilesDir(),"library.json"));FileOutputStream out=f.startWrite();try{out.write(songs.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){f.failWrite(out);throw e;}}
 void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show();}
 @Override public void onBackPressed(){if(remote){home();return;}web.evaluateJavascript("window.goHome && window.goHome()",null);}
 @Override protected void onDestroy(){destroyed=true;if(qrCamera!=null)qrCamera.close();saveQueue();stop();if(server!=null)server.close();io.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
}
