"""Compare Android instrumentation captures with the supplied SVG references.

Uses Pillow and NumPy. Run from the project directory after pulling screenshots
into docs/screenshots/. The score is an RGB error metric, not a guarantee of
perceptual similarity; the side-by-side image is the primary visual check.
"""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
NAMES = [("start", "Start", "reference_start.png")]
NAMES += [(f"onboarding_{i}", f"Onboarding {i}", f"reference_{i}.png") for i in range(1, 4)]
font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/be_vietnam_pro_regular.ttf"), 16)
results = []
tiles = []

for name, label, reference_file in NAMES:
    reference = Image.open(DOCS / "figma" / reference_file).convert("RGBA")
    background = Image.new("RGBA", reference.size, "white")
    background.alpha_composite(reference)
    reference = background.convert("RGB")
    actual = Image.open(DOCS / "screenshots" / f"{name}.png").convert("RGB")
    scale = min(actual.width / reference.width, actual.height / reference.height)
    width, height = round(reference.width * scale), round(reference.height * scale)
    left, top = round((actual.width - width) / 2), round((actual.height - height) / 2)
    actual = actual.crop((left, top, left + width, top + height)).resize(reference.size, Image.Resampling.LANCZOS)
    actual.save(DOCS / "screenshots" / f"{name}_normalized.png")
    expected_pixels = np.asarray(reference).astype(float)
    actual_pixels = np.asarray(actual).astype(float)
    difference = np.abs(expected_pixels - actual_pixels)
    # Also report the content band, excluding the large empty upper/lower areas.
    band = difference[50:810] if name != "start" else difference[250:510]
    results.append({
        "screen": name,
        "mean_absolute_rgb_error": round(float(difference.mean()), 3),
        "rgb_similarity_percent": round(float(100 * (1 - difference.mean() / 255)), 2),
        "content_band_rgb_similarity_percent": round(float(100 * (1 - band.mean() / 255)), 2),
        "pixels_with_all_channels_within_24_percent": round(float(100 * (difference.max(axis=2) <= 24).mean()), 2)
    })
    tile = Image.new("RGB", (reference.width * 2 + 36, reference.height + 70), "#e8ebf0")
    draw = ImageDraw.Draw(tile)
    draw.text((12, 8), f"{label} — SVG", font=font, fill="#1c1c1e")
    draw.text((reference.width + 24, 8), "Android / Compose", font=font, fill="#1c1c1e")
    tile.paste(reference, (12, 42))
    tile.paste(actual, (reference.width + 24, 42))
    tiles.append(tile)

board = Image.new("RGB", (max(t.width for t in tiles) * 2, max(t.height for t in tiles) * 2), "#e8ebf0")
for index, tile in enumerate(tiles):
    board.paste(tile, ((index % 2) * (board.width // 2), (index // 2) * (board.height // 2)))
board.save(DOCS / "comparison.png")
(DOCS / "visual-comparison.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf8")
print(json.dumps(results, ensure_ascii=False, indent=2))
