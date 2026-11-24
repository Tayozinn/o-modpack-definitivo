"""Generates Mythic Contracts 2.1 Mono 44.1 kHz Vorbis (.ogg) audio assets & sounds.json."""
from __future__ import annotations

import json
import math
import random
import struct
import subprocess
import wave
from pathlib import Path

import imageio_ffmpeg

ROOT = Path(__file__).resolve().parent.parent
ASSETS_DIR = ROOT / "src" / "main" / "resources" / "assets" / "mythic_contracts"
SOUNDS_DIR = ASSETS_DIR / "sounds"
SOUNDS_JSON = ASSETS_DIR / "sounds.json"
SR = 44100


def env_ad(t: float, dur: float, attack: float = 0.015, release: float = 0.12) -> float:
    if t < 0.0 or t > dur:
        return 0.0
    a = min(1.0, t / max(1e-4, attack))
    r = min(1.0, max(0.0, (dur - t) / max(1e-4, release)))
    return a * r


def exp_decay(t: float, rate: float) -> float:
    return math.exp(-max(0.0, t) * rate)


def soft_clip(x: float, drive: float = 1.0) -> float:
    return math.tanh(x * drive)


class NoiseGen:
    def __init__(self, seed: int):
        self.rng = random.Random(seed)
        self.lp = 0.0
        self.bp_prev = 0.0

    def white(self) -> float:
        return self.rng.uniform(-1.0, 1.0)

    def lowpass(self, alpha: float) -> float:
        w = self.white()
        self.lp = self.lp + alpha * (w - self.lp)
        return self.lp

    def bandpass(self, alpha_hi: float, alpha_lo: float) -> float:
        lp = self.lowpass(alpha_lo)
        hp = lp - self.bp_prev
        self.bp_prev = self.bp_prev + alpha_hi * (lp - self.bp_prev)
        return hp


def bell_partials(t: float, base_hz: float, decay_scale: float = 1.0) -> float:
    ratios = (1.0, 2.0, 2.756, 3.98, 5.4)
    amps = (0.50, 0.28, 0.14, 0.08, 0.04)
    acc = 0.0
    for idx, (ratio, amp) in enumerate(zip(ratios, amps)):
        acc += amp * math.sin(2.0 * math.pi * base_hz * ratio * t) * exp_decay(t, (2.4 + idx * 1.3) * decay_scale)
    return acc


def gong_partials(t: float, base_hz: float) -> float:
    ratios = (0.5, 1.0, 1.498, 2.02, 2.83)
    amps = (0.45, 0.55, 0.24, 0.16, 0.10)
    shimmer = 1.0 + 0.004 * math.sin(2.0 * math.pi * 5.2 * t)
    acc = 0.0
    for idx, (ratio, amp) in enumerate(zip(ratios, amps)):
        acc += amp * math.sin(2.0 * math.pi * base_hz * ratio * shimmer * t) * exp_decay(t, 1.8 + idx * 0.9)
    return acc


def chirp(t: float, f0: float, f1: float, dur: float) -> float:
    frac = max(0.0, min(1.0, t / max(1e-4, dur)))
    # Integral of linear frequency f0 + (f1 - f0) * frac
    phase = 2.0 * math.pi * (f0 * t + 0.5 * (f1 - f0) * (t * t) / max(1e-4, dur))
    return math.sin(phase)


def normalize_samples(samples: list[float], target_peak: float = 0.86) -> list[int]:
    peak = max((abs(s) for s in samples), default=0.0)
    scale = (target_peak / peak) if peak > 1e-6 else 1.0
    fade_samples = min(len(samples) // 12, int(SR * 0.008))
    out: list[int] = []
    n = len(samples)
    for i, s in enumerate(samples):
        fade = 1.0
        if i < fade_samples:
            fade = i / max(1, fade_samples)
        elif i >= n - fade_samples:
            fade = (n - 1 - i) / max(1, fade_samples)
        v = max(-1.0, min(1.0, s * scale * fade))
        out.append(int(round(v * 32767.0)))
    return out


def synth_buffer(dur: float, fn) -> list[int]:
    n = max(1, int(round(dur * SR)))
    samples = [fn(i / SR, dur) for i in range(n)]
    return normalize_samples(samples)


def gen_ra_cast() -> list[int]:
    ng = NoiseGen(101)
    def fn(t: float, d: float) -> float:
        horn = (0.55 * math.sin(2.0 * math.pi * 196.0 * t)
                + 0.25 * math.sin(2.0 * math.pi * 293.66 * t)
                + 0.14 * math.sin(2.0 * math.pi * 392.0 * t)) * env_ad(t, d, 0.08, 0.25)
        chime = bell_partials(max(0.0, t - 0.04), 587.33, 1.3) + bell_partials(max(0.0, t - 0.14), 880.0, 1.6)
        whoosh = ng.bandpass(0.06, 0.22) * env_ad(t, 0.35, 0.03, 0.18) * 0.4
        return soft_clip(horn + chime * 0.7 + whoosh)
    return synth_buffer(0.65, fn)


def gen_ra_impact() -> list[int]:
    ng = NoiseGen(102)
    def fn(t: float, d: float) -> float:
        sub = math.sin(2.0 * math.pi * (120.0 * exp_decay(t, 5.0) + 52.0) * t) * exp_decay(t, 4.5)
        bell = gong_partials(t, 293.66) * 0.8 + bell_partials(t, 1174.66, 1.4) * 0.45
        fire = ng.lowpass(0.28) * exp_decay(t, 5.5) * 0.55
        return soft_clip(sub * 0.8 + bell + fire, 1.2)
    return synth_buffer(0.80, fn)


def gen_morrigan_cast() -> list[int]:
    ng = NoiseGen(103)
    def fn(t: float, d: float) -> float:
        drone = (math.sin(2.0 * math.pi * 130.81 * t) * 0.5
                 + math.sin(2.0 * math.pi * 155.56 * t) * 0.35
                 + math.sin(2.0 * math.pi * 196.0 * t) * 0.25) * env_ad(t, d, 0.09, 0.28)
        wing = ng.bandpass(0.04, 0.18) * env_ad(t, 0.45, 0.05, 0.22) * 0.8
        chime = bell_partials(max(0.0, t - 0.06), 622.25, 1.5) * 0.5
        return soft_clip(drone + wing + chime)
    return synth_buffer(0.70, fn)


def gen_morrigan_reap() -> list[int]:
    ng = NoiseGen(104)
    def fn(t: float, d: float) -> float:
        slice_chirp = chirp(t, 1450.0, 260.0, 0.26) * exp_decay(t, 11.0) * 0.65
        steel = ng.bandpass(0.18, 0.45) * exp_decay(t, 9.0) * 0.85
        sub = chirp(t, 155.0, 48.0, 0.30) * exp_decay(t, 4.8) * 0.85
        bell = gong_partials(max(0.0, t - 0.03), 174.61) * 0.55
        return soft_clip(slice_chirp + steel + sub + bell, 1.25)
    return synth_buffer(0.75, fn)


def gen_indra_charge() -> list[int]:
    ng = NoiseGen(105)
    def fn(t: float, d: float) -> float:
        rumble = ng.lowpass(0.05) * env_ad(t, d, 0.05, 0.18) * 0.75
        arc = chirp(t, 240.0, 1280.0, d) * env_ad(t, d, 0.12, 0.10) * 0.42
        buzz = math.sin(2.0 * math.pi * 120.0 * t) * math.sin(2.0 * math.pi * 27.0 * t) * env_ad(t, d, 0.08, 0.12) * 0.25
        return soft_clip(rumble + arc + buzz)
    return synth_buffer(0.65, fn)


def gen_indra_thunder() -> list[int]:
    ng = NoiseGen(106)
    def fn(t: float, d: float) -> float:
        snap = ng.white() * exp_decay(t, 42.0) * 0.9
        crack = ng.bandpass(0.12, 0.52) * exp_decay(t, 9.5) * 0.75
        thunder = ng.lowpass(0.04) * exp_decay(t, 2.2) * 1.15
        sub = chirp(t, 140.0, 42.0, 0.28) * exp_decay(t, 3.6) * 0.9
        spear = bell_partials(t, 523.25, 1.3) * 0.42
        return soft_clip(snap + crack + thunder + sub + spear, 1.35)
    return synth_buffer(0.95, fn)


def gen_tezca_step() -> list[int]:
    ng = NoiseGen(107)
    def fn(t: float, d: float) -> float:
        blink = chirp(t, 980.0, 340.0, 0.18) * exp_decay(t, 14.0) * 0.55
        glass = bell_partials(t, 1318.5, 2.5) * 0.45 + bell_partials(t + 0.02, 1760.0, 2.8) * 0.28
        whoosh = ng.bandpass(0.09, 0.34) * exp_decay(t, 11.0) * 0.65
        return soft_clip(blink + glass + whoosh)
    return synth_buffer(0.45, fn)


def gen_tezca_slash() -> list[int]:
    ng = NoiseGen(108)
    def fn(t: float, d: float) -> float:
        s1 = chirp(t, 1600.0, 420.0, 0.18) * exp_decay(t, 14.0)
        s2 = chirp(max(0.0, t - 0.11), 1850.0, 520.0, 0.18) * exp_decay(max(0.0, t - 0.11), 14.0) * (1.0 if t > 0.11 else 0.0)
        steel = ng.bandpass(0.20, 0.55) * (exp_decay(t, 12.0) + (exp_decay(t - 0.11, 12.0) if t > 0.11 else 0.0)) * 0.7
        sub = math.sin(2.0 * math.pi * 88.0 * t) * exp_decay(t, 7.0) * 0.55
        return soft_clip(s1 * 0.55 + s2 * 0.55 + steel + sub, 1.2)
    return synth_buffer(0.60, fn)


def gen_divine_overload() -> list[int]:
    ng = NoiseGen(109)
    def fn(t: float, d: float) -> float:
        shatter = (bell_partials(t, 932.33, 2.2) + bell_partials(t, 987.77, 2.4)) * 0.5
        drop = chirp(t, 280.0, 58.0, 0.70) * exp_decay(t, 2.9) * 0.75
        grit = ng.bandpass(0.06, 0.28) * exp_decay(t, 6.0) * 0.55
        return soft_clip(shatter + drop + grit, 1.15)
    return synth_buffer(0.90, fn)


def gen_clash_start() -> list[int]:
    ng = NoiseGen(201)
    def fn(t: float, d: float) -> float:
        gong = gong_partials(t, 220.0) * 0.75
        tether = chirp(t, 330.0, 660.0, 0.55) * env_ad(t, 0.65, 0.05, 0.25) * 0.45
        chain = ng.bandpass(0.22, 0.60) * env_ad(t, 0.40, 0.01, 0.20) * 0.4
        return soft_clip(gong + tether + chain)
    return synth_buffer(0.85, fn)


def gen_clash_loop() -> list[int]:
    dur = 2.0
    n = int(round(dur * SR))
    ng = NoiseGen(202)
    raw = []
    # Exact integer cycles per 2.0s ensure zero-phase discontinuity at loop boundary
    f1, f2, f3 = 110.0, 165.0, 220.0
    pulse_hz = 2.0
    for i in range(n):
        t = i / SR
        lfo = 0.72 + 0.28 * math.sin(2.0 * math.pi * pulse_hz * t)
        drone = (0.55 * math.sin(2.0 * math.pi * f1 * t)
                 + 0.30 * math.sin(2.0 * math.pi * f2 * t)
                 + 0.16 * math.sin(2.0 * math.pi * f3 * t)
                 + 0.08 * math.sin(2.0 * math.pi * 440.0 * t)) * lfo
        hiss = ng.bandpass(0.04, 0.14) * 0.14 * lfo
        raw.append(soft_clip(drone * 0.65 + hiss))
    # Crossfade edges gently for extra loop smoothness without dip
    cf = int(SR * 0.06)
    for i in range(cf):
        w = i / cf
        a = raw[i]
        b = raw[n - cf + i]
        mixed = a * w + b * (1.0 - w)
        raw[i] = mixed
    peak = max((abs(s) for s in raw), default=1.0)
    scale = 0.75 / peak
    return [int(round(max(-1.0, min(1.0, s * scale)) * 32767.0)) for s in raw]


def gen_clash_revolt() -> list[int]:
    ng = NoiseGen(203)
    def fn(t: float, d: float) -> float:
        rattle = ng.bandpass(0.28, 0.68) * (exp_decay(t, 14.0) + (exp_decay(t - 0.09, 14.0) if t > 0.09 else 0.0)) * 0.65
        alarm = (math.sin(2.0 * math.pi * 311.13 * t) + math.sin(2.0 * math.pi * 440.0 * t)) * exp_decay(t, 6.0) * 0.45
        thump = chirp(t, 130.0, 55.0, 0.20) * exp_decay(t, 8.0) * 0.6
        return soft_clip(rattle + alarm + thump, 1.2)
    return synth_buffer(0.55, fn)


def gen_clash_suppress() -> list[int]:
    ng = NoiseGen(204)
    def fn(t: float, d: float) -> float:
        lock = bell_partials(t, 440.0, 1.8) * 0.6 + bell_partials(t, 659.25, 2.0) * 0.4
        snap = math.sin(2.0 * math.pi * 165.0 * t) * exp_decay(t, 12.0) * 0.65
        air = ng.bandpass(0.14, 0.35) * exp_decay(t, 16.0) * 0.35
        return soft_clip(lock + snap + air)
    return synth_buffer(0.45, fn)


def gen_clash_success() -> list[int]:
    def fn(t: float, d: float) -> float:
        gong = gong_partials(t, 196.0) * 0.75
        n1 = bell_partials(max(0.0, t - 0.02), 392.0, 1.2) * (1.0 if t >= 0.02 else 0.0)
        n2 = bell_partials(max(0.0, t - 0.14), 587.33, 1.2) * (1.0 if t >= 0.14 else 0.0)
        n3 = bell_partials(max(0.0, t - 0.26), 783.99, 1.1) * (1.0 if t >= 0.26 else 0.0)
        return soft_clip(gong + (n1 + n2 + n3) * 0.52)
    return synth_buffer(1.10, fn)


def gen_clash_fail() -> list[int]:
    ng = NoiseGen(206)
    def fn(t: float, d: float) -> float:
        snap = ng.bandpass(0.20, 0.52) * exp_decay(t, 11.0) * 0.65
        fall = chirp(t, 370.0, 110.0, 0.55) * exp_decay(t, 4.2) * 0.55
        chord = bell_partials(t, 220.0, 1.8) * 0.4 + bell_partials(t, 261.63, 1.9) * 0.35
        return soft_clip(snap + fall + chord)
    return synth_buffer(0.75, fn)


def gen_servant_summon() -> list[int]:
    ng = NoiseGen(207)
    def fn(t: float, d: float) -> float:
        chime = bell_partials(t, 523.25, 1.5) * 0.55 + bell_partials(max(0.0, t - 0.07), 783.99, 1.5) * 0.45
        whoosh = ng.bandpass(0.07, 0.25) * env_ad(t, 0.38, 0.03, 0.22) * 0.45
        return soft_clip(chime + whoosh)
    return synth_buffer(0.55, fn)


def gen_gale_step() -> list[int]:
    ng = NoiseGen(301)
    def fn(t: float, d: float) -> float:
        gust = ng.bandpass(0.08, 0.34) * env_ad(t, d, 0.02, 0.22) * 0.85
        flute = chirp(t, 640.0, 960.0, 0.25) * exp_decay(t, 9.0) * 0.38
        snap = math.sin(2.0 * math.pi * 190.0 * t) * exp_decay(t, 22.0) * 0.45
        return soft_clip(gust + flute + snap)
    return synth_buffer(0.42, fn)


def gen_lotus_breath() -> list[int]:
    ng = NoiseGen(302)
    def fn(t: float, d: float) -> float:
        bowl = (0.55 * math.sin(2.0 * math.pi * 432.0 * t)
                + 0.28 * math.sin(2.0 * math.pi * 648.0 * t)
                + 0.14 * math.sin(2.0 * math.pi * 864.0 * t)) * env_ad(t, d, 0.08, 0.35)
        breath = ng.lowpass(0.06) * env_ad(t, d, 0.10, 0.30) * 0.25
        sparkle = bell_partials(max(0.0, t - 0.12), 1296.0, 2.2) * 0.25
        return soft_clip(bowl + breath + sparkle)
    return synth_buffer(0.85, fn)


def gen_jade_guard() -> list[int]:
    def fn(t: float, d: float) -> float:
        clang = bell_partials(t, 493.88, 2.0) * 0.55 + bell_partials(t, 739.99, 2.2) * 0.40
        thud = math.sin(2.0 * math.pi * 115.0 * t) * exp_decay(t, 12.0) * 0.65
        return soft_clip(clang + thud)
    return synth_buffer(0.50, fn)


def gen_seismic_palm() -> list[int]:
    ng = NoiseGen(304)
    def fn(t: float, d: float) -> float:
        slap = ng.bandpass(0.16, 0.55) * exp_decay(t, 24.0) * 0.85
        sub = chirp(t, 145.0, 38.0, 0.35) * exp_decay(t, 4.4) * 1.15
        rumble = ng.lowpass(0.04) * exp_decay(t, 3.8) * 0.70
        return soft_clip(slap + sub + rumble, 1.3)
    return synth_buffer(0.68, fn)


def gen_golden_aegis() -> list[int]:
    def fn(t: float, d: float) -> float:
        dome = gong_partials(t, 329.63) * 0.65 + bell_partials(t, 659.25, 1.3) * 0.45
        hum = math.sin(2.0 * math.pi * 164.81 * t) * env_ad(t, d, 0.06, 0.30) * 0.35
        return soft_clip(dome + hum)
    return synth_buffer(0.75, fn)


def gen_blood_furnace() -> list[int]:
    ng = NoiseGen(306)
    def fn(t: float, d: float) -> float:
        drum1 = chirp(t, 140.0, 62.0, 0.16) * exp_decay(t, 8.0) * 0.8
        drum2 = chirp(max(0.0, t - 0.18), 160.0, 68.0, 0.16) * exp_decay(max(0.0, t - 0.18), 8.0) * (0.75 if t > 0.18 else 0.0)
        roar = ng.lowpass(0.14) * env_ad(t, d, 0.03, 0.25) * 0.65
        return soft_clip(drum1 + drum2 + roar, 1.2)
    return synth_buffer(0.65, fn)


def gen_soul_lance() -> list[int]:
    ng = NoiseGen(307)
    def fn(t: float, d: float) -> float:
        whistle = chirp(t, 1180.0, 480.0, 0.35) * exp_decay(t, 7.5) * 0.55
        spike = bell_partials(max(0.0, t - 0.08), 880.0, 1.6) * (0.6 if t > 0.08 else 0.0)
        air = ng.bandpass(0.18, 0.48) * exp_decay(t, 10.0) * 0.55
        return soft_clip(whistle + spike + air)
    return synth_buffer(0.62, fn)


def gen_ether_flash() -> list[int]:
    ng = NoiseGen(308)
    def fn(t: float, d: float) -> float:
        snap1 = chirp(t, 2200.0, 460.0, 0.14) * exp_decay(t, 18.0) * 0.7
        snap2 = chirp(max(0.0, t - 0.12), 2400.0, 520.0, 0.16) * exp_decay(max(0.0, t - 0.12), 16.0) * (0.7 if t > 0.12 else 0.0)
        sheen = ng.bandpass(0.24, 0.62) * exp_decay(t, 14.0) * 0.55
        return soft_clip(snap1 + snap2 + sheen)
    return synth_buffer(0.52, fn)


def gen_spirit_domain() -> list[int]:
    def fn(t: float, d: float) -> float:
        gong = gong_partials(t, 146.83) * 0.85
        pad = (math.sin(2.0 * math.pi * 293.66 * t) + 0.5 * math.sin(2.0 * math.pi * 440.0 * t)) * env_ad(t, d, 0.12, 0.40) * 0.4
        return soft_clip(gong + pad)
    return synth_buffer(1.05, fn)


def gen_undying_reversal() -> list[int]:
    def fn(t: float, d: float) -> float:
        rise = chirp(t, 220.0, 880.0, 0.45) * env_ad(t, 0.55, 0.06, 0.18) * 0.45
        chime1 = bell_partials(max(0.0, t - 0.15), 587.33, 1.3) * (0.5 if t > 0.15 else 0.0)
        chime2 = bell_partials(max(0.0, t - 0.25), 880.0, 1.2) * (0.5 if t > 0.25 else 0.0)
        heart = math.sin(2.0 * math.pi * 78.0 * t) * exp_decay(t, 6.0) * 0.7
        return soft_clip(rise + chime1 + chime2 + heart)
    return synth_buffer(0.85, fn)


def gen_heaven_sunder() -> list[int]:
    ng = NoiseGen(311)
    def fn(t: float, d: float) -> float:
        gong = gong_partials(t, 130.81) * 0.8
        crack = ng.white() * exp_decay(t, 36.0) * 0.8
        drop = chirp(t, 195.0, 40.0, 0.55) * exp_decay(t, 2.8) * 1.05
        bell = bell_partials(t, 523.25, 1.1) * 0.48 + bell_partials(t, 783.99, 1.2) * 0.35
        return soft_clip(gong + crack + drop + bell, 1.3)
    return synth_buffer(1.15, fn)


def gen_integrity_full() -> list[int]:
    def fn(t: float, d: float) -> float:
        n1 = bell_partials(t, 783.99, 2.2) * 0.55
        n2 = bell_partials(max(0.0, t - 0.11), 1046.50, 1.8) * (0.65 if t >= 0.11 else 0.0)
        return soft_clip(n1 + n2)
    return synth_buffer(0.55, fn)


def gen_integrity_shatter() -> list[int]:
    ng = NoiseGen(402)
    def fn(t: float, d: float) -> float:
        crack = ng.bandpass(0.22, 0.62) * exp_decay(t, 14.0) * 0.7
        crystal = (bell_partials(t, 698.46, 2.4) + bell_partials(t, 739.99, 2.5)) * 0.48
        drop = chirp(t, 240.0, 68.0, 0.45) * exp_decay(t, 5.2) * 0.55
        return soft_clip(crack + crystal + drop)
    return synth_buffer(0.65, fn)


def gen_slot_cycle() -> list[int]:
    def fn(t: float, d: float) -> float:
        click = bell_partials(t, 987.77, 6.5) * 0.65 + math.sin(2.0 * math.pi * 240.0 * t) * exp_decay(t, 28.0) * 0.5
        return soft_clip(click)
    return synth_buffer(0.18, fn)


SOUND_DEFS = [
    ("skill.ra_cast", "skill/ra_cast", gen_ra_cast, "subtitles.mythic_contracts.skill.ra_cast"),
    ("skill.ra_impact", "skill/ra_impact", gen_ra_impact, "subtitles.mythic_contracts.skill.ra_impact"),
    ("skill.morrigan_cast", "skill/morrigan_cast", gen_morrigan_cast, "subtitles.mythic_contracts.skill.morrigan_cast"),
    ("skill.morrigan_reap", "skill/morrigan_reap", gen_morrigan_reap, "subtitles.mythic_contracts.skill.morrigan_reap"),
    ("skill.indra_charge", "skill/indra_charge", gen_indra_charge, "subtitles.mythic_contracts.skill.indra_charge"),
    ("skill.indra_thunder", "skill/indra_thunder", gen_indra_thunder, "subtitles.mythic_contracts.skill.indra_thunder"),
    ("skill.tezca_step", "skill/tezca_step", gen_tezca_step, "subtitles.mythic_contracts.skill.tezca_step"),
    ("skill.tezca_slash", "skill/tezca_slash", gen_tezca_slash, "subtitles.mythic_contracts.skill.tezca_slash"),
    ("skill.divine_overload", "skill/divine_overload", gen_divine_overload, "subtitles.mythic_contracts.skill.divine_overload"),
    ("subjugation.clash_start", "subjugation/clash_start", gen_clash_start, "subtitles.mythic_contracts.subjugation.clash_start"),
    ("subjugation.clash_loop", "subjugation/clash_loop", gen_clash_loop, "subtitles.mythic_contracts.subjugation.clash_loop"),
    ("subjugation.clash_revolt", "subjugation/clash_revolt", gen_clash_revolt, "subtitles.mythic_contracts.subjugation.clash_revolt"),
    ("subjugation.clash_suppress", "subjugation/clash_suppress", gen_clash_suppress, "subtitles.mythic_contracts.subjugation.clash_suppress"),
    ("subjugation.clash_success", "subjugation/clash_success", gen_clash_success, "subtitles.mythic_contracts.subjugation.clash_success"),
    ("subjugation.clash_fail", "subjugation/clash_fail", gen_clash_fail, "subtitles.mythic_contracts.subjugation.clash_fail"),
    ("subjugation.servant_summon", "subjugation/servant_summon", gen_servant_summon, "subtitles.mythic_contracts.subjugation.servant_summon"),
    ("tech.gale_step", "tech/gale_step", gen_gale_step, "subtitles.mythic_contracts.tech.gale_step"),
    ("tech.lotus_breath", "tech/lotus_breath", gen_lotus_breath, "subtitles.mythic_contracts.tech.lotus_breath"),
    ("tech.jade_guard", "tech/jade_guard", gen_jade_guard, "subtitles.mythic_contracts.tech.jade_guard"),
    ("tech.seismic_palm", "tech/seismic_palm", gen_seismic_palm, "subtitles.mythic_contracts.tech.seismic_palm"),
    ("tech.golden_aegis", "tech/golden_aegis", gen_golden_aegis, "subtitles.mythic_contracts.tech.golden_aegis"),
    ("tech.blood_furnace", "tech/blood_furnace", gen_blood_furnace, "subtitles.mythic_contracts.tech.blood_furnace"),
    ("tech.soul_lance", "tech/soul_lance", gen_soul_lance, "subtitles.mythic_contracts.tech.soul_lance"),
    ("tech.ether_flash", "tech/ether_flash", gen_ether_flash, "subtitles.mythic_contracts.tech.ether_flash"),
    ("tech.spirit_domain", "tech/spirit_domain", gen_spirit_domain, "subtitles.mythic_contracts.tech.spirit_domain"),
    ("tech.undying_reversal", "tech/undying_reversal", gen_undying_reversal, "subtitles.mythic_contracts.tech.undying_reversal"),
    ("tech.heaven_sunder", "tech/heaven_sunder", gen_heaven_sunder, "subtitles.mythic_contracts.tech.heaven_sunder"),
    ("spirit.integrity_full", "spirit/integrity_full", gen_integrity_full, "subtitles.mythic_contracts.spirit.integrity_full"),
    ("spirit.integrity_shatter", "spirit/integrity_shatter", gen_integrity_shatter, "subtitles.mythic_contracts.spirit.integrity_shatter"),
    ("spirit.slot_cycle", "spirit/slot_cycle", gen_slot_cycle, "subtitles.mythic_contracts.spirit.slot_cycle"),
]


def main() -> None:
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    sounds_manifest: dict[str, dict] = {}
    for event_key, rel_path, gen_fn, subtitle_key in SOUND_DEFS:
        ogg_path = SOUNDS_DIR / f"{rel_path}.ogg"
        wav_path = SOUNDS_DIR / f"{rel_path}.tmp.wav"
        ogg_path.parent.mkdir(parents=True, exist_ok=True)

        pcm = gen_fn()
        with wave.open(str(wav_path), "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(SR)
            wf.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))

        subprocess.run(
            [ffmpeg, "-y", "-loglevel", "error", "-i", str(wav_path), "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "5", str(ogg_path)],
            check=True,
        )
        wav_path.unlink(missing_ok=True)

        sounds_manifest[event_key] = {
            "subtitle": subtitle_key,
            "sounds": [f"mythic_contracts:{rel_path}"],
        }
        print(f"Encoded {ogg_path.relative_to(ROOT)} ({ogg_path.stat().st_size} bytes)")

    SOUNDS_JSON.write_text(json.dumps(sounds_manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {SOUNDS_JSON.relative_to(ROOT)} ({len(sounds_manifest)} sound events)")


if __name__ == "__main__":
    main()
