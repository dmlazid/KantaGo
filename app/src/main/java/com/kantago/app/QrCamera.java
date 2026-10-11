package com.kantago.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

/** Temporary rear-camera QR reader: images never leave the device and are not saved. */
public final class QrCamera implements AutoCloseable {
 public interface Callback{void onScan(String url);}
 private final Activity activity;
 private final Callback callback;
 private final AtomicBoolean found=new AtomicBoolean(false);
 private Dialog dialog;
 private TextureView preview;
 private HandlerThread thread;
 private Handler cameraThread;
 private CameraDevice camera;
 private CameraCaptureSession session;
 private ImageReader images;
 private Surface previewSurface;
 private long lastFrame;
 private volatile boolean stopped;

 public QrCamera(Activity activity,Callback callback){this.activity=activity;this.callback=callback;}

 public void show(){
  if(activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
   throw new IllegalStateException("Camera permission is required to scan a QR code");
  stopped=false;
  thread=new HandlerThread("KantaGo QR reader");
  thread.start();cameraThread=new Handler(thread.getLooper());
  dialog=new Dialog(activity);
  FrameLayout layout=new FrameLayout(activity);
  layout.setBackgroundColor(0xff061426);
  preview=new TextureView(activity);
  layout.addView(preview,new FrameLayout.LayoutParams(-1,-1));
  TextView instruction=new TextView(activity);
  instruction.setText("Point your camera at the pairing QR on the main KantaGo phone");
  instruction.setTextSize(16);
  instruction.setTextColor(0xffffffff);
  instruction.setGravity(Gravity.CENTER);
  instruction.setBackgroundColor(0xb9001524);
  FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
  top.setMargins(12,12,12,0);
  instruction.setPadding(18,15,18,15);
  layout.addView(instruction,top);
  TextView cancel=new TextView(activity);
  cancel.setText("Cancel scan");
  cancel.setTextSize(18);
  cancel.setTextColor(0xffffffff);
  cancel.setGravity(Gravity.CENTER);
  cancel.setBackgroundColor(0xff165e62);
  cancel.setPadding(24,18,24,18);
  FrameLayout.LayoutParams bottom=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
  bottom.setMargins(20,0,20,18);
  layout.addView(cancel,bottom);
  cancel.setOnClickListener(v->dialog.dismiss());
  preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
   @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){openCamera();}
   @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int w,int h){}
   @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){return true;}
   @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
  });
  dialog.setOnDismissListener(d->close());
  dialog.setContentView(layout);
  dialog.show();
  Window w=dialog.getWindow();
  if(w!=null){int h=(int)(activity.getResources().getDisplayMetrics().density*430);
   w.setLayout(-1,h);w.setBackgroundDrawableResource(android.R.color.transparent);}
  if(preview.isAvailable())openCamera();
 }
 private void error(String text){
  activity.runOnUiThread(()->{
   if(stopped)return;
   Toast.makeText(activity,text,Toast.LENGTH_LONG).show();
   if(dialog!=null && dialog.isShowing())dialog.dismiss();
  });
 }
 @SuppressLint("MissingPermission")
 private void openCamera(){
  if(stopped||cameraThread==null||camera!=null||images!=null)return;
  try{
   CameraManager manager=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
   String selected=null;
   for(String id:manager.getCameraIdList()){
    CameraCharacteristics characteristics=manager.getCameraCharacteristics(id);
    Integer facing=characteristics.get(CameraCharacteristics.LENS_FACING);
    if(facing!=null && facing==CameraCharacteristics.LENS_FACING_BACK){selected=id;break;}
   }
   if(selected==null)throw new IllegalStateException("No rear camera found");
   StreamConfigurationMap map=manager.getCameraCharacteristics(selected).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
   if(map==null)throw new IllegalStateException("Camera preview is unavailable");
   android.util.Size[] sizes=map.getOutputSizes(ImageFormat.YUV_420_888);
   if(sizes==null||sizes.length==0)throw new IllegalStateException("This camera cannot read QR images");
   android.util.Size best=sizes[0];
   long diff=Long.MAX_VALUE;
   for(android.util.Size size:sizes){
    if(size.getWidth()>1280||size.getHeight()>960||size.getWidth()<320||size.getHeight()<240)continue;
    long candidate=Math.abs(size.getWidth()-640)+Math.abs(size.getHeight()-480);
    if(candidate<diff){diff=candidate;best=size;}
   }
   int width=best.getWidth(),height=best.getHeight();
   images=ImageReader.newInstance(width,height,ImageFormat.YUV_420_888,2);
   images.setOnImageAvailableListener(this::decodeFrame,cameraThread);
   SurfaceTexture texture=preview.getSurfaceTexture();
   if(texture==null)throw new IllegalStateException("Camera preview has not initialized");
   texture.setDefaultBufferSize(width,height);
   previewSurface=new Surface(texture);
   manager.openCamera(selected,new CameraDevice.StateCallback(){
    @Override public void onOpened(CameraDevice c){
     if(stopped){c.close();return;}
     camera=c;startPreview();
    }
    @Override public void onDisconnected(CameraDevice c){c.close();error("Camera disconnected");}
    @Override public void onError(CameraDevice c,int cause){c.close();error("Camera could not start");}
   },cameraThread);
  }catch(Exception e){error("QR scanner unavailable: "+(e.getMessage()==null?"camera error":e.getMessage()));}
 }
 private void startPreview(){
  try{
   if(stopped||camera==null||images==null||previewSurface==null)return;
   List<Surface> outputs=Arrays.asList(previewSurface,images.getSurface());
   camera.createCaptureSession(outputs,new CameraCaptureSession.StateCallback(){
    @Override public void onConfigured(CameraCaptureSession configured){
     if(stopped){configured.close();return;}
     session=configured;
     try{
      CaptureRequest.Builder request=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      request.addTarget(previewSurface);request.addTarget(images.getSurface());
      request.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      configured.setRepeatingRequest(request.build(),null,cameraThread);
     }catch(Exception e){error("Unable to scan QR");}
    }
    @Override public void onConfigureFailed(CameraCaptureSession configured){error("Camera preview configuration failed");}
   },cameraThread);
  }catch(Exception e){error("Camera configuration failed");}
 }
 private void decodeFrame(ImageReader reader){
  Image image=null;
  try{
   image=reader.acquireLatestImage();
   if(image==null||stopped||found.get())return;
   long now=SystemClock.elapsedRealtime();
   if(now-lastFrame<180)return;
   lastFrame=now;
   Image.Plane y=image.getPlanes()[0];
   int width=image.getWidth(),height=image.getHeight(),stride=y.getRowStride(),pixel=y.getPixelStride();
   byte[] luminance=new byte[width*height];
   ByteBuffer bytes=y.getBuffer();
   for(int row=0;row<height;row++)for(int col=0;col<width;col++)
    luminance[row*width+col]=bytes.get(row*stride+col*pixel);
   PlanarYUVLuminanceSource src=new PlanarYUVLuminanceSource(luminance,width,height,0,0,width,height,false);
   BinaryBitmap bitmap=new BinaryBitmap(new HybridBinarizer(src));
   MultiFormatReader decoder=new MultiFormatReader();
   Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
   hints.put(DecodeHintType.POSSIBLE_FORMATS,Collections.singletonList(BarcodeFormat.QR_CODE));
   hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
   decoder.setHints(hints);
   String scanned=decoder.decodeWithState(bitmap).getText();
   String url=PairingUrl.normalize(scanned);
   if(found.compareAndSet(false,true)){
    activity.runOnUiThread(()->{
     if(stopped)return;
     if(dialog!=null&&dialog.isShowing())dialog.dismiss();
     callback.onScan(url);
    });
   }
  }catch(NotFoundException|IllegalArgumentException ignored){
   // Keep scanning until a valid local KantaGo QR is visible.
  }catch(Exception e){
   // Ignore malformed/incomplete frames; the next camera frame can succeed.
  }finally{if(image!=null)image.close();}
 }
 @Override public void close(){
  if(stopped)return;
  stopped=true;
  if(session!=null){session.close();session=null;}
  if(camera!=null){camera.close();camera=null;}
  if(images!=null){images.close();images=null;}
  if(previewSurface!=null){previewSurface.release();previewSurface=null;}
  if(thread!=null){thread.quitSafely();thread=null;cameraThread=null;}
 }
}
