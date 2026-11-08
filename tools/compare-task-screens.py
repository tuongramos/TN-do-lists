"""Compare task screenshots with the exact supplied list/detail SVG frames."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/be_vietnam_pro_regular.ttf"), 16)
results, comparisons, previews = [], [], []
for name, title, height in [("list", "Danh sách", 844), ("detail", "Chi tiết công việc", 941)]:
    reference = Image.open(DOCS / "figma/tasks" / f"reference_{name}.png").convert("RGB")
    shot = Image.open(DOCS / "screenshots" / f"task_{name}.png").convert("RGB")
    scale = min(shot.width / 390, shot.height / height)
    width, scaled_height = round(390 * scale), round(height * scale)
    x, y = round((shot.width - width) / 2), round((shot.height - scaled_height) / 2)
    actual = shot.crop((x, y, x + width, y + scaled_height)).resize((390, height), Image.Resampling.LANCZOS)
    difference = np.abs(np.asarray(reference).astype(float) - np.asarray(actual).astype(float))
    results.append({
        "screen": name, "frame": [390, height],
        "mean_absolute_rgb_error": round(float(difference.mean()), 3),
        "rgb_similarity_percent": round(float(100 * (1 - difference.mean() / 255)), 2),
        "pixels_with_all_channels_within_24_percent": round(float(100 * (difference.max(axis=2) <= 24).mean()), 2)
    })
    tile = Image.new("RGB", (816, height + 56), "#e8ebf0")
    draw = ImageDraw.Draw(tile)
    draw.text((12, 8), title + " — SVG", font=font, fill="#1c1c1e")
    draw.text((414, 8), "Android / Compose", font=font, fill="#1c1c1e")
    tile.paste(reference, (12, 42))
    tile.paste(actual, (414, 42))
    comparisons.append(tile)
    previews.append((title, actual))

board = Image.new("RGB", (816, sum(tile.height for tile in comparisons)), "#e8ebf0")
y = 0
for tile in comparisons:
    board.paste(tile, (0, y))
    y += tile.height
board.save(DOCS / "tasks-comparison.png")
preview = Image.new("RGB", (816, 997), "#e8ebf0")
draw = ImageDraw.Draw(preview)
for index, (title, actual) in enumerate(previews):
    draw.text((12 + index * 402, 8), title, font=font, fill="#1c1c1e")
    preview.paste(actual, (12 + index * 402, 42))
preview.save(DOCS / "tasks-preview.png")
(DOCS / "tasks-visual-comparison.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(results, ensure_ascii=False, indent=2))
