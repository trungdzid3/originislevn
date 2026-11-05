import math
import os
from PIL import Image, ImageDraw, ImageFilter

def create_base_canvas(width=1080, height=1080):
    """
    Vivo OriginOS 6 Official Background:
    Deep cosmic indigo & sapphire gradient (#0D1730 -> #1A36A0 -> #2555FF)
    with top-left soft dome light ambient glow.
    """
    bg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    bg_pixels = bg.load()
    
    # 3-stop gradient: Deep cosmic blue (#0A1128) to Vivo Royal Blue (#1E3A8A) to Electric Blue (#2555FF)
    c0 = (10, 17, 40)     # #0A1128
    c1 = (30, 58, 138)    # #1E3A8A
    c2 = (37, 85, 255)    # #2555FF

    for y in range(height):
        for x in range(width):
            # 135 deg: diagonal projection
            t = (x + y) / (width + height)
            if t < 0.5:
                sub_t = t * 2.0
                r = int(c0[0] * (1.0 - sub_t) + c1[0] * sub_t)
                g = int(c0[1] * (1.0 - sub_t) + c1[1] * sub_t)
                b = int(c0[2] * (1.0 - sub_t) + c1[2] * sub_t)
            else:
                sub_t = (t - 0.5) * 2.0
                r = int(c1[0] * (1.0 - sub_t) + c2[0] * sub_t)
                g = int(c1[1] * (1.0 - sub_t) + c2[1] * sub_t)
                b = int(c1[2] * (1.0 - sub_t) + c2[2] * sub_t)

            # Soft dome light / ambient radial highlight centered at top-left (280, 240)
            dx = x - 280
            dy = y - 240
            dist = math.hypot(dx, dy)
            radius = 720.0
            if dist < radius:
                factor = ((1.0 - dist / radius) ** 1.8) * 0.40
                r = min(255, int(r + (255 - r) * factor))
                g = min(255, int(g + (255 - g) * factor))
                b = min(255, int(b + (255 - b) * factor))

            # Bottom-right neon cyan subtle reflection
            dx2 = x - 900
            dy2 = y - 900
            dist2 = math.hypot(dx2, dy2)
            if dist2 < 600.0:
                factor2 = ((1.0 - dist2 / 600.0) ** 2.0) * 0.18
                g = min(255, int(g + (229 - g) * factor2))
                b = min(255, int(b + (255 - b) * factor2))

            bg_pixels[x, y] = (r, g, b, 255)

    return bg

def create_foreground_canvas(width=1080, height=1080):
    """
    Vivo OriginOS 6 Central Symbol:
    - Layer 1: G2 Squircle Liquid Glass Card (#33FFFFFF with specular rim #E6FFFFFF)
    - Layer 2: Glowing Atomic Island Pill (Deep Obsidian #0D111A with Dynamic Glow Neon Cyan & Electric Blue)
    - Layer 3: Optics Telemetry (Camera lens dot, pulse sensor ring, audio dynamic bars)
    """
    fg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    
    # -------------------------------------------------------------
    # LAYER 1: Liquid Glass Card (Squircle)
    # [250, 250, 830, 830], G2 continuous curvature radius 160
    # -------------------------------------------------------------
    # 1.1 Card Ambient Depth Shadow
    shadow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle([250, 275, 830, 855], radius=160, fill=(5, 13, 36, 80))
    shadow = shadow.filter(ImageFilter.GaussianBlur(15))
    fg = Image.alpha_composite(fg, shadow)

    # 1.2 Translucent Frosted Glass Card
    card_surface = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    cs_draw = ImageDraw.Draw(card_surface)
    cs_draw.rounded_rectangle([250, 250, 830, 830], radius=160, fill=(255, 255, 255, 38))
    fg = Image.alpha_composite(fg, card_surface)

    # 1.3 Glass Rim
    draw = ImageDraw.Draw(fg)
    draw.rounded_rectangle([250, 250, 830, 830], radius=160, outline=(255, 255, 255, 85), width=10)

    # 1.4 Specular Rim Highlight (Top & Top-Left arc)
    draw.arc([250, 250, 570, 570], start=180, end=270, fill=(255, 255, 255, 230), width=12)
    draw.line([(410, 250), (670, 250)], fill=(255, 255, 255, 210), width=10)

    # -------------------------------------------------------------
    # LAYER 2: Atomic Island Pill (Viên nang Đảo Nguyên Tử)
    # Bounds: [310, 360, 770, 520], height=160, radius=80
    # -------------------------------------------------------------
    # 2.1 Outer Dynamic Glow Halo (Electric Blue #2555FF)
    halo_blue = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    hb_draw = ImageDraw.Draw(halo_blue)
    hb_draw.rounded_rectangle([290, 340, 790, 540], radius=100, fill=(37, 85, 255, 110))
    halo_blue = halo_blue.filter(ImageFilter.GaussianBlur(20))
    fg = Image.alpha_composite(fg, halo_blue)

    # 2.2 Inner Dynamic Glow Halo (Neon Cyan #00E5FF)
    halo_cyan = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    hc_draw = ImageDraw.Draw(halo_cyan)
    hc_draw.rounded_rectangle([302, 352, 778, 528], radius=88, fill=(0, 229, 255, 130))
    halo_cyan = halo_cyan.filter(ImageFilter.GaussianBlur(8))
    fg = Image.alpha_composite(fg, halo_cyan)

    # 2.3 Deep Obsidian Island Pill Body (#090D16)
    draw = ImageDraw.Draw(fg)
    draw.rounded_rectangle([310, 360, 770, 520], radius=80, fill=(9, 13, 22, 255), outline=(32, 45, 68, 255), width=7)

    # 2.4 Top Specular Line on Pill
    draw.line([(390, 364), (690, 364)], fill=(0, 229, 255, 160), width=5)

    # -------------------------------------------------------------
    # LAYER 3: Telemetry Optics & Waveforms (Camera & Live Data)
    # -------------------------------------------------------------
    # 3.1 Camera / Sensor Aperture on the Left (center cx=390, cy=440)
    cx, cy = 390, 440
    sensor_glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    sg_draw = ImageDraw.Draw(sensor_glow)
    sg_draw.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], fill=(0, 229, 255, 90))
    sensor_glow = sensor_glow.filter(ImageFilter.GaussianBlur(6))
    fg = Image.alpha_composite(fg, sensor_glow)

    draw = ImageDraw.Draw(fg)
    # Outer cyan optical ring
    draw.ellipse([cx - 28, cy - 28, cx + 28, cy + 28], outline=(0, 229, 255, 255), width=7)
    # Inner dark lens
    draw.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], fill=(5, 8, 15, 255))
    # Glass glint
    draw.ellipse([cx - 10, cy - 10, cx - 2, cy - 2], fill=(255, 255, 255, 220))

    # 3.2 Right: Animated Dynamic Island Waveform Bars (rx=620, ry=440)
    # 4 distinct audio/progress bars in neon cyan & electric blue & white
    bars = [
        (560, 440, 28, (0, 229, 255, 255)),    # Cyan
        (600, 440, 52, (255, 255, 255, 255)),  # Pure White hero bar
        (640, 440, 38, (59, 130, 246, 255)),   # Blue
        (680, 440, 22, (0, 229, 255, 220)),    # Cyan
    ]
    for bx, by, bh, bcolor in bars:
        draw.rounded_rectangle([bx - 6, by - bh//2, bx + 6, by + bh//2], radius=6, fill=bcolor)

    # -------------------------------------------------------------
    # LAYER 4: SuperX Multi-Tier Sub-Streams (Thẻ mở rộng đa tầng)
    # -------------------------------------------------------------
    # 4.1 Primary Atomic capsule bar
    draw.rounded_rectangle([320, 580, 580, 624], radius=22, fill=(255, 255, 255, 240))
    # 4.2 Status chip pill (Neon Cyan #00E5FF)
    draw.rounded_rectangle([620, 580, 760, 624], radius=22, fill=(0, 229, 255, 255))

    # 4.3 Secondary sub-bar (Translucent White)
    draw.rounded_rectangle([320, 660, 680, 696], radius=18, fill=(255, 255, 255, 140))

    return fg

def render_all_icons(project_res_dir):
    print("Generating OriginOS 6 vector art at 1080x1080...")
    bg = create_base_canvas(1080, 1080)
    fg = create_foreground_canvas(1080, 1080)
    full_icon = Image.alpha_composite(bg, fg)

    # 1. G2 Squircle mask for standard ic_launcher.png (radius 240)
    squircle_mask = Image.new("L", (1080, 1080), 0)
    m_draw = ImageDraw.Draw(squircle_mask)
    m_draw.rounded_rectangle([0, 0, 1080, 1080], radius=240, fill=255)

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
