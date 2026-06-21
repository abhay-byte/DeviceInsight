from PIL import Image, ImageDraw, ImageFont
import os
import textwrap

in_file = "/home/abhay/.gemini/antigravity-cli/brain/f4cab869-0e0f-465d-9f9a-40217348aa75/raw_feature_graphic_1782038196917.jpg"
out_file = "docs/storelisting/final_feature_graphic_16_9.jpg"

if not os.path.exists(in_file):
    print(f"Missing {in_file}")
    exit(1)

img = Image.open(in_file).convert("RGBA")
draw = ImageDraw.Draw(img)

try:
    logo = Image.open("assets/logo.webp").convert("RGBA")
except:
    logo = Image.new("RGBA", (150, 150), (255, 255, 255, 0))

font_path = "/usr/share/fonts/TTF/OpenSans-Bold.ttf"

logo_w, logo_h = logo.size
new_logo_w = 160
new_logo_h = int(160 * (logo_h / logo_w))
logo_resized = logo.resize((new_logo_w, new_logo_h), Image.LANCZOS)

# Same as promo settings
img.paste(logo_resized, (150, 200), logo_resized)

try:
    font_title = ImageFont.truetype(font_path, 80)
    font_sub = ImageFont.truetype(font_path, 45)
except:
    font_title = ImageFont.load_default()
    font_sub = ImageFont.load_default()

shadow = 4

title = "Device Insights"
subtitle = "The Ultimate Android System Monitor."

# Wrap title
title_lines = textwrap.wrap(title, width=16)
y = 200 + new_logo_h + 40
for line in title_lines:
    draw.text((150+shadow, y+shadow), line, font=font_title, fill="black")
    draw.text((150, y), line, font=font_title, fill="white")
    y += 90

y += 20
# Wrap subtitle
sub_lines = textwrap.wrap(subtitle, width=22)
for line in sub_lines:
    draw.text((150+shadow, y+shadow), line, font=font_sub, fill="black")
    draw.text((150, y), line, font=font_sub, fill="#E0E0E0")
    y += 55

img = img.convert("RGB")
img.save(out_file, quality=95)
print(f"Saved {out_file}")

