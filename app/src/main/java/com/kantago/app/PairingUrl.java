package com.kantago.app;
import java.net.URI;
import java.util.Locale;

/** Strictly accepts a private-network KantaGo host or its session QR URL. */
public final class PairingUrl {
 private PairingUrl(){}
 public static String normalize(String raw) {
  if(raw==null)throw new IllegalArgumentException("Enter the address shown on the main phone");
  String input=raw.trim();
  if(!input.startsWith("http://"))input="http://"+input;
  try {
   URI uri=new URI(input);
   if(!"http".equals(uri.getScheme()) || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null)
    throw new IllegalArgumentException("Only local KantaGo addresses are supported");
   String host=uri.getHost();
   if(host==null||!host.matches("[0-9.]+")||uri.getPort()<1||uri.getPort()>65535)
    throw new IllegalArgumentException("Enter the Wi-Fi address including its port");
   String[] octets=host.split("\\.",-1);
   if(octets.length!=4)throw new IllegalArgumentException("Not an IPv4 address");
   int[] n=new int[4];
   for(int i=0;i<4;i++){
    if(octets[i].isEmpty()||octets[i].length()>3)throw new IllegalArgumentException("Not an IPv4 address");
    n[i]=Integer.parseInt(octets[i]);
    if(n[i]<0||n[i]>255)throw new IllegalArgumentException("Not an IPv4 address");
   }
   if(!(n[0]==10 || n[0]==192&&n[1]==168 || n[0]==172&&n[1]>=16&&n[1]<=31 || n[0]==127))
    throw new IllegalArgumentException("Pair only on private Wi-Fi or hotspot");
   String path=uri.getRawPath();
   if(path!=null&&!path.isEmpty()&&!path.equals("/"))
    throw new IllegalArgumentException("Not a KantaGo pairing link");
   String fragment=uri.getRawFragment();
   if(fragment!=null&&!fragment.matches("token=[A-Za-z0-9_-]{20,100}"))
    throw new IllegalArgumentException("Invalid KantaGo pairing code");
   return "http://"+String.format(Locale.US,"%d.%d.%d.%d",n[0],n[1],n[2],n[3])+":"+uri.getPort()+"/"+(fragment==null?"":"#"+fragment);
  }catch(IllegalArgumentException e){throw e;}
   catch(Exception e){throw new IllegalArgumentException("Check the address shown on the main phone");}
 }
}
