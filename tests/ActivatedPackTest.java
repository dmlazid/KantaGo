package com.kantago.app;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.zip.*;
public class ActivatedPackTest{
 static void must(boolean test,String label){if(!test)throw new AssertionError(label);}
 static byte[] generate(String key,byte[] zip)throws Exception{
  byte[] header=new byte[ActivatedPack.HEADER_SIZE];
  System.arraycopy(ActivatedPack.MAGIC,0,header,0,8);
  byte[] salt=new byte[16],prefix=new byte[8];
  new SecureRandom().nextBytes(salt);new SecureRandom().nextBytes(prefix);
  System.arraycopy(salt,0,header,8,16);
  System.arraycopy(prefix,0,header,24,8);
  ByteBuffer.wrap(header,32,8).putLong(zip.length);
  SecretKeySpec secret=ActivatedPack.derive(key.toCharArray(),salt);
  ByteArrayOutputStream buf=new ByteArrayOutputStream();buf.write(header);
  for(int i=0,offset=0;offset<zip.length;i++){
   int count=Math.min(ActivatedPack.CHUNK_SIZE,zip.length-offset);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
   c.init(Cipher.ENCRYPT_MODE,secret,new GCMParameterSpec(128,ActivatedPack.iv(prefix,i)));
   c.updateAAD(header);
   c.updateAAD(ByteBuffer.allocate(4).putInt(i).array());
   buf.write(c.doFinal(zip,offset,count));offset+=count;
  }
  return buf.toByteArray();
 }
 static byte[] zip()throws Exception{
  ByteArrayOutputStream buf=new ByteArrayOutputStream();
  try(ZipOutputStream archive=new ZipOutputStream(buf)){
   archive.putNextEntry(new ZipEntry("KantaGo - Welcome.mid"));
   byte[] b=new byte[550000];new SecureRandom().nextBytes(b);
   archive.write(b);archive.closeEntry();
  }
  return buf.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  File folder=new File(System.getProperty("java.io.tmpdir"),"kantago-activate-"+System.nanoTime());
  must(folder.mkdirs(),"create cache");
  try{
   byte[] pack=generate("KANTA-SAMPLE-2026",zip());
   File decrypted=ActivatedPack.decrypt(new ByteArrayInputStream(pack),folder,"KANTA-SAMPLE-2026".toCharArray(),null);
   must(decrypted.length()>500000,"multi-chunk decryption");
   decrypted.delete();
   try{ActivatedPack.decrypt(new ByteArrayInputStream(pack),folder,"WRONG-KEY".toCharArray(),null);throw new AssertionError("accepted incorrect key");}catch(IOException expected){}
   pack[pack.length-100]^=3;
   try{ActivatedPack.decrypt(new ByteArrayInputStream(pack),folder,"KANTA-SAMPLE-2026".toCharArray(),null);throw new AssertionError("accepted altered ciphertext");}catch(IOException expected){}
   must(folder.listFiles().length==0,"no partial files after failure");
   System.out.println("PASS: activated pack decrypts, wrong key rejected, tampering rejected");
  }finally{for(File f:folder.listFiles())f.delete();folder.delete();}
 }
}
