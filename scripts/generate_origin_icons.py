import math
import os
from PIL import Image, ImageDraw, ImageFilter

def create_base_canvas(width=1080, height=1080):
    """
    Vivo OriginOS 6 Official Background:
    135-degree linear gradient from Vivo Electric Blue (#2555FF) to Origin Cyan-Blue (#0099FF)
    with top-left soft dome light ambient glow.
    """
    bg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    bg_pixels = bg.load()
    
    c1 = (37, 85, 255)    # #2555FF
    c2 = (0, 153, 255)    # #0099FF

    for y in range(height):
        for x in range(width):
            # 135 deg: diagonal projection
            t = (x + y) / (width + height)
            r = int(c1[0] * (1.0 - t) + c2[0] * t)
            g = int(c1[1] * (1.0 - t) + c2[1] * t)
            b = int(c1[2] * (1.0 - t) + c2[2] * t)

            # Soft dome light / ambient radial highlight centered at top-left (280, 240)
            dx = x - 280
            dy = y - 240
            dist = math.hypot(dx, dy)
            radius = 760.0
            if dist < radius:
                factor = ((1.0 - dist / radius) ** 1.6) * 0.32
                r = min(255, int(r + (255 - r) * factor))
                g = min(255, int(g + (255 - g) * factor))
                b = min(255, int(b + (255 - b) * factor))

            bg_pixels[x, y] = (r, g, b, 255)

    return bg

def create_foreground_canvas(width=1080, height=1080):
    """
    Vivo OriginOS 6 Central Symbol:
    - Layer 1: Origin Glass Card (frosted squircle card #33FFFFFF with luminous rim #66FFFFFF)
    - Layer 2: Atomic Island Pill (Obsidian Black #0A0E17 with Cyan Sensor #00E5FF & Energy Ring)
    - Layer 3: Expanded Info Streams (Pure White #FFFFFF & 70% White #B3FFFFFF capsules)
    """
    fg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    
    # LAYER 1: Origin Glass Card
    # Bounds: [260, 270, 820, 810], corner radius 140
    shadow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle([260, 285, 820, 825], radius=140, fill=(0, 32, 77, 45))
    fg = Image.alpha_composite(fg, shadow)

    card_surface = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    cs_draw = ImageDraw.Draw(card_surface)
    cs_draw.rounded_rectangle([260, 270, 820, 810], radius=140, fill=(255, 255, 255, 51))
    fg = Image.alpha_composite(fg, card_surface)

    draw = ImageDraw.Draw(fg)
    # Glass rim
    draw.rounded_rectangle([260, 270, 820, 810], radius=140, outline=(255, 255, 255, 102), width=12)
    # Glass top specular highlight
    draw.line([(400, 276), (680, 276)], fill=(255, 255, 255, 160), width=10)

    # LAYER 2: Atomic Island Pill
    # [330, 360, 750, 520], radius 80
    halo = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    h_draw = ImageDraw.Draw(halo)
    h_draw.rounded_rectangle([316, 346, 764, 534], radius=94, fill=(37, 85, 255, 90))
    halo = halo.filter(ImageFilter.GaussianBlur(10))
    fg = Image.alpha_composite(fg, halo)

    draw = ImageDraw.Draw(fg)
    # Obsidian body
    draw.rounded_rectangle([330, 360, 750, 520], radius=80, fill=(10, 14, 23, 255), outline=(45, 59, 82, 255), width=8)

    # Left Sensor Dot (cx=405, cy=440)
    cx, cy = 405, 440
    sensor_halo = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(sensor_halo)
    sh_draw.ellipse([cx - 35, cy - 35, cx + 35, cy + 35], fill=(0, 229, 255, 75))
    fg = Image.alpha_composite(fg, sensor_halo)

    draw = ImageDraw.Draw(fg)
    draw.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], fill=(0, 229, 255, 255))
    draw.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(255, 255, 255, 255))

    # Right Energy Ring (rx=675, ry=440)
    rx, ry = 675, 440
    r_outer = 45
    draw.ellipse([rx - r_outer, ry - r_outer, rx + r_outer, ry + r_outer], outline=(30, 44, 66, 255), width=15)
    draw.arc([rx - r_outer, ry - r_outer, rx + r_outer, ry + r_outer], start=-90, end=160, fill=(0, 229, 255, 255), width=15)
    draw.ellipse([rx - 15, ry - 15, rx + 15, ry + 15], fill=(56, 122, 255, 255))

    # LAYER 3: Waveform & Info Bars
    # Primary bar & status chip
    draw.rounded_rectangle([340, 580, 590, 620], radius=20, fill=(255, 255, 255, 255))
    draw.rounded_rectangle([630, 580, 740, 620], radius=20, fill=(0, 229, 255, 255))

    # Secondary stream bar
    draw.rounded_rectangle([340, 655, 640, 685], radius=15, fill=(255, 255, 255, 180))

    return fg

def render_all_icons(project_res_dir):
    print("Generating base vector art at 1080x1080...")
    bg = create_base_canvas(1080, 1080)
    fg = create_foreground_canvas(1080, 1080)
    full_icon = Image.alpha_composite(bg, fg)

    # 1. Squircle mask for standard ic_launcher.png (radius 220)
    squircle_mask = Image.new("L", (1080, 1080), 0)
    m_draw = ImageDraw.Draw(squircle_mask)
    m_draw.rounded_rectangle([0, 0, 1080, 1080], radius=220, fill=255)

    square_icon = Image.new("RGBA", (1080, 1080), (0, 0, 0, 0))
    square_icon.paste(full_icon, (0, 0), squircle_mask)

    # 2. Circular mask for ic_launcher_round.png
    circle_mask = Image.new("L", (1080, 1080), 0)
    c_draw = ImageDraw.Draw(circle_mask)
    c_draw.ellipse([0, 0, 1080, 1080], fill=255)

    round_icon = Image.new("RGBA", (1080, 1080), (0, 0, 0, 0))
    round_icon.paste(full_icon, (0, 0), circle_mask)

    # Density specifications
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder_name, size in densities.items():
        folder_path = os.path.join(project_res_dir, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        # 1. ic_launcher.png
        icon_path = os.path.join(folder_path, "ic_launcher.png")
        img_resized = square_icon.resize((size, size), Image.Resampling.LANCZOS)
        img_resized.save(icon_path, "PNG", optimize=True)
        print(f"Rendered: {icon_path} ({size}x{size})")

        # 2. ic_launcher_round.png
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        round_resized = round_icon.resize((size, size), Image.Resampling.LANCZOS)
        round_resized.save(round_path, "PNG", optimize=True)
        print(f"Rendered: {round_path} ({size}x{size})")

        # 3. ic_launcher_foreground.png
        fg_path = os.path.join(folder_path, "ic_launcher_foreground.png")
        fg_resized = fg.resize((size, size), Image.Resampling.LANCZOS)
        fg_resized.save(fg_path, "PNG", optimize=True)
        print(f"Rendered: {fg_path} ({size}x{size})")

    # Update drawable-xxxhdpi/ic_launcher_foreground.png (432x432)
    drawable_xxxhdpi = os.path.join(project_res_dir, "drawable-xxxhdpi")
    if os.path.exists(drawable_xxxhdpi):
        drawable_fg_path = os.path.join(drawable_xxxhdpi, "ic_launcher_foreground.png")
        fg_432 = fg.resize((432, 432), Image.Resampling.LANCZOS)
        fg_432.save(drawable_fg_path, "PNG", optimize=True)
        print(f"Rendered: {drawable_fg_path} (432x432)")

    print("All PNG icons rendered successfully!")

if __name__ == "__main__":
    res_dir = r"C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\app\src\main\res"
    render_all_icons(res_dir)
