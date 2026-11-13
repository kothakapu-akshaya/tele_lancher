#!/usr/bin/env python3
"""Programmatic tutorial video renderer (slides + ffmpeg)."""
import os, subprocess
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))
SLIDES = os.path.join(OUT, "slides")
os.makedirs(SLIDES, exist_ok=True)

TITLE_FONT = None
try:
    TITLE_FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 48)
    BODY_FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 28)
except Exception:
    TITLE_FONT = ImageFont.load_default()
    BODY_FONT = ImageFont.load_default()

SLIDE_TEXTS = [
    ("Tele Launcher - Tutorial", "Project introduction\nAndroid TV launcher, TMDB integration, app shelf, watchlist, search."),
    ("Architecture", "Kotlin + Jetpack Compose\nMainActivity, HomeScreen, data layer (TmdbApi/Repository), SharedPreferences."),
    ("Audit & Classification", "PLACEHOLDER: no audit/classification in this repo; see droidshield."),
    ("Debloater & Hardening", "PLACEHOLDER: no debloater in this repo; tools/tv-debloater is separate."),
    ("Benchmarking", "PLACEHOLDER: no benchmarks recorded in this repo."),
    ("Launcher & Indic Locales", "Launcher UI present. Indic locale resources: MISSING (values-hi/te not present)."),
    ("Backup / Restore / Rollback", "PLACEHOLDER: no rollback implementation in this repo."),
    ("Testing & Limitations", "Unit/instrumentation tests: NavigationTest.\nHW validation: UNVERIFIED (emulator only)."),
]

def make_slide(i, title, body):
    img = Image.new("RGB", (1280, 720), (20, 24, 36))
    d = ImageDraw.Draw(img)
    d.text((60, 80), title, font=TITLE_FONT, fill=(255,255,255))
    y = 200
    for line in body.split("\n"):
        d.text((60, y), line, font=BODY_FONT, fill=(200,210,220))
        y += 40
    path = os.path.join(SLIDES, f"slide_{i:02d}.png")
    img.save(path)
    return path

def main():
    for i,(t,b) in enumerate(SLIDE_TEXTS):
        make_slide(i,t,b)
    # concat via ffmpeg
    listfile = os.path.join(SLIDES, "list.txt")
    with open(listfile,"w") as f:
        for i in range(len(SLIDE_TEXTS)):
            f.write(f"file 'slide_{i:02d}.png'\n")
            f.write("duration 3\n")
    out = os.path.join(OUT, "tele_lancher_tutorial.mp4")
    subprocess.run(["ffmpeg","-y","-f","concat","-safe","0","-i",listfile,"-vf","format=yuv420p","-r","30",out], check=True)
    print("Rendered", out)

if __name__ == "__main__":
    main()
