"""Original 32-beat test melody and lyric cues, authored for KantaGo; CC0."""
import struct
from pathlib import Path

def vlq(n):
    b=[n&127];n>>=7
    while n:b.insert(0,(n&127)|128);n>>=7
    return bytes(b)
events=[(0,b'\xff\x51\x03\x07\xa1\x20'),(0,b'\xc0\x00')]
notes=[60,64,67,72,71,67,64,62,60,62,64,67,69,67,64,60,65,69,72,74,72,69,65,64,62,64,67,71,72,67,64,60]
words=['/Welcome ','to ','Kanta','Go! ','/Pick ','a ','song ','and sing. ','/Turn ','the ','music ','up. ','/Let ','your ','voice ','ring. ','/Friends ','and ','family ','here. ','/Share ','a ','happy ','song. ','/This ','is ','our ','sound check. ','/Come ','and ','sing ','along!']
for i,(note,word) in enumerate(zip(notes,words)):
    tick=i*480
    events.append((tick,b'\xff\x05'+vlq(len(word.encode()))+word.encode()))
    events.append((tick,bytes([0x90,note,88])))
    events.append((tick+400,bytes([0x80,note,0])))
    if i%4==0:
        for n in [48+(i//8)%3,55]:
            events.append((tick,bytes([0x91,n,52])))
            events.append((tick+1700,bytes([0x81,n,0])))
events.append((32*480,b'\xff\x2f\x00'))
track=b'';last=0
for t,e in sorted(events,key=lambda x:x[0]):track+=vlq(t-last)+e;last=t
Path('app/src/main/assets/demo.mid').write_bytes(b'MThd'+struct.pack('>IHHH',6,0,1,480)+b'MTrk'+struct.pack('>I',len(track))+track)
