#!/usr/bin/env python3
"""Generate eight original MIDI/KAR-style practice songs for a KantaGo starter pack.
These compositions and short lyric cues are original, NOT chart-song downloads.
"""
from pathlib import Path
import math
import random
import struct

TRACKS = [
    ("Island Sunrise", 106, 0, 60, [0, 2, 4, 7, 9], ["The new sun is rising", "Our island shines again", "Sing along together", "Feel the morning breeze"]),
    ("Fiesta sa Barangay", 138, 24, 62, [0, 2, 5, 7, 9], ["Sayaw ta karon", "Kuyog sa musika", "Fiesta sa atong lugar", "Kanta na barkada"]),
    ("Rain Over Davao", 92, 4, 57, [0, 2, 3, 7, 10], ["Rain falls on the city", "Lights shine on the road", "We keep every memory", "Till the skies turn gold"]),
    ("Weekend Barkada", 125, 0, 60, [0, 2, 4, 7, 11], ["It's a weekend night", "Friends are here to stay", "Keep the music playing", "Sing until the day"]),
    ("Dreaming Tonight", 84, 10, 55, [0, 3, 5, 7, 10], ["Close your eyes and listen", "Stars are in the sky", "Every little dream", "Has a chance to fly"]),
    ("Sunset By the Bay", 112, 25, 60, [0, 2, 5, 7, 9], ["There is light on the water", "Colors softly glow", "Music in the sunset", "Where the warm winds blow"]),
    ("Happy Little Home", 118, 11, 65, [0, 2, 4, 7, 9], ["Welcome to our home", "Laughter fills the air", "When we sing together", "Joy is everywhere"]),
    ("Starlight Parade", 146, 62, 64, [0, 2, 4, 7, 11], ["Follow little stars", "Dancing through the night", "Raise your voice and sing", "Everything is bright"]),
]

def variable(value):
    result=[value & 127]
    value >>= 7
    while value:
        result.insert(0,(value & 127)|128)
        value >>= 7
    return bytes(result)

def midi_file(name, bpm, instrument, base, scale, lines, seed):
    rng=random.Random(seed)
    ticks=480
    events=[]
    def add(t, p, data):
        events.append((t,p,data))
    add(0,0,b"\xff\x51\x03"+int(60000000/bpm).to_bytes(3,'big'))
    add(0,0,b"\xc0"+bytes([instrument]))
    add(0,0,b"\xc1\x20") # soft bass
    add(0,0,b"\xc2\x30") # string chord
    for bar in range(24):
        start=bar*4*ticks
        chord=([0,3,4,3],[0,4,2,3],[0,3,1,4])[seed%3][bar%4]
        root=base+scale[chord%len(scale)]
        for note in (root-12,root-5,root+4):
            add(start,2,bytes([0x92,note,34]))
            add(start+4*ticks-30,1,bytes([0x82,note,0]))
        for beat in range(4):
            t=start+beat*ticks
            bass=root-24 + (7 if beat%2 else 0)
            add(t,2,bytes([0x91,bass,54]))
            add(t+ticks-25,1,bytes([0x81,bass,0]))
            ix=(bar*3 + beat + rng.randint(0,2))%len(scale)
            melody=base+12+scale[ix]-(12 if bar%7==0 and beat==0 else 0)
            add(t,2,bytes([0x90,melody,80]))
            add(t+int(ticks*.83),1,bytes([0x80,melody,0]))
        if bar%2==0:
            line=lines[(bar//2)%len(lines)]
            # KAR lyric newline markers: a slash denotes a fresh line
            lyric=("/"+line).encode("ascii")
            add(start,0,b"\xff\x05"+variable(len(lyric))+lyric)
    end=24*4*ticks+1
    add(end,9,b"\xff\x2f\x00")
    events.sort(key=lambda ev:(ev[0],ev[1]))
    timeline=bytearray()
    last=0
    for t,_,event in events:
        timeline.extend(variable(t-last))
        timeline.extend(event)
        last=t
    header=b"MThd"+struct.pack(">IHHH",6,0,1,ticks)
    return header+b"MTrk"+struct.pack(">I",len(timeline))+timeline

def main(path):
    target=Path(path)
    target.mkdir(parents=True,exist_ok=True)
    for i,(title,bpm,instrument,base,scale,lines) in enumerate(TRACKS):
        (target/("KantaGo - "+title+".kar")).write_bytes(midi_file(title,bpm,instrument,base,scale,lines,i+13))
    (target/"ABOUT.txt").write_text(
        "KantaGo Starter Collection\n"
        "Eight original instrumental karaoke practice tracks with short lyric prompts.\n"
        "No third-party chart songs, copyrighted backing tracks, or pirated material are included.\n",
        encoding="utf-8")
    print("Generated",len(TRACKS),"original MIDI karaoke practice songs in",target)

if __name__=="__main__":
    import argparse
    parser=argparse.ArgumentParser()
    parser.add_argument("--output",required=True)
    args=parser.parse_args()
    main(args.output)
