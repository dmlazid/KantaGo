#!/usr/bin/env python3
"""Render four original, silent loopable animated karaoke stage videos.
All graphics are original procedural artwork. Requires Pillow, NumPy and ffmpeg.
No copyrighted music or external footage is downloaded.
"""
import math,subprocess,argparse
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
W,H,FPS,FRAMES=512,288,12,84
def ffmpeg_binary():
 import shutil
 system=shutil.which('ffmpeg')
 if system:return system
 import imageio_ffmpeg
 return imageio_ffmpeg.get_ffmpeg_exe()

def poly(d,p,c):d.polygon([(int(x),int(y)) for x,y in p],fill=c)
def circ(d,x,y,r,c):d.ellipse((int(x-r),int(y-r),int(x+r),int(y+r)),fill=c)
def dancer(image,t,colors,center):
 d=ImageDraw.Draw(image,'RGBA'); x,y=center
 x+=math.sin(t*.8)*10;y+=math.sin(t*7)*7
 base,light,dark=colors
 d.ellipse((x-77,248,x+77,265),fill=(5,4,22,115))
 for k in [-1,1]:
  px=x+k*31;tx=px+math.sin(t*7+k)*9
  poly(d,[(px-7,y+68),(px+9,y+67),(tx+9,y+123),(tx-9,y+124)],(17,24,48,255))
  d.ellipse((tx-20,y+110,tx+20,y+126),fill=(230,244,255,255))
 for k in [-1,1]:
  sx=x+k*52;sy=y-13
  ex=x+k*(87+20*math.sin(t*5+k));ey=y+8-26*math.sin(t*5+k)
  d.line([(sx,sy),(ex,ey)],fill=(17,31,64,255),width=18)
  circ(d,ex,ey,12,(249,254,255,255))
 poly(d,[(x-54,y-69),(x+38,y-72),(x+56,y+73),(x-46,y+74)],dark)
 poly(d,[(x-63,y-76),(x+28,y-76),(x+45,y+68),(x-55,y+72)],base)
 poly(d,[(x-63,y-76),(x-30,y-103),(x+54,y-99),(x+28,y-76)],light)
 poly(d,[(x+28,y-76),(x+54,y-99),(x+56,y+73),(x+45,y+68)],dark)
 d.line([(x-52,y-57),(x-44,y+56)],fill=(255,255,255,75),width=4)
 d.line([(x-50,y-20),(x+47,y-22)],fill=(14,28,58,255),width=12)
 for gx in [x-28,x+20]:
  d.rounded_rectangle((gx-24,y-41,gx+24,y+7),radius=16,fill=(11,27,57,255),outline=(24,43,85,255),width=4)
  d.rounded_rectangle((gx-14,y-32,gx+12,y-13),radius=8,fill=(80,156,204,115))
  circ(d,gx-7,y-27,4,(194,237,255,120))
 d.arc((x-12,y+17,x+12,y+34),0,180,fill=(19,29,65,255),width=3)

def abstract_spectrum(t):
 # Original animated palette of shifting chevrons and layered confetti triangles.
 img=Image.new('RGBA',(W,H),(247,223,188,255))
 d=ImageDraw.Draw(img,'RGBA')
 palette=[(247,89,136,245),(241,202,75,248),(75,203,186,245),(252,151,88,240)]
 for row in range(11):
  for col in range(15):
   center=(col*47-row%2*25-45+int(13*math.sin(t*.7+row*.6)))%635-60
   y=row*34-30+int(10*math.sin(t*.9+col*.4))
   size=12+(col*7+row*5)%12
   color=palette[(row*3+col)%len(palette)]
   d.polygon([(center,y-size),(center-size,y+size),(center+size,y+size)],fill=color)
   if col%3==0:
    d.line([(center-17,y-19),(center,y-size)],fill=(255,255,255,135),width=5)
 for n in range(11):
  xx=(n*83+20+int(24*math.sin(t+n)))%W
  yy=(n*61+40+int(21*math.cos(t+n)))%H
  circ(d,xx,yy,3+(n%4),(255,255,255,145))
 return img.convert('RGB')

def abstract_tunnel(t):
 # Independent original concentric dotted ribbons with a zooming perspective.
 img=Image.new('RGBA',(W,H),(7,11,32,255))
 d=ImageDraw.Draw(img,'RGBA')
 center=(W*.51,H*.45)
 palette=[(42,182,251,248),(252,138,72,240),(215,64,183,238)]
 for ring in range(15):
  radius=(ring*25+int(t*29))%400+10
  for j in range(58):
   a=2*math.pi*j/58 +t*.08 +ring*.04
   x=center[0]+math.cos(a)*radius*1.25
   y=center[1]+math.sin(a)*radius*.75
   if -10<x<W+10 and -10<y<H+10:
    size=max(1,min(8,int(radius/68)+1))
    circ(d,x,y,size,palette[(ring+j//11)%3])
 for ray in range(12):
  angle=ray*math.pi/6
  d.line([(center[0],center[1]),(center[0]+math.cos(angle)*410,center[1]+math.sin(angle)*340)],fill=(57,92,175,50),width=2)
 return img.convert('RGB')

def frame(scene,index):
 t=index/FPS
 if scene=='spectrum':return abstract_spectrum(t)
 if scene=='tunnel':return abstract_tunnel(t)
 t=index/FPS; yy,xx=np.mgrid[0:H,0:W];v=yy/H
 a=np.zeros((H,W,3),dtype=np.uint8)
 if scene=='disco':
  a[:,:,0]=(24+50*(1-v)+15*np.sin(xx/W*math.pi)).clip(0,255)
  a[:,:,1]=(8+18*v).clip(0,255);a[:,:,2]=(78+62*v+14*np.sin(xx/W*4)).clip(0,255)
 else:
  a[:,:,0]=(70+107*(1-v)).clip(0,255)
  a[:,:,1]=(72+118*(1-v)).clip(0,255);a[:,:,2]=(139+36*v).clip(0,255)
 im=Image.fromarray(a,'RGB').convert('RGBA');d=ImageDraw.Draw(im,'RGBA')
 if scene=='disco':
  poly(d,[(0,196),(W,196),(W,H),(0,H)],(10,19,41,255))
  for n in range(11):
   d.line([(W//2,192),((n-2)*W/7,H)],fill=(72,215,240,96),width=2)
  for n in range(7):
   y=200+int((n/7)**1.5*100)
   d.line([(0,y),(W,y)],fill=(73,217,252,85),width=2)
  for k in [-1,1]:
   x=W//2+k*205
   poly(d,[(x-12,0),(x+12,0),(x+12,197),(x-12,197)],(18,17,51,255))
   for j in range(7):
    d.rounded_rectangle((x-8,17+j*25,x+8,30+j*25),radius=4,fill=((249,77,230,145) if j%2 else (67,229,229,145)))
 else:
  circ(d,W*.77-13*math.sin(t*.8),70,44,(255,220,148,220))
  poly(d,[(0,179),(80,138),(165,175),(266,130),(354,171),(W,125),(W,H),(0,H)],(37,116,146,210))
  for j in range(23):
   bx=j*29-20+5*math.sin(t*.2);y=170+(j*31)%36
   d.rectangle((bx,y,bx+22,231),fill=(44,64,96,220))
   for dy in [8,20]:
    for dx in [6,15]:
     d.rectangle((bx+dx,y+dy,bx+dx+3,y+dy+5),fill=(255,224,153,100))
  poly(d,[(0,226),(W,226),(W,H),(0,H)],(21,54,81,255))
  for j in range(6):d.line([(0,232+j*11),(W,232+j*11)],fill=(91,225,244,80),width=2)
 for j in range(4):
  x=37+j*136+math.sin(t*1.7+j)*33
  color=[(253,93,215,40),(83,235,255,45),(255,217,90,35),(255,255,255,30)][j]
  poly(d,[(x,0),(x+28,0),(x+116,218),(x-90,218)],color)
 for n in range(11):
  px=(n*87+19)%W+math.sin(t+n)*17;py=20+(n*51)%171+math.sin(t*1.2+n)*10
  circ(d,px,py,1+n%3,[(255,224,160,170),(255,131,230,146),(132,236,255,158)][n%3])
 if scene=='disco':
  dancer(im,t,((255,187,65,255),(255,231,126,255),(204,120,43,255)),(W*.55,138))
 else:
  dancer(im,t,((96,229,199,255),(177,255,231,255),(20,133,159,255)),(W*.5,141))
 return im.convert('RGB')
def generate(path,scene):
 proc=subprocess.Popen([ffmpeg_binary(),'-hide_banner','-loglevel','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-an','-vcodec','libx264','-preset','veryfast','-crf','26','-pix_fmt','yuv420p','-movflags','+faststart',str(path)],stdin=subprocess.PIPE)
 for i in range(FRAMES):proc.stdin.write(frame(scene,i).tobytes())
 proc.stdin.close()
 if proc.wait():raise RuntimeError(f'Cannot create background {scene}')
 if path.stat().st_size<10000:raise RuntimeError('Generated stage video is empty')
 print(scene,path.stat().st_size,'bytes, original silent 7-second animated video')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--output',default='app/src/main/assets/scenes');args=parser.parse_args()
 out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
 for scene in ('disco','sunset','spectrum','tunnel'):generate(out/f'{scene}.mp4',scene)
