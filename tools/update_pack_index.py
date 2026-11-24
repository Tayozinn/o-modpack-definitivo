#!/usr/bin/env python3
"""
Atualiza automaticamente:
1. index.toml e pack.toml (Packwiz SHA-256) incluindo config/, defaultconfigs/, kubejs/ e mods/ (22 .pw.toml + mythic_contracts-2.0.0.jar).
2. O_Modpack_Definitivo_Fase3.mrpack (instalador Modrinth completo).
3. Instância local do Prism Launcher (%APPDATA%/PrismLauncher/instances/O_Modpack_Definitivo*), se existir na máquina.
"""

import hashlib
import os
import pathlib
import shutil
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAR_NAME = "mythic_contracts-2.1.0.jar"


def replace_core_jar(dest_dir: pathlib.Path, source_jar: pathlib.Path) -> None:
    if not source_jar.exists():
        return
    dest_dir.mkdir(parents=True, exist_ok=True)
    for stale in dest_dir.glob("mythic_contracts-*.jar"):
        if stale.name != source_jar.name:
            stale.unlink()
    shutil.copy2(source_jar, dest_dir / source_jar.name)


def sha256_file(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def update_packwiz_index() -> str:
    tracked = []
    for folder in ["config", "defaultconfigs", "kubejs", "mods"]:
        folder_path = ROOT / folder
        if not folder_path.exists():
            continue
        for p in sorted(folder_path.rglob("*")):
            if not p.is_file() or p.name == ".gitkeep":
                continue
            if folder == "mods" and not (
                p.name.endswith(".pw.toml") or p.name.startswith("mythic_contracts-")
            ):
                continue
            rel = p.relative_to(ROOT).as_posix()
            sha = sha256_file(p)
            is_meta = p.name.endswith(".pw.toml")
            tracked.append((rel, sha, is_meta))

    lines = ['hash-format = "sha256"', ""]
    for rel, sha, is_meta in sorted(tracked, key=lambda x: x[0]):
        lines.append("[[files]]")
        lines.append(f'file = "{rel}"')
        lines.append(f'hash = "{sha}"')
        if is_meta:
            lines.append("metafile = true")
        lines.append("")

    index_path = ROOT / "index.toml"
    index_path.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    idx_hash = sha256_file(index_path)

    pack_path = ROOT / "pack.toml"
    pack_lines = pack_path.read_text(encoding="utf-8").splitlines()
    new_pack = []
    for line in pack_lines:
        if line.strip().startswith("hash ="):
            new_pack.append(f'hash = "{idx_hash}"')
        else:
            new_pack.append(line)
    pack_path.write_text("\n".join(new_pack) + "\n", encoding="utf-8", newline="\n")
    print(f"[Packwiz] index.toml atualizado com {len(tracked)} arquivos (SHA-256: {idx_hash[:16]}...)")
    return idx_hash


def rebuild_mrpack() -> None:
    mrpack_path = ROOT / "O_Modpack_Definitivo_Fase3.mrpack"
    modrinth_idx = ROOT / "modrinth.index.json"
    if not modrinth_idx.exists():
        return

    with zipfile.ZipFile(mrpack_path, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(modrinth_idx, "modrinth.index.json")
        for folder in ["config", "defaultconfigs", "kubejs"]:
            folder_path = ROOT / folder
            if not folder_path.exists():
                continue
            for p in sorted(folder_path.rglob("*")):
                if p.is_file() and p.name != ".gitkeep":
                    rel = p.relative_to(ROOT).as_posix()
                    zf.write(p, f"overrides/{rel}")

        core_jar = ROOT / "mods" / JAR_NAME
        if core_jar.exists():
            zf.write(core_jar, f"overrides/mods/{JAR_NAME}")

        opts = ROOT / "defaultconfigs" / "options.txt"
        if opts.exists():
            zf.write(opts, "overrides/options.txt")

    print(f"[MRPACK] {mrpack_path.name} atualizado ({mrpack_path.stat().st_size} bytes)")


def sync_local_prism_instances() -> None:
    appdata = os.environ.get("APPDATA")
    if not appdata:
        return
    instances_dir = pathlib.Path(appdata) / "PrismLauncher" / "instances"
    if not instances_dir.exists():
        return

    for inst in instances_dir.iterdir():
        if not inst.is_dir() or not inst.name.startswith("O_Modpack_Definitivo"):
            continue
        mc_dir = inst / "minecraft" if (inst / "minecraft").exists() else inst / ".minecraft"
        if not mc_dir.exists():
            continue

        # Copy mythic_contracts JAR (removing any stale version first)
        core_jar = ROOT / "mods" / JAR_NAME
        if core_jar.exists():
            replace_core_jar(mc_dir / "mods", core_jar)

        # Sync config, defaultconfigs, kubejs
        for folder in ["config", "defaultconfigs", "kubejs"]:
            src_folder = ROOT / folder
            if not src_folder.exists():
                continue
            for p in src_folder.rglob("*"):
                if p.is_file() and p.name != ".gitkeep":
                    rel = p.relative_to(ROOT)
                    dest_file = mc_dir / rel
                    dest_file.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(p, dest_file)

        print(f"[PrismLauncher] Instancia sincronizada: {inst.name} ({mc_dir})")


if __name__ == "__main__":
    # Se build/libs/mythic_contracts-2.0.0.jar existir, substitui com segurança em mods/
    built_jar = ROOT / "build" / "libs" / JAR_NAME
    if built_jar.exists():
        replace_core_jar(ROOT / "mods", built_jar)

    update_packwiz_index()
    rebuild_mrpack()
    sync_local_prism_instances()
