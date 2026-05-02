#!/usr/bin/env python3
from __future__ import annotations

import base64
import shutil
import struct
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IOS_APPICON = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset"
IOS_LAUNCH_LOGO = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "LaunchLogo.imageset"
DESKTOP_RES = ROOT / "composeApp" / "src" / "jvmMain" / "resources"
DESKTOP_ICONS = ROOT / "composeApp" / "desktop-icons"
BRANDING_OUT = ROOT / "branding" / "generated"
NUNITO_BLACK = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "font" / "nunito_black.ttf"

BOARD_SVG_FINAL = "stackshift-board.svg"
BACKGROUND_SVG_FINAL = "stackshift-background.svg"
FEATURE_GRAPHIC_SVG_NAME = "stackshift-feature-graphic-1024x500.svg"
FEATURE_GRAPHIC_PNG_NAME = "stackshift-feature-graphic-1024x500.png"

FULL_SVG_NAME = "stackshift-app-icon.svg"
FOREGROUND_SVG_NAME = "stackshift-adaptive-foreground.svg"
MACOS_FOREGROUND_SVG_NAME = "stackshift-macos-foreground.svg"

LOGO_GRID_SIZE = 4

def main() -> None:
    ensure_tools()
    with tempfile.TemporaryDirectory(prefix="stackshift-icon-") as temp_dir:
        temp_path = Path(temp_dir)
        BRANDING_OUT.mkdir(parents=True, exist_ok=True)

        # 1. Android / Manual SVG Outputs
        (BRANDING_OUT / BOARD_SVG_FINAL).write_text(
            mosaic_icon_svg(include_background=False, include_art=True, logo_scale=0.8),
            encoding="utf-8"
        )
        (BRANDING_OUT / BACKGROUND_SVG_FINAL).write_text(
            mosaic_icon_svg(include_background=True, include_art=False),
            encoding="utf-8"
        )

        # 2. Google Play Feature Graphic
        feature_graphic_svg = BRANDING_OUT / FEATURE_GRAPHIC_SVG_NAME
        feature_graphic_svg.write_text(feature_graphic_svg_content(), encoding="utf-8")
        feature_graphic_png = BRANDING_OUT / FEATURE_GRAPHIC_PNG_NAME
        rasterize_svg(feature_graphic_svg, feature_graphic_png, 1024, height=500)

        # 3. iOS, macOS, Desktop Assets
        full_svg = temp_path / FULL_SVG_NAME
        foreground_svg = temp_path / FOREGROUND_SVG_NAME
        macos_foreground_svg = temp_path / MACOS_FOREGROUND_SVG_NAME

        full_svg.write_text(full_icon_svg(), encoding="utf-8")
        foreground_svg.write_text(foreground_icon_svg(), encoding="utf-8")
        macos_foreground_svg.write_text(macos_foreground_icon_svg(), encoding="utf-8")

        full_png = temp_path / "full-1024.png"
        foreground_png = temp_path / "foreground-1024.png"
        macos_foreground_png = temp_path / "macos-foreground-1024.png"

        rasterize_svg(full_svg, full_png, 1024)
        rasterize_svg(foreground_svg, foreground_png, 1024)
        rasterize_svg(macos_foreground_svg, macos_foreground_png, 1024)

        generate_ios_assets(full_png, foreground_png)
        generate_desktop_assets(full_png, macos_foreground_png)

    print(f"Generated assets in {BRANDING_OUT} and platform resource folders.")


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

        # 1. Create a square version of the SVG with the content centered
        # This ensures qlmanage renders it without any automatic offsets or stretching
        target_h = height if height is not None else size
        padding_top = (size - target_h) / 2

        original_svg_content = svg_path.read_text(encoding="utf-8")
        # Use base64 to embed the original SVG cleanly inside a square wrapper
        svg_base64 = base64.b64encode(original_svg_content.encode('utf-8')).decode('utf-8')

        square_svg_path = temp_path / "square_wrapper.svg"
        square_svg_content = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}">
            <image x="0" y="{padding_top}" width="{size}" height="{target_h}" href="data:image/svg+xml;base64,{svg_base64}"/>
        </svg>'''
        square_svg_path.write_text(square_svg_content, encoding="utf-8")

        # 2. Render the square SVG using QuickLook (respects embedded fonts)
        run("qlmanage", "-t", "-s", str(size), "-o", str(temp_path), str(square_svg_path))
        rendered_png = temp_path / f"{square_svg_path.name}.png"

        if not rendered_png.exists():
            # Fallback to direct sips if qlmanage fails
            run("sips", "-s", "format", "png", str(svg_path), "--resampleHeightWidth", str(target_h), str(size), "--out", str(output_png))
        else:
            # 3. Precise center crop to get back to the final dimensions (1024x500)
            run("sips", "-c", str(target_h), str(size), str(rendered_png), "--out", str(output_png))


def resize_png(source: Path, target: Path, size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    run("sips", "-z", str(size), str(size), str(source), "--out", str(target))


def generate_ios_assets(full_png: Path, foreground_png: Path) -> None:
    resize_png(full_png, IOS_APPICON / "app-icon-1024.png", 1024)
    resize_png(foreground_png, IOS_LAUNCH_LOGO / "launch-logo.png", 1024)


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
    return mosaic_icon_svg(
        include_background=True,
        logo_scale=0.64,
    )


def foreground_icon_svg() -> str:
    return mosaic_icon_svg(
        include_background=False,
        clip_shape="rounded_rect",
        logo_scale=0.45,
        clip_inset=56,
        clip_radius=192,
    )


def macos_foreground_icon_svg() -> str:
    return mosaic_icon_svg(
        include_background=False,
        clip_shape="rounded_rect",
        logo_scale=0.64,
        clip_inset=100,
        clip_radius=180,
    )


def mosaic_icon_svg(
    include_background: bool,
    include_art: bool = True,
    clip_shape: str | None = None,
    logo_scale: float = 1.0,
    logo_offset_x: int = 0,
    logo_offset_y: int = 0,
    clip_inset: int | None = None,
    clip_radius: int | None = None,
) -> str:
    geometry = logo_geometry(
        logo_scale=logo_scale,
        offset_x=logo_offset_x,
        offset_y=logo_offset_y,
    )
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

    art_content = "" if not include_art else f"""
  {mosaic_tiles(geometry)}"""

    art = f"""  {background}
{art_content}"""
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
  </defs>
  {background}
  {art_content}
</svg>
"""


def get_nunito_font_base64() -> str:
    print(f"Font loading attempt: {NUNITO_BLACK}")
    if NUNITO_BLACK.exists():
        data = NUNITO_BLACK.read_bytes()
        encoded = base64.b64encode(data).decode('utf-8')
        print(f"Font successfully loaded. Size: {len(data)} bytes")
        return encoded
    return ""


def feature_graphic_svg_content() -> str:
    font_base64 = get_nunito_font_base64()
    font_style = f"""
    <style type="text/css">
      @font-face {{
        font-family: 'NunitoBlack';
        src: url(data:font/ttf;base64,{font_base64}) format('truetype');
      }}
    </style>""" if font_base64 else ""

    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500" fill="none">
  {font_style}
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
    <linearGradient id="textGradient" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#FF4D6D"/>
      <stop offset="25%" stop-color="#A2FF00"/>
      <stop offset="50%" stop-color="#00D2FF"/>
      <stop offset="75%" stop-color="#6B74FF"/>
      <stop offset="100%" stop-color="#FFEA00"/>
    </linearGradient>
  </defs>
  <rect width="1024" height="500" fill="url(#featureBg)"/>
  <rect width="1024" height="500" fill="url(#featureGlowA)"/>
  <rect width="1024" height="500" fill="url(#featureGlowB)"/>
  <text x="160" y="288" font-family="'NunitoBlack', sans-serif" font-weight="900" font-size="90" fill="url(#textGradient)" stroke="url(#textGradient)" stroke-width="4" text-anchor="middle">STACK</text>
  <text x="864" y="288" font-family="'NunitoBlack', sans-serif" font-weight="900" font-size="90" fill="url(#textGradient)" stroke="url(#textGradient)" stroke-width="4" text-anchor="middle">SHIFT</text>
  <g transform="translate(312, 50) scale(0.390625)">
    {mosaic_icon_svg(include_background=False, logo_scale=0.8)}
  </g>
</svg>'''


def logo_geometry(logo_scale: float = 1.0, offset_x: int = 0, offset_y: int = 0) -> dict[str, int]:
    base_logo_size = 1024
    logo_size = round(base_logo_size * logo_scale)
    logo_x = ((1024 - logo_size) // 2) + offset_x
    logo_y = ((1024 - logo_size) // 2) + offset_y
    grid_size = LOGO_GRID_SIZE
    gap = max(10, round(12 * logo_scale))
    cell_size = (logo_size - ((grid_size - 1) * gap)) // grid_size
    return {
        "logo_x": logo_x,
        "logo_y": logo_y,
        "logo_size": logo_size,
        "grid_size": grid_size,
        "gap": gap,
        "cell_size": cell_size,
    }


def mosaic_tiles(geometry: dict[str, int]) -> str:
    c_pink = "#FF4D6D"
    c_lime = "#A2FF00"
    c_cyan = "#00D2FF"
    c_gold = "#FFEA00"
    c_purple = "#6B74FF"

    tile_layout: list[list[dict[str, str] | None]] = [
        [{"color": c_purple}, {"color": c_purple}, {"color": c_pink}, {"color": c_pink}],
        [{"color": c_lime}, {"color": c_lime}, None, {"color": c_pink}],
        [{"color": c_cyan}, None, None, {"color": c_pink}],
        [{"color": c_cyan}, None, None, None],
    ]

    lines: list[str] = []
    # Background slots
    for row in range(geometry["grid_size"]):
        for column in range(geometry["grid_size"]):
            x = geometry["logo_x"] + column * (geometry["cell_size"] + geometry["gap"])
            y = geometry["logo_y"] + row * (geometry["cell_size"] + geometry["gap"])
            lines.append(empty_slot(x=x, y=y, size=geometry["cell_size"]))

    # Draw static board tiles
    for row, cells in enumerate(tile_layout):
        for column, cell in enumerate(cells):
            if cell:
                x = geometry["logo_x"] + column * (geometry["cell_size"] + geometry["gap"])
                y = geometry["logo_y"] + row * (geometry["cell_size"] + geometry["gap"])
                lines.append(tile_block(x=x, y=y, color=cell["color"], size=geometry["cell_size"]))

    # Define falling pieces as distinct units
    pieces = [
        {"color": c_gold, "blocks": [(1, 1.95), (2, 1.95), (2, 0.95)], "angle": 0},
    ]

    for i, p in enumerate(pieces):
        # 1. Create a mask that is the union of all shadow blocks for this piece
        mask_id = f"shadow_mask_ss_{i}"
        size = geometry["cell_size"]
        gap = geometry["gap"]
        glow = size * 0.38
        offset_y = size * 0.25
        corner = round(size * 0.21)

        lines.append(f'  <defs><mask id="{mask_id}">')
        for b_col, b_row in p["blocks"]:
            x = geometry["logo_x"] + b_col * (size + gap)

            # Custom starting points: 2x3 top (idx 1) and 3x2 top (idx 2)
            if round(b_col) == 1:
                y_start = geometry["logo_y"] + 2 * (size + gap)
            elif round(b_col) == 2:
                y_start = geometry["logo_y"] + 1 * (size + gap)
            else:
                y_start = geometry["logo_y"] + b_row * (size + gap) + offset_y

            # Extend height to the bottom of the 1024x1024 canvas
            shadow_h = 1024 - y_start
            lines.append(f'    <rect x="{x}" y="{y_start}" width="{size}" height="{shadow_h}" rx="{corner}" fill="white"/>')
        lines.append('  </mask></defs>')

        # 2. Draw a SINGLE rectangle for the entire piece's shadow area using the mask
        # This ensures zero internal overlaps and a perfectly flat 0.25 opacity
        lines.append(f'  <rect x="0" y="0" width="1024" height="1024" fill="{p["color"]}" opacity="0.25" mask="url(#{mask_id})"/>')

        # 3. Draw foreground blocks
        for b_col, b_row in p["blocks"]:
            x = geometry["logo_x"] + b_col * (size + gap)
            y = geometry["logo_y"] + b_row * (size + gap)
            lines.append(tile_block(x=x, y=y, color=p["color"], size=size, falling=True, angle=p["angle"], include_shadow=False))

    return "\n".join(lines)


def empty_slot(x: int, y: int, size: int = 150) -> str:
    corner = round(size * 0.21)
    return f"""
  <rect x=\"{x}\" y=\"{y}\" width=\"{size}\" height=\"{size}\" rx=\"{corner}\" fill=\"#FFFFFF\" fill-opacity=\"0.05\" stroke=\"#FFFFFF\" stroke-opacity=\"0.1\" stroke-width=\"2\"/>"""


def tile_block(x: int, y: int, color: str, size: int = 150, special: str | None = None, falling: bool = False, angle: float = 0, include_shadow: bool = True) -> str:
    corner = round(size * 0.21)
    special_art = "" if special is None else special_overlay(x=x, y=y, size=size, kind=special)

    if falling:
        offset_y = size * 0.28
        cx = x + size / 2
        cy = y + size / 2
        transform = f'transform="translate({cx}, {cy + offset_y}) rotate({angle}) translate({-size/2}, {-size/2})"'

        # Glow/Shadow around for depth
        glow_size = size * 0.18
        trail = f'<rect x="{-glow_size}" y="{-glow_size}" width="{size + 2*glow_size}" height="{size + 2*glow_size}" rx="{corner*1.2}" fill="{color}" opacity="0.25"/>' if include_shadow else ""

        return f"""
  <g {transform}>
    {trail}
    <rect width="{size}" height="{size}" rx="{corner}" fill="{color}"/>
    {special_art}
  </g>"""

    return f"""
  <g>
    <rect x="{x}" y="{y}" width="{size}" height="{size}" rx="{corner}" fill="{color}"/>
    {special_art}
  </g>"""


def special_overlay(x: int, y: int, size: int, kind: str) -> str:
    icon_padding = size * 0.20
    icon_display_size = size - (icon_padding * 2)
    scale = icon_display_size / 24.0

    offset_x = x + (size - (24.0 * scale)) / 2.0
    offset_y = y + (size - (24.0 * scale)) / 2.0

    paths = {
        "column_clearer": "M16,17.01L16,10h-2v7.01h-3L15,21l4,-3.99h-3zM9,3L5,6.99h3L8,14h2L10,6.99h3L9,3z",
        "row_clearer": "M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z",
        "ghost": "M11.99,18.54l-7.37,-5.73L3,14.07l9,7 9,-7 -1.63,-1.27zM12,16l7.36,-5.73L21,9l-9,-7 -9,7 1.63,1.27L12,16zM12,4.53L17.74,9 12,13.47 6.26,9 12,4.53z",
        "heavy": "M21,6.5c-1.66,0 -3,1.34 -3,3c0,0.07 0,0.14 0.01,0.21l-2.03,0.68c-0.64,-1.21 -1.82,-2.09 -3.22,-2.32V5.91C14.04,5.57 15,4.4 15,3c0,-1.66 -1.34,-3 -3,-3S9,1.34 9,3c0,1.4 0.96,2.57 2.25,2.91v2.16c-1.4,0.23 -2.58,1.11 -3.22,2.32L5.99,9.71C6,9.64 6,9.57 6,9.5c0,-1.66 -1.34,-3 -3,-3s-3,1.34 -3,3s1.34,3 3,3c1.06,0 1.98,-0.55 2.52,-1.37l2.03,0.68c-0.2,1.29 0.17,2.66 1.09,3.69l-1.41,1.77C6.85,17.09 6.44,17 6,17c-1.66,0 -3,1.34 -3,3s1.34,3 3,3s3,-1.34 3,-3c0,-0.68 -0.22,-1.3 -0.6,-1.8l1.41,-1.77c1.36,0.76 3.02,0.75 4.37,0l1.41,1.77C15.22,18.7 15,19.32 15,20c0,1.66 1.34,3 3,3s3,-1.34 3,-3s-1.34,-3 -3,-3c-0.44,0 -0.85,0.09 -1.23,0.26l-1.41,-1.77c0.93,-1.04 1.29,-2.4 1.09,-3.69l2.03,-0.68c0.53,0.82 1.46,1.37 2.52,1.37c1.66,0 3,-1.34 3,-3S22.66,6.5 21,6.5zM3,10.5c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C4,10.05 3.55,10.5 3,10.5zM6,21c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C7,20.55 6.55,21 6,21zM11,3c0,-0.55 0.45,-1 1,-1s1,0.45 1,1c0,0.55 -0.45,1 -1,1S11,3.55 11,3zM12,15c-1.38,0 -2.5,-1.12 -2.5,-2.5c0,-1.38 1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5C14.5,13.88 13.38,15 12,15zM18,19c0.55,0 1,0.45 1,1c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1C17,19.45 17.45,19 18,19zM21,10.5c-0.55,0 -1,-0.45 -1,-1c0,-0.55 0.45,-1 1,-1s1,0.45 1,1C22,10.05 21.55,10.5 21,10.5z"
    }

    path_data = paths.get(kind, "")
    if not path_data:
        return ""

    return f'<path d="{path_data}" fill="#FFFFFF" transform="translate({offset_x}, {offset_y}) scale({scale})"/>'


if __name__ == "__main__":
    main()
