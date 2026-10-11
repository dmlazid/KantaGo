package com.kantago.app;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Offline activated KantaGo song bundle reader, with authenticated 256 KiB chunks.
 * File format: magic(8), salt(16), nonce prefix(8), zip byte length(8),
 * then AES-256-GCM ciphertext frames (<=256 KiB data + 16-byte tag).
 *
 * Each chunk authenticates the entire 40-byte header and its chunk counter.
 * Only a fully verified ZIP is passed to SongPack; never import untrusted plaintext.
 */
public final class ActivatedPack {
 public static final byte[] MAGIC="KGPACK1\n".getBytes(StandardCharsets.US_ASCII);
 public static final int CHUNK_SIZE=256*1024, HEADER_SIZE=40, SALT_SIZE=16, TAG_SIZE=16;
 public static final int ITERATIONS=210000;
 private ActivatedPack(){}
 public interface Progress{void update(long done,long total);}
 static byte[] iv(byte[] prefix,int chunk){
  byte[] nonce=Arrays.copyOf(prefix,12);
  ByteBuffer.wrap(nonce,8,4).order(ByteOrder.BIG_ENDIAN).putInt(chunk);
  return nonce;
 }
 static SecretKeySpec derive(char[] pass,byte[] salt)throws Exception{
  if(pass==null||pass.length<4||pass.length>256)throw new IOException("Enter the activation key from your KantaGo song pack");
  PBEKeySpec kdf=new PBEKeySpec(pass,salt,ITERATIONS,256);
  try{
   byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(kdf).getEncoded();
   try{return new SecretKeySpec(key,"AES");}finally{Arrays.fill(key,(byte)0);}
  }finally{kdf.clearPassword();}
 }
 /** Decrypt to a temporary ZIP, verify every tag and reject trailing data. */
 public static File decrypt(InputStream stream,File cache,char[] activation,Progress progress)throws Exception{
  if(stream==null)throw new IOException("Cannot open pack file");
  if(!cache.isDirectory()&&!cache.mkdirs())throw new IOException("Cannot prepare import cache");
  File verified=File.createTempFile("kantago-verified-", ".zip",cache);
  try(InputStream raw=stream;DataInputStream input=new DataInputStream(new BufferedInputStream(raw))){
   byte[] header=new byte[HEADER_SIZE];
   input.readFully(header);
   if(!Arrays.equals(Arrays.copyOf(header,8),MAGIC))throw new IOException("Not a KantaGo activated song pack");
   byte[] salt=Arrays.copyOfRange(header,8,24);
   byte[] prefix=Arrays.copyOfRange(header,24,32);
   long total=ByteBuffer.wrap(header,32,8).order(ByteOrder.BIG_ENDIAN).getLong();
   if(total<=0||total>SongPack.MAX_COMPRESSED_BYTES)throw new IOException("Pack is too large or invalid");
   SecretKeySpec key=derive(activation,salt);
   long done=0;
   int chunk=0;
   try(OutputStream output=new BufferedOutputStream(new FileOutputStream(verified))){
    while(done<total){
     int size=(int)Math.min(CHUNK_SIZE,total-done);
     byte[] encrypted=new byte[size+TAG_SIZE];
     input.readFully(encrypted);
     Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
     cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv(prefix,chunk)));
     cipher.updateAAD(header);
     cipher.updateAAD(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(chunk).array());
     byte[] plain;
     try{plain=cipher.doFinal(encrypted);}
     catch(AEADBadTagException bad){throw new IOException("Wrong activation key or damaged song pack");}
     output.write(plain);
     done+=plain.length;
     chunk++;
     if(progress!=null&&chunk%8==0)progress.update(done,total);
    }
   }
   if(input.read()!=-1)throw new IOException("Pack includes unexpected extra data");
   if(progress!=null)progress.update(total,total);
   return verified;
  }catch(Exception failed){
   verified.delete();
   if(failed instanceof EOFException)throw new IOException("Song pack download is incomplete",failed);
   throw failed;
  }finally{if(activation!=null)Arrays.fill(activation,'\0');}
 }
}
