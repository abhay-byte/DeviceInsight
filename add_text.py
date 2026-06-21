from PIL import Image, ImageDraw, ImageFont
import os

images = [
    {
        "file": "docs/storelisting/clean_promo_1.jpg",
        "out": "docs/storelisting/final_promo_1.jpg",
        "title": "Comprehensive Dashboard",
        "subtitle": "Monitor everything at a glance",
        "logo_pos": (100, 100),
        "text_pos": (280, 100)
    },
    {
        "file": "docs/storelisting/clean_promo_2.jpg",
        "out": "docs/storelisting/final_promo_2.jpg",
        "title": "Advanced Task Manager",
        "subtitle": "Take control of your applications",
        "logo_pos": (100, 100),
        "text_pos": (280, 100)
    },
    {
        "file": "docs/storelisting/clean_promo_3.jpg",
        "out": "docs/storelisting/final_promo_3.jpg",
        "title": "Deep Hardware Insights",
        "subtitle": "Uncover your device's true capabilities",
        "logo_pos": (100, 100),
        "text_pos": (280, 100)
    },
    {
        "file": "docs/storelisting/clean_promo_4.jpg",
        "out": "docs/storelisting/final_promo_4.jpg",
        "title": "Real-Time Floating HUD",
        "subtitle": "Track performance over any app",
        "logo_pos": (100, 100),
        "text_pos": (280, 100)
    },
    {
        "file": "docs/storelisting/feature_graphic_1024x500.jpg",
        "out": "docs/storelisting/final_feature_graphic.jpg",
        "title": "Device Insights",
        "subtitle": "The Ultimate Android System Monitor",
        "logo_pos": (60, 60),
        "text_pos": (160, 60),
        "scale": 0.6
    }
]

try:
    logo = Image.open("assets/logo.webp").convert("RGBA")
except Exception as e:
    print("Could not open logo:", e)
    logo = Image.new("RGBA", (150, 150), (255, 255, 255, 0))

font_path = "/usr/share/fonts/TTF/OpenSans-Bold.ttf"

for item in images:
    if not os.path.exists(item["file"]):
        print(f"Skipping {item['file']}, does not exist.")
        continue
    
    img = Image.open(item["file"]).convert("RGBA")
    draw = ImageDraw.Draw(img)
    
    scale = item.get("scale", 1.0)
    
    # Scale logo
    logo_w, logo_h = logo.size
    new_logo_w = int(150 * scale)
    new_logo_h = int(150 * scale * (logo_h / logo_w))
    logo_resized = logo.resize((new_logo_w, new_logo_h), Image.LANCZOS)
    
    # Load fonts
    try:
        font_title = ImageFont.truetype(font_path, int(80 * scale))
        font_sub = ImageFont.truetype(font_path, int(45 * scale))
    except:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        
    # Paste logo
    lx, ly = item["logo_pos"]
    img.paste(logo_resized, (lx, ly), logo_resized)
    
    # Draw text with simple drop shadow for readability
    tx, ty = item["text_pos"]
    
    # Shadow
    shadow_offset = max(2, int(4 * scale))
    draw.text((tx+shadow_offset, ty+shadow_offset), item["title"], font=font_title, fill="black")
    draw.text((tx+shadow_offset, ty+int(90*scale)+shadow_offset), item["subtitle"], font=font_sub, fill="black")
    
    # Text
    draw.text((tx, ty), item["title"], font=font_title, fill="white")
    draw.text((tx, ty+int(90*scale)), item["subtitle"], font=font_sub, fill="#E0E0E0")
    
    img = img.convert("RGB")
    img.save(item["out"], quality=95)
    print(f"Saved {item['out']}")
