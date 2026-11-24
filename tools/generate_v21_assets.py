"""Generates Mythic Contracts 2.1 particle PNG sprites and JSON definitions using pure Python stdlib."""
from __future__ import annotations

import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEX_DIR = ROOT / "src" / "main" / "resources" / "assets" / "mythic_contracts" / "textures" / "particle"
PARTICLE_JSON_DIR = ROOT / "src" / "main" / "resources" / "assets" / "mythic_contracts" / "particles"


def clamp_u8(val: float) -> int:
    return max(0, min(255, int(round(val))))


def write_rgba_png(path: Path, width: int, height: int, pixels: list[tuple[int, int, int, int]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0 (None)
        row_start = y * width
        for x in range(width):
            r, g, b, a = pixels[row_start + x]
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), level=9)
    png_bytes = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")
    path.write_bytes(png_bytes)


def make_subjugation_rune_ring() -> list[tuple[int, int, int, int]]:
    w, h = 32, 32
    cx, cy = 15.5, 15.5
    out: list[tuple[int, int, int, int]] = []
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            dist = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)

            outer_ring = math.exp(-((dist - 12.2) ** 2) / 1.4)
            inner_ring = math.exp(-((dist - 7.8) ** 2) / 1.0)
            rune_ticks = max(0.0, math.cos(ang * 8.0)) ** 3 * math.exp(-((dist - 10.0) ** 2) / 1.8)
            cardinal = max(0.0, math.cos(ang * 4.0)) ** 8 * math.exp(-((dist - 13.4) ** 2) / 1.2)

            alpha_f = min(1.0, outer_ring * 0.95 + inner_ring * 0.72 + rune_ticks * 0.78 + cardinal * 0.9)
            gold_mix = min(1.0, cardinal * 0.85 + inner_ring * 0.45)
            # Blend from luminous amethyst (#C084FC / #E9D5FF) to imperial gold (#FDE68A)
            r = clamp_u8(192 * (1.0 - gold_mix) + 253 * gold_mix + outer_ring * 35)
            g = clamp_u8(132 * (1.0 - gold_mix) + 224 * gold_mix + cardinal * 20)
            b = clamp_u8(252 * (1.0 - gold_mix) + 140 * gold_mix)
            a = clamp_u8(alpha_f * 255.0)
            out.append((r, g, b, a))
    return out


def make_subjugation_chain_link() -> list[tuple[int, int, int, int]]:
    w, h = 16, 16
    cx, cy = 7.5, 7.5
    out: list[tuple[int, int, int, int]] = []
    for y in range(h):
        for x in range(w):
            # Rotated oval distance for crystalline chain link
            dx = (x - cx) * 0.7071 - (y - cy) * 0.7071
            dy = (x - cx) * 0.7071 + (y - cy) * 0.7071
            ellipse_r = math.hypot(dx / 4.6, dy / 2.7)
            link_shell = math.exp(-((ellipse_r - 1.0) ** 2) / 0.085)
            core_glint = math.exp(-((dx ** 2 + dy ** 2) / 5.5)) * 0.35
            cross_bar = math.exp(-((abs(dy) ** 2) / 1.1)) * math.exp(-((dx ** 2) / 10.0)) * 0.65

            alpha_f = min(1.0, link_shell + cross_bar + core_glint)
            highlight = min(1.0, core_glint * 1.4 + link_shell * 0.45)
            r = clamp_u8(178 + highlight * 77)
            g = clamp_u8(118 + highlight * 116)
            b = clamp_u8(250 - cross_bar * 60)
            a = clamp_u8(alpha_f * 255.0)
            out.append((r, g, b, a))
    return out


def make_subjugation_sigil() -> list[tuple[int, int, int, int]]:
    w, h = 32, 32
    cx, cy = 15.5, 15.5
    out: list[tuple[int, int, int, int]] = []
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            # Axis diamond and rotated diamond (octagram)
            d1 = abs(dx) + abs(dy)
            rx = (dx - dy) * 0.7071
            ry = (dx + dy) * 0.7071
            d2 = abs(rx) + abs(ry)
            star1 = math.exp(-((d1 - 11.0) ** 2) / 1.7)
            star2 = math.exp(-((d2 - 11.0) ** 2) / 1.7)
            center_gem = math.exp(-((dx ** 2 + dy ** 2) / 9.5))
            radial = math.hypot(dx, dy)
            inner_circle = math.exp(-((radial - 5.2) ** 2) / 1.1)

            alpha_f = min(1.0, star1 * 0.9 + star2 * 0.8 + center_gem * 0.95 + inner_circle * 0.7)
            gold = min(1.0, center_gem * 0.8 + star1 * 0.35)
            r = clamp_u8(186 * (1.0 - gold) + 254 * gold)
            g = clamp_u8(116 * (1.0 - gold) + 230 * gold)
            b = clamp_u8(255 * (1.0 - gold) + 170 * gold)
            a = clamp_u8(alpha_f * 255.0)
            out.append((r, g, b, a))
    return out


def make_qi_ribbon_wisp() -> list[tuple[int, int, int, int]]:
    w, h = 16, 16
    cx, cy = 7.5, 7.5
    out: list[tuple[int, int, int, int]] = []
    for y in range(h):
        for x in range(w):
            dist = math.hypot(x - cx, y - cy)
            glow = math.exp(-(dist ** 2) / 11.0)
            core = math.exp(-(dist ** 2) / 2.8)
            val = min(1.0, glow * 0.75 + core * 0.55)
            # Neutral luminous white-ivory for clean RGB tinting in MythicParticleBase
            c = clamp_u8(225 + core * 30)
            a = clamp_u8(val * 255.0)
            out.append((c, c, 255, a))
    return out


def write_particle_json(name: str) -> None:
    PARTICLE_JSON_DIR.mkdir(parents=True, exist_ok=True)
    payload = {"textures": [f"mythic_contracts:{name}"]}
    (PARTICLE_JSON_DIR / f"{name}.json").write_text(
        json.dumps(payload, indent=2) + "\n", encoding="utf-8"
    )


def main() -> None:
    specs = [
        ("subjugation_rune_ring", 32, 32, make_subjugation_rune_ring()),
        ("subjugation_chain_link", 16, 16, make_subjugation_chain_link()),
        ("subjugation_sigil", 32, 32, make_subjugation_sigil()),
        ("qi_ribbon_wisp", 16, 16, make_qi_ribbon_wisp()),
    ]
    for name, w, h, pixels in specs:
        png_path = TEX_DIR / f"{name}.png"
        write_rgba_png(png_path, w, h, pixels)
        write_particle_json(name)
        print(f"Generated {png_path.relative_to(ROOT)} ({w}x{h}) and particles/{name}.json")


if __name__ == "__main__":
    main()
