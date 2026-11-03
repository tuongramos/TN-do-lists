"""Compare the three new Android screens with SVG references, including
the explicitly requested second provider on the login screen.
"""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
REFERENCES = DOCS / "figma" / "auth"
SHOTS = DOCS / "screenshots"
font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/be_vietnam_pro_regular.ttf"), 16)
results, tiles, actuals = [], [], []

for name, title in [("login", "Đăng nhập"), ("home", "Trang chủ"), ("profile", "Hồ sơ")]:
    ref_name = "reference_login_two_providers.png" if name == "login" else f"reference_{name}.png"
    reference = Image.open(REFERENCES / ref_name).convert("RGBA")
    white = Image.new("RGBA", reference.size, "white")
    white.alpha_composite(reference)
    reference = white.convert("RGB")
    shot = Image.open(SHOTS / f"{name}.png").convert("RGB")
    scale = min(shot.width / 390, shot.height / 844)
    w, h = round(390 * scale), round(844 * scale)
    x, y = round((shot.width - w) / 2), round((shot.height - h) / 2)
    actual = shot.crop((x, y, x+w, y+h)).resize((390, 844), Image.Resampling.LANCZOS)
    actual.save(SHOTS / f"{name}_normalized.png")
    diff = np.abs(np.asarray(reference).astype(float) - np.asarray(actual).astype(float))
    results.append({
        "screen": name,
        "mean_absolute_rgb_error": round(float(diff.mean()), 3),
        "rgb_similarity_percent": round(float(100 * (1 - diff.mean()/255)), 2),
        "pixels_with_all_channels_within_24_percent": round(float(100*(diff.max(axis=2)<=24).mean()), 2),
        "reference": ref_name
    })
    tile = Image.new("RGB", (816, 900), "#e8ebf0")
    draw = ImageDraw.Draw(tile)
    draw.text((12, 8), title + " — SVG", font=font, fill="#1c1c1e")
    draw.text((414, 8), "Android / Compose", font=font, fill="#1c1c1e")
    tile.paste(reference, (12, 42))
    tile.paste(actual, (414, 42))
    tiles.append(tile)
    actuals.append((title, actual))

board = Image.new("RGB", (2448, 900), "#e8ebf0")
for index, tile in enumerate(tiles):
    board.paste(tile, (816*index, 0))
board.save(DOCS / "auth-comparison.png")
preview = Image.new("RGB", (1230, 900), "#e8ebf0")
draw = ImageDraw.Draw(preview)
for index, (title, actual) in enumerate(actuals):
    draw.text((12 + index*410, 8), title, font=font, fill="#1c1c1e")
    preview.paste(actual, (12 + index*410, 42))
preview.save(DOCS / "auth-preview.png")
(DOCS / "auth-visual-comparison.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(results, ensure_ascii=False, indent=2))
