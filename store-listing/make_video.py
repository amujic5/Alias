#!/usr/bin/env python3
import os, subprocess
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FF = "/Users/azzi/work/AliasML/.venv/lib/python3.13/site-packages/imageio_ffmpeg/binaries/ffmpeg-macos-aarch64-v7.1"
OUT = "/tmp/aliasshots/out"
SEG = "/tmp/aliasshots/seg"
os.makedirs(SEG, exist_ok=True)
FONT = "/Users/azzi/work/AliasAndroid_new/app/src/main/res/font/baloo2.ttf"
TOP=(0x3A,0xA0,0xF5); BOT=(0x0F,0x70,0xD1)
CONF=[(0xFF,0x5C,0x8A),(0xFF,0x8A,0x3D),(0x4C,0xD9,0x64),(0x9B,0x6D,0xFF),(0x28,0xD9,0xC4),(0xFF,0xD2,0x3D)]

def fnt(sz,w=800):
    f=ImageFont.truetype(FONT,sz)
    try: f.set_variation_by_axes([w])
    except: pass
    return f

def grad(w,h):
    im=Image.new("RGB",(w,h)); px=im.load()
    for y in range(h):
        t=y/(h-1); px_r=(int(TOP[0]+(BOT[0]-TOP[0])*t),int(TOP[1]+(BOT[1]-TOP[1])*t),int(TOP[2]+(BOT[2]-TOP[2])*t))
        for x in range(w): px[x,y]=px_r
    return im

def confetti(img,spots):
    for cx,cy,sz,ci,rot in spots:
        sq=Image.new("RGBA",(sz,sz),(0,0,0,0))
        ImageDraw.Draw(sq).rounded_rectangle([0,0,sz-1,sz-1],radius=int(sz*0.28),fill=CONF[ci]+(235,))
        sq=sq.rotate(rot,expand=True,resample=Image.BICUBIC); img.paste(sq,(cx,cy),sq)

def center(d,txt,f,y,w,fill="white"):
    tw=d.textlength(txt,font=f); d.text(((w-tw)/2,y),txt,font=f,fill=fill); return tw

def intro_card():
    W,H=1080,1920; c=grad(W,H).convert("RGBA")
    confetti(c,[(120,360,150,0,12),(840,420,120,5,-10),(150,1380,130,2,8),(840,1440,110,3,-12),(500,250,90,4,10)])
    d=ImageDraw.Draw(c)
    center(d,"ALIAS",fnt(150,800),760,W); center(d,"WORDS",fnt(150,800),930,W)
    center(d,"The party word game",fnt(52,700),1180,W,(255,255,255,235))
    c.convert("RGB").save(f"{SEG}/intro.png");

def outro_card():
    W,H=1080,1920; c=grad(W,H).convert("RGBA")
    confetti(c,[(120,360,150,3,12),(840,420,120,0,-10),(150,1380,130,5,8),(840,1440,110,2,-12)])
    d=ImageDraw.Draw(c)
    center(d,"ALIAS",fnt(140,800),700,W); center(d,"WORDS",fnt(140,800),860,W)
    # pill
    pill=fnt(46,800); label="NOW WITH AI"; tw=d.textlength(label,font=pill); pad=30
    x0=(W-(tw+pad*2))/2
    d.rounded_rectangle([x0,1080,x0+tw+pad*2,1080+86],radius=43,fill=(0xFF,0xD2,0x3D))
    d.text((x0+pad,1096),label,font=pill,fill=(0x0E,0x17,0x26))
    center(d,"Free · Offline · Endless fun",fnt(46,700),1240,W,(255,255,255,235))
    c.convert("RGB").save(f"{SEG}/outro.png")

intro_card(); outro_card()

# (image, duration, pan-direction)  dir: 0 TL->BR, 1 TR->BL, 2 center-zoomish(use TL->BR small)
TL=f"{OUT}"
SHOTS=[
 (f"{SEG}/intro.png",2.8,0),
 (f"{TL}/01_home.png",3.0,0),
 (f"{TL}/02_teams.png",3.0,1),
 (f"{TL}/04_aiexplain.png",3.2,0),
 (f"{TL}/05_challenge.png",3.2,1),
 (f"{TL}/06_decks.png",3.0,0),
 (f"{SEG}/outro.png",3.2,1),
]
FPS=30; W,H=1080,1920; SW,SH=1188,2112  # 1.1x for pan room

segfiles=[]
for i,(img,dur,dr) in enumerate(SHOTS):
    out=f"{SEG}/s{i:02d}.mp4"; segfiles.append((out,dur))
    if dr==0: xexpr=f"(in_w-out_w)*t/{dur}"; yexpr=f"(in_h-out_h)*t/{dur}"
    else:     xexpr=f"(in_w-out_w)*(1-t/{dur})"; yexpr=f"(in_h-out_h)*t/{dur}"
    vf=f"scale={SW}:{SH},crop={W}:{H}:'{xexpr}':'{yexpr}',format=yuv420p"
    cmd=[FF,"-y","-loop","1","-framerate",str(FPS),"-t",f"{dur}","-i",img,"-vf",vf,
         "-c:v","libx264","-preset","medium","-crf","20","-pix_fmt","yuv420p","-r",str(FPS),out]
    r=subprocess.run(cmd,capture_output=True,text=True)
    if r.returncode!=0: print("SEG ERR",i,r.stderr[-600:]); raise SystemExit(1)
print("segments ok")

# xfade chain
C=0.5
inputs=[]
for f,_ in segfiles: inputs+=["-i",f]
fc=[]; acc=segfiles[0][1]; last="0:v"
for i in range(1,len(segfiles)):
    off=acc-C; lbl=f"x{i}"
    fc.append(f"[{last}][{i}:v]xfade=transition=fade:duration={C}:offset={off:.3f}[{lbl}]")
    last=lbl; acc=acc+segfiles[i][1]-C
filtc=";".join(fc)
cmd=[FF,"-y"]+inputs+["-filter_complex",filtc,"-map",f"[{last}]","-c:v","libx264","-preset","medium","-crf","20","-pix_fmt","yuv420p","-r",str(FPS),"/tmp/aliasshots/out/promo.mp4"]
r=subprocess.run(cmd,capture_output=True,text=True)
if r.returncode!=0: print("XFADE ERR",r.stderr[-1200:]); raise SystemExit(1)
print("DONE promo.mp4  total~",round(acc,1),"s")
