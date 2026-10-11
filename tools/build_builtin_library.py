#!/usr/bin/env python3
"""Build the offline karaoke library bundled inside KantaGo's APK.

All music added here is either original for KantaGo or a newly created
MIDI arrangement of a historical public-domain melody. No music from external
commercial karaoke sites is copied. The app uses local MIDI synthesis.
"""
from __future__ import annotations
import argparse, json, math, random, re, struct
from pathlib import Path
from create_original_demos import TRACKS as STARTER, midi_file

# Distinct original practice songs. Each includes vocals-on-screen lyrics and
# keyboard/bass/accompaniment MIDI. No commercial melody is sampled.
# title | language | style | four original lyric lines
ORIGINALS = [
("City Lights Again","English","pop","City lights are calling me|Underneath the open sky|Every road can lead us home|Sing our little song tonight"),
("Open Road Tonight","English","rock","On the open road tonight|I can see the morning glow|Every mile becomes a memory|Come along and take it slow"),
("Golden Afternoon","English","ballad","In the golden afternoon|All the windows fill with light|We can hear the gentle music|As the day becomes the night"),
("Little Reasons to Smile","English","folk","There are little things to love|In the quiet of the day|When the world is feeling heavy|One sweet song can light the way"),
("Riverboat Dream","English","jazz","Down the river we will go|With the stars above the stream|Hear the water keep the rhythm|In our little riverboat dream"),
("The Road Home","English","country","When the long day comes to end|And the road is running wide|I can hear the home lights shining|Like a song I keep inside"),
("Stars Above Us","English","waltz","All the stars above us shine|Like a lantern in the blue|Every wish becomes a chorus|When I sing along with you"),
("One More Sunrise","English","pop","One more sunrise paints the sky|We can start a brand new day|Leave the shadows far behind us|Let the music lead the way"),
("Weekend Sky","English","dance","It's another weekend sky|Hear the laughter in the air|Turn the music up together|There is happiness to share"),
("Paper Lanterns","English","folk","Paper lanterns in the breeze|Floating gently down the lane|All our hopes are little lights|Shining through the summer rain"),
("Blue Ocean Waves","English","island","Blue ocean waves beside me|Gentle winds across the sand|Every tide will bring a story|From this beautiful island"),
("Sing With Friends","English","pop","Sing with friends until the dawn|Keep the happy melody|Every voice can be a light|In our little harmony"),
("Sa Bawat Umaga","OPM","ballad","Sa bawat umaga may pag-asa|Unti-unting sisikat ang araw|Ang ating awit ay magkasama|Sa bagong buhay na tanaw"),
("Bituing Gabay","OPM","waltz","May bituing gabay sa gabi|Kumikislap sa aking daan|Ang munting pangarap na tangan|Aawitin nang dahan-dahan"),
("Kuwento ng Bayan","OPM","folk","Ito ang kuwento ng bayan|May tawanan at kasiyahan|Kapag tayo ay nagkakaisa|May awit sa bawat tahanan"),
("Liwanag sa Daan","OPM","pop","May liwanag sa ating daan|May pag-asa sa bawat hakbang|Sa ilalim ng kalangitan|Sabay nating aawitan"),
("Tayo ay Kakanta","OPM","dance","Tayo ay kakanta ngayong gabi|Kasama ang buong barkada|Ang saya ay ating ibahagi|Sa musika at pagsasama"),
("Buhay na Masaya","OPM","island","Buhay na masaya ang hangad|Dagat at araw ang kasama|Sa bawat sandali ng buhay|May awit na hindi mawawala"),
("Sa Piling ng Kaibigan","OPM","ballad","Sa piling ng kaibigan ko|May mga kuwento at tawa|Kahit dumating man ang ulan|Ang awit ay laging kasama"),
("Awit ng Ating Tahanan","OPM","folk","Ito ang awit ng tahanan|Sa hapag at sa bakuran|May pagmamahal sa bawat isa|At ligayang walang hanggan"),
("Kanta Ta","Bisaya","dance","Kanta ta ug sayaw karon|Malipayong adlaw sa tanan|Dungan ta sa atong awit|Ug katawa sa kabarkada"),
("Adlaw sa Paglaum","Bisaya","ballad","Adlaw sa paglaum moabot|Kahayag sa atong dalan|Dili kita mag-inusara|Mag-uban sa kalipay"),
("Balay nga Malipayon","Bisaya","folk","Sa among balay adunay awit|Adunay katawa ug gugma|Malipayon ang matag adlaw|Kung mag-uban ang pamilya"),
("Higala sa Kanunay","Bisaya","pop","Higala sa kanunay kauban|Sa kalipay ug kasubo|Ang atong awit magpadayon|Sa mga adlaw nga moabot"),
("Kahayag sa Buntag","Bisaya","waltz","Kahayag sa buntag misidlak|Bag-ong adlaw atong sugdan|Ang hangin nagdala ug awit|Sa paglaum nga walay katapusan"),
("Dungan Ta","Bisaya","rock","Dungan ta sa matag kanta|Dungan ta sa pagsayaw|Sa tibuok nga gabii karon|Ang kalipay dili mawala"),
("Dapit sa Dagat","Bisaya","island","Sa dapit sa dagat mag-awit|Maminaw sa balod ug hangin|Adunay kahayag sa adlaw|Ug kalinaw sa atong kasingkasing"),
("Lipay sa Kasingkasing","Bisaya","ballad","Lipay sa kasingkasing karon|Sa tingog sa atong musika|Mag-uban kita sa dalan|Hangtod sa kabuntagon"),
("Moonlight Fiesta","Other","latin","Moonlight fiesta fills the square|Hear the bright guitars tonight|Every little step is dancing|Underneath the silver light"),
("Summer Mambo","Other","latin","Summer mambo by the shore|Rhythm dancing in the street|Every heart can feel the music|Every foot can find the beat"),
("Starlit Boulevard","Other","jazz","On a quiet boulevard|Hear the piano play so low|Every corner has a story|Every light a gentle glow"),
("The Midnight Waltz","Other","waltz","Let the midnight waltz begin|As the silver hours pass|All the stars are moving slowly|Like reflections on the glass"),
]

STYLE = {
 "pop":      (112,0,[0,2,4,5,7,9,11]),
 "rock":     (136,29,[0,2,3,5,7,10]),
 "ballad":   (82,0,[0,2,4,7,9]),
 "folk":     (108,24,[0,2,4,5,7,9]),
 "waltz":    (96,11,[0,2,4,5,7,9]),
 "dance":    (142,80,[0,2,3,5,7,10]),
 "island":   (115,25,[0,2,4,5,7,9]),
 "jazz":     (104,4,[0,2,3,5,7,10]),
 "country":  (120,25,[0,2,4,5,7,9]),
 "latin":    (130,24,[0,2,4,5,7,9,11]),
}

# Public-domain melodies newly arranged for this app (scale degrees in C).
# Lyrics here are historic/common traditional verses, not modern recordings.
# A 0.5 beat rest is represented by -1. Every note is a quarter note otherwise.
TRADITIONALS=[
 ("Twinkle Twinkle Little Star","C C G G A A G F F E E D D C G G F F E E D G G F F E E D C C G G A A G F F E E D D C","Twinkle twinkle little star|How I wonder what you are|Up above the world so high|Like a diamond in the sky"),
 ("Mary Had a Little Lamb","E D C D E E E D D D E G G E D C D E E E E D D E D C","Mary had a little lamb|Its fleece was white as snow|And everywhere that Mary went|The lamb was sure to go"),
 ("Frere Jacques","C D E C C D E C E F G E F G G A G F E C G A G F E C C G C C G C","Frere Jacques, Frere Jacques|Dormez-vous, dormez-vous|Sonnez les matines|Din, din, don, din, din, don"),
 ("Row Row Row Your Boat","C C C D E E D E F G C5 C5 C5 G G G E E E C C C G F E D C","Row row row your boat|Gently down the stream|Merrily merrily merrily merrily|Life is but a dream"),
 ("London Bridge","G A G F E F G D E F E F G G A G F E F G D G E C","London Bridge is falling down|Falling down, falling down|London Bridge is falling down|My fair lady"),
 ("Ode to Joy","E E F G G F E D C C D E E D D E E F G G F E D C C D E D C C","Joyful music fills the air|Come and sing with all your friends|Hearts together find their rhythm|Let the song go on and on"),
 ("Jingle Bells","E E E E E E E G C D E F F F F F E E E E D D E D G","Jingle bells, jingle bells|Jingle all the way|Oh what fun it is to ride|In a one horse open sleigh"),
 ("Old MacDonald Had a Farm","G G G D E E D B B A A G D G G G D E E D B B A A G","Old MacDonald had a farm|E I E I O|And on his farm he had a cow|E I E I O"),
]
SCALE_NOTES={"C":60,"D":62,"E":64,"F":65,"G":67,"A":69,"B":71,"C5":72}
def vlq(v):
 buf=[v&127];v//=128
 while v:
  buf.insert(0,(v&127)|128);v//=128
 return bytes(buf)
def classic_midi(name,seq,lines,tempo=108):
 notes=[SCALE_NOTES[n] for n in seq.split()]
 # two melodic passes, with accompaniment; 4/4 480 ticks/quarter.
 events=[]
 def event(t,order,payload):events.append((t,order,payload))
 event(0,0,b'\xff\x51\x03'+int(60000000/tempo).to_bytes(3,'big'))
 event(0,0,b'\xc0\x0a')  # music box melody
 event(0,0,b'\xc1\x18')  # acoustic guitar accompaniment
 event(0,0,b'\xc2\x20')  # bass
 ticks=480
 for loop in range(3):
  offset=loop*len(notes)*ticks
  for i,pitch in enumerate(notes):
   t=offset+i*ticks
   event(t,2,bytes([0x90,pitch,88]))
   event(t+int(ticks*.85),1,bytes([0x80,pitch,0]))
   if i%4==0:
    root=48 if ((i//4)%4)<2 else 53
    for chord in (root,root+4,root+7):
     event(t,2,bytes([0x91,chord,34]))
     event(t+ticks*4-28,1,bytes([0x81,chord,0]))
    event(t,2,bytes([0x92,root-12,55]))
    event(t+ticks*3-28,1,bytes([0x82,root-12,0]))
  for j,line in enumerate(lines):
   at=offset+min(j*(len(notes)//len(lines)),len(notes)-1)*ticks
   words=line.split()
   line_ticks=max(ticks, (len(notes)//len(lines))*ticks)
   for wi,word in enumerate(words):
    part=(('/' if wi==0 else '')+word+(' ' if wi<len(words)-1 else '')).encode('utf-8')
    lyric_at=at+int(wi*line_ticks/max(1,len(words)))
    event(lyric_at,0,b'\xff\x05'+vlq(len(part))+part)
 end=3*len(notes)*ticks
 event(end,9,b'\xff\x2f\x00')
 events.sort(key=lambda x:(x[0],x[1]))
 track=bytearray();last=0
 for tick,_,msg in events:
  track.extend(vlq(tick-last));track.extend(msg);last=tick
 return b'MThd'+struct.pack('>IHHH',6,0,1,ticks)+b'MTrk'+struct.pack('>I',len(track))+track

def main(dest):
 target=Path(dest);target.mkdir(parents=True,exist_ok=True)
 records=[]
 def add(song_id,title,artist,language,data,kind="original"):
  filename=f"kb-{song_id:06d}.kar"
  (target/filename).write_bytes(data)
  records.append({"id":f"{song_id:06d}","title":title,"artist":artist,
      "language":language,"year":2026 if kind=="original" else 0,
      "file":filename,"favorite":False,"source":"builtin",
      "rights":"Original composition by KantaGo" if kind=="original" else "Traditional/public-domain tune, newly arranged by KantaGo"})
 number=700001
 for i,(title,bpm,program,base,scale,lyrics) in enumerate(STARTER):
  add(number,title,"KantaGo Original","Bisaya" if i==1 else "English",midi_file(title,bpm,program,base,scale,lyrics,i+13));number+=1
 for i,(title,language,genre,lyricstr) in enumerate(ORIGINALS):
  bpm,program,scale=STYLE[genre]
  data=midi_file(title,bpm+(i%5-2)*2,program,55+(i*3)%10,scale,lyricstr.split("|"),i+101)
  add(number,title,"KantaGo Original",language,data);number+=1
 for title,notes,lyrics in TRADITIONALS:
  add(number,title,"Traditional / KantaGo Arrangement","English",classic_midi(title,notes,lyrics.split("|")),"traditional");number+=1
 (target/"catalog.json").write_text(json.dumps(records,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
 print(f"Bundled {len(records)} playable karaoke MIDIs ({len(STARTER)+len(ORIGINALS)} original, {len(TRADITIONALS)} traditional); {sum(f.stat().st_size for f in target.glob('*.kar'))/1024:.0f} KiB")
 assert len(records)==48
if __name__=='__main__':
 parser=argparse.ArgumentParser()
 parser.add_argument('--output',default='app/src/main/assets/builtin')
 main(parser.parse_args().output)
