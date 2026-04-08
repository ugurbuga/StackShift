#!/usr/bin/env python3
from __future__ import annotations

import shutil
import struct
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_RES = ROOT / "composeApp" / "src" / "androidMain" / "res"
IOS_APPICON = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset"
DESKTOP_RES = ROOT / "composeApp" / "src" / "jvmMain" / "resources"
DESKTOP_ICONS = ROOT / "composeApp" / "desktop-icons"
BRANDING_OUT = ROOT / "branding" / "generated"

FULL_SVG_NAME = "stackshift-app-icon.svg"
FOREGROUND_SVG_NAME = "stackshift-adaptive-foreground.svg"
FULL_PNG_NAME = "stackshift-app-icon-1024.png"
FOREGROUND_PNG_NAME = "stackshift-adaptive-foreground-1024.png"


def main() -> None:
    ensure_tools()
    with tempfile.TemporaryDirectory(prefix="stackshift-icon-") as temp_dir:
        temp_path = Path(temp_dir)
        full_svg = temp_path / FULL_SVG_NAME
        foreground_svg = temp_path / FOREGROUND_SVG_NAME
        full_svg.write_text(full_icon_svg(), encoding="utf-8")
        foreground_svg.write_text(foreground_icon_svg(), encoding="utf-8")

        full_png = BRANDING_OUT / FULL_PNG_NAME
        foreground_png = BRANDING_OUT / FOREGROUND_PNG_NAME
        BRANDING_OUT.mkdir(parents=True, exist_ok=True)
        rasterize_svg(full_svg, full_png, 1024)
        rasterize_svg(foreground_svg, foreground_png, 1024)

        generate_android_assets(full_png, foreground_png)
        generate_ios_assets(full_png)
        generate_desktop_assets(full_png)

    print("Generated StackShift icons for Android, iOS, and desktop.")


def ensure_tools() -> None:
    missing = [tool for tool in ("qlmanage", "sips", "iconutil") if shutil.which(tool) is None]
    if missing:
        raise SystemExit(f"Missing required tools: {', '.join(missing)}")


def run(*args: str) -> None:
    subprocess.run(args, check=True)


def rasterize_svg(svg_path: Path, output_png: Path, size: int) -> None:
    output_png.parent.mkdir(parents=True, exist_ok=True)
    temp_dir = output_png.parent
    run("qlmanage", "-t", "-s", str(size), "-o", str(temp_dir), str(svg_path))
    generated = temp_dir / f"{svg_path.name}.png"
    if not generated.exists():
        raise FileNotFoundError(f"Quick Look did not generate PNG for {svg_path}")
    if output_png.exists():
        output_png.unlink()
    generated.rename(output_png)


def resize_png(source: Path, target: Path, size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    run("sips", "-z", str(size), str(size), str(source), "--out", str(target))


def generate_android_assets(full_png: Path, foreground_png: Path) -> None:
    android_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in android_sizes.items():
        resize_png(full_png, ANDROID_RES / folder / "ic_launcher.png", size)
        resize_png(full_png, ANDROID_RES / folder / "ic_launcher_round.png", size)

    resize_png(
        foreground_png,
        ANDROID_RES / "drawable" / "ic_launcher_foreground_art.png",
        432,
    )
    resize_png(
        foreground_png,
        ANDROID_RES / "drawable-nodpi" / "ic_launcher_foreground_art.png",
        432,
    )


def generate_ios_assets(full_png: Path) -> None:
    resize_png(full_png, IOS_APPICON / "app-icon-1024.png", 1024)


def generate_desktop_assets(full_png: Path) -> None:
    DESKTOP_RES.mkdir(parents=True, exist_ok=True)
    DESKTOP_ICONS.mkdir(parents=True, exist_ok=True)

    resize_png(full_png, DESKTOP_RES / "app_icon_window.png", 512)
    resize_png(full_png, DESKTOP_ICONS / "stackshift.png", 512)

    ico_sizes = [16, 24, 32, 48, 64, 128, 256]
    ico_images: list[tuple[int, bytes]] = []
    iconset_dir = DESKTOP_ICONS / "StackShift.iconset"
    if iconset_dir.exists():
        shutil.rmtree(iconset_dir)
    iconset_dir.mkdir(parents=True, exist_ok=True)

    iconset_map = {
        "icon_16x16.png": 16,
        "icon_16x16@2x.png": 32,
        "icon_32x32.png": 32,
        "icon_32x32@2x.png": 64,
        "icon_128x128.png": 128,
        "icon_128x128@2x.png": 256,
        "icon_256x256.png": 256,
        "icon_256x256@2x.png": 512,
        "icon_512x512.png": 512,
        "icon_512x512@2x.png": 1024,
    }

    with tempfile.TemporaryDirectory(prefix="stackshift-ico-") as ico_temp_dir:
        ico_temp = Path(ico_temp_dir)
        for size in ico_sizes:
            png_path = ico_temp / f"stackshift-{size}.png"
            resize_png(full_png, png_path, size)
            ico_images.append((size, png_path.read_bytes()))

        write_ico(DESKTOP_ICONS / "stackshift.ico", ico_images)

    for name, size in iconset_map.items():
        resize_png(full_png, iconset_dir / name, size)
    icns_path = DESKTOP_ICONS / "stackshift.icns"
    if icns_path.exists():
        icns_path.unlink()
    run("iconutil", "-c", "icns", str(iconset_dir), "-o", str(icns_path))
    shutil.rmtree(iconset_dir)


def write_ico(path: Path, images: list[tuple[int, bytes]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    count = len(images)
    header = struct.pack("<HHH", 0, 1, count)
    entries = bytearray()
    image_data = bytearray()
    offset = 6 + (16 * count)

    for size, data in images:
        width = 0 if size >= 256 else size
        height = 0 if size >= 256 else size
        entries.extend(
            struct.pack(
                "<BBBBHHII",
                width,
                height,
                0,
                0,
                1,
                32,
                len(data),
                offset,
            )
        )
        image_data.extend(data)
        offset += len(data)

    path.write_bytes(header + entries + image_data)


def full_icon_svg() -> str:
    return board_icon_svg(include_background=True)


def foreground_icon_svg() -> str:
    return board_icon_svg(include_background=False)


def board_icon_svg(include_background: bool) -> str:
    background = "" if not include_background else """
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenBg)\"/>
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenGlowA)\"/>
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenGlowB)\"/>
"""
    return f"""<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1024\" height=\"1024\" viewBox=\"0 0 1024 1024\" fill=\"none\">
  <defs>
    <linearGradient id=\"screenBg\" x1=\"132\" y1=\"92\" x2=\"884\" y2=\"936\" gradientUnits=\"userSpaceOnUse\">
      <stop offset=\"0\" stop-color=\"#152434\"/>
      <stop offset=\"0.48\" stop-color=\"#0C1621\"/>
      <stop offset=\"1\" stop-color=\"#070D14\"/>
    </linearGradient>
    <radialGradient id=\"screenGlowA\" cx=\"0\" cy=\"0\" r=\"1\" gradientUnits=\"userSpaceOnUse\" gradientTransform=\"translate(232 214) rotate(45) scale(448)\">
      <stop stop-color=\"#48D8C8\" stop-opacity=\"0.26\"/>
      <stop offset=\"1\" stop-color=\"#48D8C8\" stop-opacity=\"0\"/>
    </radialGradient>
    <radialGradient id=\"screenGlowB\" cx=\"0\" cy=\"0\" r=\"1\" gradientUnits=\"userSpaceOnUse\" gradientTransform=\"translate(796 822) rotate(45) scale(404)\">
      <stop stop-color=\"#6B74FF\" stop-opacity=\"0.22\"/>
      <stop offset=\"1\" stop-color=\"#6B74FF\" stop-opacity=\"0\"/>
    </radialGradient>
    <linearGradient id=\"boardBg\" x1=\"108\" y1=\"104\" x2=\"918\" y2=\"930\" gradientUnits=\"userSpaceOnUse\">
      <stop offset=\"0\" stop-color=\"#111D29\"/>
      <stop offset=\"0.52\" stop-color=\"#152230\"/>
      <stop offset=\"1\" stop-color=\"#0C141D\"/>
    </linearGradient>
    <radialGradient id=\"boardGlowA\" cx=\"0\" cy=\"0\" r=\"1\" gradientUnits=\"userSpaceOnUse\" gradientTransform=\"translate(232 214) rotate(45) scale(488)\">
      <stop stop-color=\"#48D8C8\" stop-opacity=\"0.20\"/>
      <stop offset=\"1\" stop-color=\"#48D8C8\" stop-opacity=\"0\"/>
    </radialGradient>
    <radialGradient id=\"boardGlowB\" cx=\"0\" cy=\"0\" r=\"1\" gradientUnits=\"userSpaceOnUse\" gradientTransform=\"translate(808 826) rotate(45) scale(436)\">
      <stop stop-color=\"#6B74FF\" stop-opacity=\"0.18\"/>
      <stop offset=\"1\" stop-color=\"#6B74FF\" stop-opacity=\"0\"/>
    </radialGradient>
  </defs>
  {background}
  <rect x=\"104\" y=\"104\" width=\"816\" height=\"816\" rx=\"132\" fill=\"url(#boardBg)\"/>
  <rect x=\"104\" y=\"104\" width=\"816\" height=\"816\" rx=\"132\" fill=\"url(#boardGlowA)\"/>
  <rect x=\"104\" y=\"104\" width=\"816\" height=\"816\" rx=\"132\" fill=\"url(#boardGlowB)\"/>
  <rect x=\"104\" y=\"104\" width=\"816\" height=\"816\" rx=\"132\" stroke=\"#9AB0C6\" stroke-opacity=\"0.30\" stroke-width=\"8\"/>
  {board_slots()}
  {placed_blocks()}
</svg>
"""


def board_geometry() -> dict[str, int]:
    board_x = 148
    board_y = 148
    board_size = 728
    grid_size = 5
    gap = 12
    cell_size = (board_size - ((grid_size - 1) * gap)) // grid_size
    return {
        "board_x": board_x,
        "board_y": board_y,
        "board_size": board_size,
        "grid_size": grid_size,
        "gap": gap,
        "cell_size": cell_size,
    }


def board_slots() -> str:
    geometry = board_geometry()
    lines: list[str] = []
    for row in range(geometry["grid_size"]):
        for column in range(geometry["grid_size"]):
            x = geometry["board_x"] + column * (geometry["cell_size"] + geometry["gap"])
            y = geometry["board_y"] + row * (geometry["cell_size"] + geometry["gap"])
            lines.append(
                f'  <rect x="{x}" y="{y}" width="{geometry["cell_size"]}" height="{geometry["cell_size"]}" rx="28" fill="#EAF7FF" fill-opacity="0.055"/>'
            )
            lines.append(
                f'  <rect x="{x}" y="{y}" width="{geometry["cell_size"]}" height="{geometry["cell_size"]}" rx="28" stroke="#EAF7FF" stroke-opacity="0.08" stroke-width="3"/>'
            )
    return "\n".join(lines)


def placed_blocks() -> str:
    geometry = board_geometry()
    placements = [
        ((0, 0), "#4FC3F7"),
        ((0, 1), "#4FC3F7"),
        ((0, 3), "#FFD166"),
        ((0, 4), "#FFD166"),
        ((1, 1), "#4FC3F7"),
        ((1, 2), "#C084FC"),
        ((1, 3), "#FFD166"),
        ((2, 0), "#2DD4BF"),
        ((2, 1), "#57E389"),
        ((2, 2), "#C084FC"),
        ((2, 4), "#FF9F43"),
        ((3, 0), "#2DD4BF"),
        ((3, 2), "#57E389"),
        ((3, 3), "#57E389"),
        ((3, 4), "#FF9F43"),
        ((4, 0), "#FF7A90"),
        ((4, 1), "#FF7A90"),
        ((4, 3), "#FF9F43"),
    ]
    return "\n".join(
        tile_block(
            x=geometry["board_x"] + column * (geometry["cell_size"] + geometry["gap"]),
            y=geometry["board_y"] + row * (geometry["cell_size"] + geometry["gap"]),
            color=color,
            size=geometry["cell_size"],
        )
        for (row, column), color in placements
    )


def tile_block(x: int, y: int, color: str, size: int = 150) -> str:
    stroke = shade(color, 0.72)
    inner = tint(color, 0.16)
    corner = round(size * 0.24)
    inner_inset = round(size * 0.10)
    inner_width = size - (inner_inset * 2)
    shine_height = round(size * 0.20)
    stroke_width = max(4, round(size * 0.066))
    return f"""
  <g>
    <rect x=\"{x}\" y=\"{y}\" width=\"{size}\" height=\"{size}\" rx=\"{corner}\" fill=\"{color}\"/>
    <rect x=\"{x}\" y=\"{y}\" width=\"{size}\" height=\"{size}\" rx=\"{corner}\" stroke=\"{stroke}\" stroke-width=\"{stroke_width}\" stroke-opacity=\"0.70\"/>
    <rect x=\"{x + inner_inset}\" y=\"{y + inner_inset}\" width=\"{inner_width}\" height=\"{shine_height}\" rx=\"{round(shine_height / 2)}\" fill=\"{inner}\" fill-opacity=\"0.88\"/>
  </g>"""


def tint(hex_color: str, amount: float) -> str:
    r, g, b = parse_hex(hex_color)
    r = int(r + (255 - r) * amount)
    g = int(g + (255 - g) * amount)
    b = int(b + (255 - b) * amount)
    return to_hex(r, g, b)


def shade(hex_color: str, amount: float) -> str:
    r, g, b = parse_hex(hex_color)
    return to_hex(int(r * amount), int(g * amount), int(b * amount))


def parse_hex(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.removeprefix('#')
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


def to_hex(r: int, g: int, b: int) -> str:
    return f"#{max(0, min(255, r)):02X}{max(0, min(255, g)):02X}{max(0, min(255, b)):02X}"


if __name__ == "__main__":
    main()

