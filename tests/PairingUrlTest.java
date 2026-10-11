package com.kantago.app;
public final class PairingUrlTest {
 static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("expected "+expected+" got "+actual);}
 static void invalid(String url){
  try{PairingUrl.normalize(url);throw new AssertionError("Accepted bad URL: "+url);}
  catch(IllegalArgumentException expected){}
 }
 public static void main(String[] args){
  eq("http://192.168.1.8:40000/",PairingUrl.normalize("192.168.1.8:40000"));
  eq("http://172.20.10.1:3030/",PairingUrl.normalize("http://172.20.10.1:3030/"));
  eq("http://10.1.2.3:9000/#token=abcdefghijklmnopqrstuvwxyzABCDEF",PairingUrl.normalize("http://10.1.2.3:9000/#token=abcdefghijklmnopqrstuvwxyzABCDEF"));
  invalid("http://evil.example:8000/");
  invalid("8.8.8.8:4000");
  invalid("192.168.1.1:0");
  invalid("http://192.168.1.9:8000/other/path");
  invalid("http://192.168.1.9:8000/?token=abc");
  invalid("http://user@192.168.1.9:8000");
  invalid("http://192.168.1.9:8000/#token=bad");
  invalid("http://192.168.1.999:8000");
  System.out.println("PASS: KantaGo local pairing URLs, QR fragments and invalid remote hosts");
 }
}
