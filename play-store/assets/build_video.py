"""Assemble the SkyPulse promo video from scene frames + Aria voiceover.
Per scene: subtle Ken-Burns zoom on the still, VO delayed slightly, then concat
with fade in/out. Pass --music to mix a soft ambient air bed under the narration."""
import subprocess, os, sys, json
from scenes import SCENES

V = r"h:\flight-tracker\play-store\video"
SCN = rf"{V}\scenes"; VO = rf"{V}\voiceover"; TMP = rf"{V}\_clips"
os.makedirs(TMP, exist_ok=True)
FPS = 30; LEAD = 0.45; TAIL = 0.75

def dur(path):
    out = subprocess.check_output(["ffprobe","-v","error","-show_entries",
        "format=duration","-of","csv=p=0", path], text=True)
    return float(out.strip())

def run(cmd):
    r = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if r.returncode != 0:
        print(r.stdout[-2500:]); raise SystemExit(f"ffmpeg failed: {cmd[:3]}")

clips = []; total = 0.0
for s in SCENES:
    i = s["id"]
    vod = dur(rf"{VO}\vo_{i:02d}.mp3")
    cd = round(LEAD + vod + TAIL, 3)
    frames = int(round(cd * FPS))
    lead_ms = int(LEAD * 1000)
    img = rf"{SCN}\scene_{i:02d}.png"
    vo = rf"{VO}\vo_{i:02d}.mp3"
    clip = rf"{TMP}\clip_{i:02d}.mp4"
    vf = (f"scale=2400:1350,zoompan=z='min(zoom+0.00020,1.085)':d={frames}"
          f":x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps={FPS},"
          f"trim=duration={cd},setsar=1,format=yuv420p")
    af = f"adelay={lead_ms}|{lead_ms},apad,atrim=duration={cd},aresample=48000"
    run(["ffmpeg","-y","-loop","1","-i",img,"-i",vo,
         "-filter_complex",f"[0:v]{vf}[v];[1:a]{af}[a]",
         "-map","[v]","-map","[a]","-t",str(cd),
         "-c:v","libx264","-preset","medium","-crf","19","-pix_fmt","yuv420p",
         "-r",str(FPS),"-c:a","aac","-b:a","192k","-ar","48000", clip])
    clips.append(clip); total += cd
    print(f"clip {i}: {cd}s ({frames}f)")

# concat
lst = rf"{TMP}\list.txt"
with open(lst,"w") as f:
    for c in clips: f.write(f"file '{c}'\n")
joined = rf"{TMP}\joined.mp4"
run(["ffmpeg","-y","-f","concat","-safe","0","-i",lst,"-c","copy",joined])

total = round(total, 2)
out = rf"{V}\skypulse-promo.mp4"
vfade = f"fade=t=in:st=0:d=0.6,fade=t=out:st={total-0.7:.2f}:d=0.7"
if "--music" in sys.argv:
    fc = (f"anoisesrc=color=brown:amplitude=0.85:duration={total}[n];"
          f"[n]highpass=f=90,lowpass=f=300,tremolo=f=0.12:d=0.7,volume=0.05[bed];"
          f"[0:a]volume=1.0[vo];[vo][bed]amix=inputs=2:normalize=0[a];"
          f"[0:v]{vfade}[v]")
    run(["ffmpeg","-y","-i",joined,"-filter_complex",fc,
         "-map","[v]","-map","[a]","-c:v","libx264","-preset","slow","-crf","18",
         "-pix_fmt","yuv420p","-c:a","aac","-b:a","192k", out])
else:
    run(["ffmpeg","-y","-i",joined,
         "-vf",vfade,"-af",f"afade=t=in:d=0.5,afade=t=out:st={total-0.7:.2f}:d=0.7",
         "-c:v","libx264","-preset","slow","-crf","18","-pix_fmt","yuv420p",
         "-c:a","aac","-b:a","192k", out])
print("TOTAL", total, "->", out)
