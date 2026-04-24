#!/usr/bin/env python3
from __future__ import annotations

import shutil
import struct
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
IOS_APPICON = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset"
DESKTOP_RES = ROOT / "composeApp" / "src" / "jvmMain" / "resources"
DESKTOP_ICONS = ROOT / "composeApp" / "desktop-icons"
BRANDING_OUT = ROOT / "branding" / "generated"

FULL_SVG_NAME = "stackshift-app-icon.svg"
FOREGROUND_SVG_NAME = "stackshift-adaptive-foreground.svg"
MACOS_FOREGROUND_SVG_NAME = "stackshift-macos-foreground.svg"
ANDROID_ROUND_SVG_NAME = "stackshift-android-round-icon.svg"
FULL_PNG_NAME = "stackshift-app-icon-1024.png"
FOREGROUND_PNG_NAME = "stackshift-adaptive-foreground-1024.png"
MACOS_FOREGROUND_PNG_NAME = "stackshift-macos-foreground-1024.png"
ANDROID_ROUND_PNG_NAME = "stackshift-android-round-1024.png"
FEATURE_GRAPHIC_SVG_NAME = "stackshift-feature-graphic-1024x500.svg"
FEATURE_GRAPHIC_PNG_NAME = "stackshift-feature-graphic-1024x500.png"


def main() -> None:
    ensure_tools()
    with tempfile.TemporaryDirectory(prefix="stackshift-icon-") as temp_dir:
        temp_path = Path(temp_dir)
        full_svg = temp_path / FULL_SVG_NAME
        foreground_svg = temp_path / FOREGROUND_SVG_NAME
        macos_foreground_svg = temp_path / MACOS_FOREGROUND_SVG_NAME
        android_round_svg = temp_path / ANDROID_ROUND_SVG_NAME
        full_svg.write_text(full_icon_svg(), encoding="utf-8")
        foreground_svg.write_text(foreground_icon_svg(), encoding="utf-8")
        macos_foreground_svg.write_text(macos_foreground_icon_svg(), encoding="utf-8")
        android_round_svg.write_text(android_round_icon_svg(), encoding="utf-8")

        full_png = BRANDING_OUT / FULL_PNG_NAME
        foreground_png = BRANDING_OUT / FOREGROUND_PNG_NAME
        macos_foreground_png = BRANDING_OUT / MACOS_FOREGROUND_PNG_NAME
        android_round_png = BRANDING_OUT / ANDROID_ROUND_PNG_NAME
        BRANDING_OUT.mkdir(parents=True, exist_ok=True)
        rasterize_svg(full_svg, full_png, 1024)
        rasterize_svg(foreground_svg, foreground_png, 1024)
        rasterize_svg(macos_foreground_svg, macos_foreground_png, 1024)
        rasterize_svg(android_round_svg, android_round_png, 1024)

        feature_graphic_svg = temp_path / FEATURE_GRAPHIC_SVG_NAME
        feature_graphic_svg.write_text(feature_graphic_svg_content(), encoding="utf-8")
        feature_graphic_png = BRANDING_OUT / FEATURE_GRAPHIC_PNG_NAME
        rasterize_svg(feature_graphic_svg, feature_graphic_png, 1024, height=500)

        generate_android_assets(full_png, foreground_png, android_round_png)
        generate_ios_assets(full_png)
        generate_desktop_assets(full_png, macos_foreground_png)

    print("Generated StackShift icons for Android, iOS, desktop, and feature graphic.")


def ensure_tools() -> None:
    missing = [tool for tool in ("sips", "iconutil") if shutil.which(tool) is None]
    if missing:
        raise SystemExit(f"Missing required tools: {', '.join(missing)}")


def run(*args: str) -> None:
    subprocess.run(args, check=True)


def rasterize_svg(svg_path: Path, output_png: Path, size: int, height: int | None = None) -> None:
    output_png.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="stackshift-svg-raster-") as temp_dir:
        temp_path = Path(temp_dir)
        converted = temp_path / f"{svg_path.stem}.png"
        if height is not None:
            run("sips", "-s", "format", "png", str(svg_path), "--resampleHeightWidth", str(height), str(size), "--out", str(converted))
        else:
            run("sips", "-s", "format", "png", str(svg_path), "--out", str(converted))
        if height is not None:
            run("sips", "-z", str(height), str(size), str(converted), "--out", str(output_png))
        else:
            resize_png(converted, output_png, size)


def resize_png(source: Path, target: Path, size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    run("sips", "-z", str(size), str(size), str(source), "--out", str(target))


def generate_android_assets(full_png: Path, foreground_png: Path, round_png: Path) -> None:
    android_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in android_sizes.items():
        resize_png(full_png, ANDROID_RES / folder / "ic_launcher.png", size)
        resize_png(round_png, ANDROID_RES / folder / "ic_launcher_round.png", size)

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


def generate_desktop_assets(full_png: Path, macos_png: Path) -> None:
    DESKTOP_RES.mkdir(parents=True, exist_ok=True)
    DESKTOP_ICONS.mkdir(parents=True, exist_ok=True)

    resize_png(full_png, DESKTOP_RES / "app_icon_window.png", 512)
    resize_png(macos_png, DESKTOP_RES / "app_icon_window_macos.png", 512)
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
        resize_png(macos_png, iconset_dir / name, size)
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
    return board_icon_svg(include_background=True, board_scale=0.82)


def foreground_icon_svg() -> str:
    return board_icon_svg(
        include_background=False,
        clip_shape="rounded_rect",
        board_scale=0.78,
        clip_inset=96,
        clip_radius=188,
        surface_inset=138,
        surface_radius=168,
    )


def macos_foreground_icon_svg() -> str:
    return board_icon_svg(
        include_background=False,
        clip_shape="rounded_rect",
        board_scale=0.90,
        clip_inset=82,
        clip_radius=192,
        surface_inset=112,
        surface_radius=180,
    )


def android_round_icon_svg() -> str:
    return board_icon_svg(include_background=True, clip_shape="circle", board_scale=0.88)


def board_icon_svg(
    include_background: bool,
    clip_shape: str | None = None,
    board_scale: float = 1.0,
    clip_inset: int | None = None,
    clip_radius: int | None = None,
    surface_inset: int | None = None,
    surface_radius: int | None = None,
) -> str:
    geometry = board_geometry(board_scale=board_scale)
    background = "" if not include_background else """
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenBg)\"/>
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenGlowA)\"/>
  <rect width=\"1024\" height=\"1024\" fill=\"url(#screenGlowB)\"/>
"""
    resolved_clip_inset = 0 if clip_shape == "circle" else (clip_inset if clip_inset is not None else 112)
    resolved_clip_radius = clip_radius if clip_radius is not None else 176
    clip_size = 1024 - (resolved_clip_inset * 2)
    clip_definition = {
        None: "",
        "circle": """
    <clipPath id=\"iconMask\">
      <circle cx=\"512\" cy=\"512\" r=\"512\"/>
    </clipPath>
""",
        "rounded_rect": f"""
    <clipPath id=\"iconMask\">
      <rect x=\"{resolved_clip_inset}\" y=\"{resolved_clip_inset}\" width=\"{clip_size}\" height=\"{clip_size}\" rx=\"{resolved_clip_radius}\"/>
    </clipPath>
""",
    }[clip_shape]
    resolved_surface_inset = surface_inset if surface_inset is not None else geometry["board_x"] - max(20, round(32 * board_scale))
    surface_x = resolved_surface_inset
    surface_y = resolved_surface_inset
    surface_size = 1024 - (resolved_surface_inset * 2)
    surface_corner = surface_radius if surface_radius is not None else max(40, round(144 * board_scale))

    # macOS için arkaplanı geri ekliyoruz
    board_surface = "" if include_background and clip_shape != "rounded_rect" else f"""
  <rect x=\"{surface_x}\" y=\"{surface_y}\" width=\"{surface_size}\" height=\"{surface_size}\" rx=\"{surface_corner}\" fill=\"url(#boardBg)\"/>
  <rect x=\"{surface_x}\" y=\"{surface_y}\" width=\"{surface_size}\" height=\"{surface_size}\" rx=\"{surface_corner}\" fill=\"url(#boardGlowA)\"/>
  <rect x=\"{surface_x}\" y=\"{surface_y}\" width=\"{surface_size}\" height=\"{surface_size}\" rx=\"{surface_corner}\" fill=\"url(#boardGlowB)\"/>
"""
    art = f"""  {background}
  {board_surface}
  {board_slots(geometry, board_scale)}
  {placed_blocks(geometry)}"""
    content = art if clip_shape is None else f"  <g clip-path=\"url(#iconMask)\">\n{art}\n  </g>"
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
{clip_definition}
  </defs>
{content}
</svg>
"""


def feature_graphic_svg_content() -> str:
    # 1024x500, ortada 400x400 logo, arka plan uzatılmış
    # Ortalamak için: (1024-400)/2 = 312, (500-400)/2 = 50
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500" fill="none">
  <defs>
    <linearGradient id="featureBg" x1="132" y1="92" x2="884" y2="436" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#152434"/>
      <stop offset="0.48" stop-color="#0C1621"/>
      <stop offset="1" stop-color="#070D14"/>
    </linearGradient>
    <radialGradient id="featureGlowA" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(232 114) rotate(45) scale(448)">
      <stop stop-color="#48D8C8" stop-opacity="0.18"/>
      <stop offset="1" stop-color="#48D8C8" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="featureGlowB" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(796 386) rotate(45) scale(404)">
      <stop stop-color="#6B74FF" stop-opacity="0.14"/>
      <stop offset="1" stop-color="#6B74FF" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1024" height="500" fill="url(#featureBg)"/>
  <rect width="1024" height="500" fill="url(#featureGlowA)"/>
  <rect width="1024" height="500" fill="url(#featureGlowB)"/>
  <g transform="translate(312, 50) scale(0.390625)">
    {board_icon_svg(include_background=False)}
  </g>
</svg>'''


def board_geometry(board_scale: float = 1.0) -> dict[str, int]:
    base_board_size = 636
    board_size = round(base_board_size * board_scale)
    board_x = (1024 - board_size) // 2
    board_y = (1024 - board_size) // 2
    grid_size = 4
    gap = max(8, round(12 * board_scale))
    cell_size = (board_size - ((grid_size - 1) * gap)) // grid_size
    return {
        "board_x": board_x,
        "board_y": board_y,
        "board_size": board_size,
        "grid_size": grid_size,
        "gap": gap,
        "cell_size": cell_size,
    }


def board_slots(geometry: dict[str, int], board_scale: float) -> str:
    lines: list[str] = []
    slot_corner = max(18, round(26 * board_scale))
    slot_stroke_width = round(max(1.5, 2.5 * board_scale) * 10) / 10
    for row in range(geometry["grid_size"]):
        for column in range(geometry["grid_size"]):
            x = geometry["board_x"] + column * (geometry["cell_size"] + geometry["gap"])
            y = geometry["board_y"] + row * (geometry["cell_size"] + geometry["gap"])
            # Glass effect for slots
            lines.append(
                f'  <rect x="{x}" y="{y}" width="{geometry["cell_size"]}" height="{geometry["cell_size"]}" rx="{slot_corner}" fill="#FFFFFF" fill-opacity="0.08"/>'
            )
            lines.append(
                f'  <rect x="{x}" y="{y}" width="{geometry["cell_size"]}" height="{geometry["cell_size"]}" rx="{slot_corner}" stroke="#FFFFFF" stroke-opacity="0.15" stroke-width="{slot_stroke_width}"/>'
            )
    return "\n".join(lines)


def placed_blocks(geometry: dict[str, int]) -> str:
    # 4x4 Grid
    # Colors
    c_cyan = "#00BBF9"
    c_gold = "#FFBE0B"
    c_violet = "#9B5DE5"
    c_emerald = "#00F5D4"
    c_coral = "#FF5C8A"

    placements = [
        ((0, 0), c_cyan, None),
        ((0, 1), c_cyan, "row"),
        ((1, 1), c_cyan, None),

        ((0, 3), c_gold, "heavy"),
        ((1, 3), c_gold, None),

        ((2, 0), c_violet, "ghost"),
        ((3, 0), c_violet, None),
        ((3, 1), c_violet, None),

        ((2, 2), c_emerald, "column"),
        ((3, 2), c_emerald, None),

        ((2, 3), c_coral, None),
    ]

    return "\n".join(
        tile_block(
            x=geometry["board_x"] + column * (geometry["cell_size"] + geometry["gap"]),
            y=geometry["board_y"] + row * (geometry["cell_size"] + geometry["gap"]),
            color=color,
            size=geometry["cell_size"],
            special=special,
        )
        for (row, column), color, special in placements
    )


def tile_block(x: int, y: int, color: str, size: int = 150, special: str | None = None) -> str:
    corner = round(size * 0.21)
    overlay = "" if special is None else special_overlay(x=x, y=y, size=size, kind=special)
    return f"""
  <g>
    <rect x=\"{x}\" y=\"{y}\" width=\"{size}\" height=\"{size}\" rx=\"{corner}\" fill=\"{color}\"/>
    <rect x=\"{x + round(size * 0.08)}\" y=\"{y + round(size * 0.08)}\" width=\"{round(size * 0.84)}\" height=\"{round(size * 0.30)}\" rx=\"{round(size * 0.12)}\" fill=\"#FFFFFF\" fill-opacity=\"0.20\"/>
{overlay}
  </g>"""


def special_overlay(x: int, y: int, size: int, kind: str) -> str:
    # All provided icons have a 24x24 viewport
    # We want to scale them to fit nicely inside the block
    icon_padding = size * 0.20
    icon_display_size = size - (icon_padding * 2)
    scale = icon_display_size / 24.0

    # Calculate offset to center the 24x24 icon in the size x size block
    offset_x = x + (size - (24.0 * scale)) / 2.0
    offset_y = y + (size - (24.0 * scale)) / 2.0

    paths = {
        "column": "M16,17.01L16,10h-2v7.01h-3L15,21l4,-3.99h-3zM9,3L5,6.99h3L8,14h2L10,6.99h3L9,3zM16,17.01L16,10h-2v7.01h-3L15,21l4,-3.99h-3zM9,3L5,6.99h3L8,14h2L10,6.99h3L9,3z",
        "row": "M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z",
        "ghost": "M11.99,18.54l-7.37,-5.73L3,14.07l9,7 9,-7 -1.63,-1.27zM12,16l7.36,-5.73L21,9l-9,-7 -9,7 1.63,1.27L12,16zM12,4.53L17.74,9 12,13.47 6.26,9 12,4.53z",
        "heavy": "M21,6.5c-1.66,0 -3,1.34 -3,3c0,0.07 0,0.14 0.01,0.21l-2.03,0.68c-0.64,-1.21 -1.82,-2.09 -3.22,-2.32V5.91C14.04,5.57 15,4.4 15,3c0,-1.66 -1.34,-3 -3,-3S9,1.34 9,3c0,1.4 0.96,2.57 2.25,2.91v2.16c-1.4,0.23 -2.58,1.11 -3.22,2.32L5.99,9.71C6,9.64 6,9.57 6,9.5c0,-1.66 -1.34,-3 -3,-3s-3,1.34 -3,3s1.34,3 3,3c1.06,0 1.98,-0.55 2.52,-1.37l2.03,0.68c-0.2,1.29 0.17,2.66 1.09,3.69l-1.41,1.77C6.85,17.09 6.44,17 6,17c-1.66,0 -3,1.34 -3,3s1.34,3 3,3s3,-1.34 3,-3c0,-0.68 -0.22,-1.3 -0.6,-1.8l1.41,-1.77c1.36,0.76 3.02,0.75 4.37,0l1.41,1.77C15.22,18.7 15,19.32 15,20c0,1.66 1.34,3 3,3s3,-1.34 3,-3s-1.34,-3 -3,-3c-0.44,0 -0.85,0.09 -1.23,0.26l-1.41,-1.77c0.93,-1.04 1.29,-2.4 1.09,-3.69l2.03,-0.68c0.53,0.82 1.46,1.37 2.52,1.37c1.66,0 3,-1.34 3,-3S22.66,6.5 21,6.5zM3,10.5c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C4,10.05 3.55,10.5 3,10.5zM6,21c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C7,20.55 6.55,21 6,21zM11,3c0,-0.55 0.45,-1 1,-1s1,0.45 1,1c0,0.55 -0.45,1 -1,1S11,3.55 11,3zM12,15c-1.38,0 -2.5,-1.12 -2.5,-2.5c0,-1.38 1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5C14.5,13.88 13.38,15 12,15zM18,19c0.55,0 1,0.45 1,1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1C17,19.45 17.45,19 18,19zM21,10.5c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C22,10.05 21.55,10.5 21,10.5z"
    }

    path_data = paths.get(kind, "")
    if not path_data:
        return ""

    return f'<path d="{path_data}" fill="#FFFFFF" fill-opacity="0.94" transform="translate({offset_x}, {offset_y}) scale({scale})"/>'


if __name__ == "__main__":
    main()

