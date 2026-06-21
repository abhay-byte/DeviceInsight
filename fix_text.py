from PIL import Image, ImageDraw, ImageFont
import os
import textwrap

images = [
    {
        "file": "docs/storelisting/clean_promo_1.jpg",
        "out": "docs/storelisting/final_promo_1.jpg",
        "title": "Comprehensive Dashboard",
        "subtitle": "Monitor everything at a glance.",
        "type": "promo"
    },
    {
        "file": "docs/storelisting/clean_promo_2.jpg",
        "out": "docs/storelisting/final_promo_2.jpg",
        "title": "Advanced Task Manager",
        "subtitle": "Take control of your applications.",
        "type": "promo"
    },
    {
        "file": "docs/storelisting/clean_promo_3.jpg",
        "out": "docs/storelisting/final_promo_3.jpg",
        "title": "Deep Hardware Insights",
        "subtitle": "Uncover your device's true capabilities.",
        "type": "promo"
    },
    {
        "file": "docs/storelisting/clean_promo_4.jpg",
        "out": "docs/storelisting/final_promo_4.jpg",
        "title": "Real-Time Floating HUD",
        "subtitle": "Track performance over any app.",
        "type": "promo"
    },
    {
        "file": "docs/storelisting/feature_graphic_1024x500.jpg",
        "out": "docs/storelisting/final_feature_graphic.jpg",
        "title": "Device Insights",
        "subtitle": "The Ultimate Android System Monitor",
        "type": "feature"
    }
]

try:
    logo = Image.open("assets/logo.webp").convert("RGBA")
except:
    logo = Image.new("RGBA", (150, 150), (255, 255, 255, 0))

font_path = "/usr/share/fonts/TTF/OpenSans-Bold.ttf"

for item in images:
    if not os.path.exists(item["file"]):
        print(f"Skipping {item['file']}")
        continue
    
    img = Image.open(item["file"]).convert("RGBA")
    draw = ImageDraw.Draw(img)
    
    if item["type"] == "promo":
        # 1920x1080 layout
        # Place logo on left center-top
        logo_w, logo_h = logo.size
        new_logo_w = 160
        new_logo_h = int(160 * (logo_h / logo_w))
        logo_resized = logo.resize((new_logo_w, new_logo_h), Image.LANCZOS)
        
        img.paste(logo_resized, (150, 200), logo_resized)
        
        try:
            font_title = ImageFont.truetype(font_path, 80)
            font_sub = ImageFont.truetype(font_path, 45)
        except:
            font_title = ImageFont.load_default()
            font_sub = ImageFont.load_default()
            
        shadow = 4
        
        # Wrap title
        title_lines = textwrap.wrap(item["title"], width=16)
        y = 200 + new_logo_h + 40
        for line in title_lines:
            draw.text((150+shadow, y+shadow), line, font=font_title, fill="black")
            draw.text((150, y), line, font=font_title, fill="white")
            y += 90
            
        y += 20
        # Wrap subtitle
        sub_lines = textwrap.wrap(item["subtitle"], width=22)
        for line in sub_lines:
            draw.text((150+shadow, y+shadow), line, font=font_sub, fill="black")
            draw.text((150, y), line, font=font_sub, fill="#E0E0E0")
            y += 55

    else:
        # Feature graphic 1024x500 layout
        # Place logo top centerish or top left
        logo_w, logo_h = logo.size
        new_logo_w = 90
        new_logo_h = int(90 * (logo_h / logo_w))
        logo_resized = logo.resize((new_logo_w, new_logo_h), Image.LANCZOS)
        
        # Let's put everything top-left with a strong dark gradient behind it if possible, 
        # or just strong shadow.
        img.paste(logo_resized, (50, 40), logo_resized)
        
        try:
            font_title = ImageFont.truetype(font_path, 45)
            font_sub = ImageFont.truetype(font_path, 25)
        except:
            font_title = ImageFont.load_default()
            font_sub = ImageFont.load_default()
            
        shadow = 3
        draw.text((160+shadow, 50+shadow), item["title"], font=font_title, fill="black")
        draw.text((160, 50), item["title"], font=font_title, fill="white")
        
        draw.text((160+shadow, 105+shadow), item["subtitle"], font=font_sub, fill="black")
        draw.text((160, 105), item["subtitle"], font=font_sub, fill="#E0E0E0")

    img = img.convert("RGB")
    img.save(item["out"], quality=95)
    print(f"Saved {item['out']}")

