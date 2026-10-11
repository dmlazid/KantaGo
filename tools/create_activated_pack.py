#!/usr/bin/env python3
"""Make one activated KantaGo song bundle from authorized media files or archives.

The --source folder may include .mid, .kar, .mp3, .mp4, .wav, .ogg, .m4a, .lrc,
.zip or .7z files. Flatten media names; avoid overwriting the user's imports.
Unpacking third-party songs does NOT establish distribution permission.
"""
import argparse
import hashlib
import os
import struct
import tempfile
import zipfile
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

MAGIC = b"KGPACK1\n"
CHUNK = 256 * 1024
ITERATIONS = 210_000
FORMATS = {".mid", ".kar", ".mp3", ".mp4", ".wav", ".ogg", ".m4a", ".lrc"}
MAX_FILE = 500 * 1024 * 1024
MAX_PACK = 768 * 1024 * 1024
MAX_TOTAL = 1024 * 1024 * 1024
MAX_ENTRIES = 2500


def filename(raw):
    path = str(raw).replace("\\", "/")
    if path.startswith("/") or ":" in path or any(p in ("", ".", "..") for p in path.split("/")):
        return None
    last = path.split("/")[-1]
    if len(last) > 180 or Path(last).suffix.lower() not in FORMATS:
        return None
    import re
    clean = re.sub(r"[^\\w .()-]", "_", last, flags=re.UNICODE)
    return clean[: -len(Path(clean).suffix)] + Path(clean).suffix.lower()


def choose(items):
    found = set()
    count = 0
    for source_name, data in items:
        safe = filename(source_name)
        if not safe or safe.casefold() in found:
            continue
        found.add(safe.casefold())
        count += 1
        if count > MAX_ENTRIES:
            raise ValueError("Too many media files; split into multiple packs")
        if len(data) > MAX_FILE:
            raise ValueError("Media file exceeds 500 MB")
        yield safe, data


def collect(folder):
    folder = Path(folder)
    for path in sorted(folder.rglob("*")):
        if not path.is_file():
            continue
        kind = path.suffix.lower()
        if kind in FORMATS:
            if path.stat().st_size <= MAX_FILE:
                yield path.name, path.read_bytes()
        elif kind == ".zip":
            with zipfile.ZipFile(path) as pack:
                for item in pack.infolist():
                    if item.is_dir() or not filename(item.filename):
                        continue
                    if item.file_size > MAX_FILE:
                        raise ValueError("ZIP entry too large")
                    yield item.filename, pack.read(item)
        elif kind == ".7z":
            try:
                import py7zr
            except ImportError as error:
                raise RuntimeError("Install py7zr to combine .7z archives") from error
            # Extract into a temporary folder because py7zr requires streaming factories
            # for in-memory readers and we cap each imported file.
            with tempfile.TemporaryDirectory(prefix="kantago-sevenzip-") as scratch:
                with py7zr.SevenZipFile(path, "r") as archive:
                    archive.extractall(path=scratch)
                for extracted in sorted(Path(scratch).rglob("*")):
                    if extracted.is_file() and extracted.suffix.lower() in FORMATS:
                        if extracted.stat().st_size > MAX_FILE:
                            raise ValueError("7z entry too large")
                        yield extracted.name, extracted.read_bytes()


def build(source, destination, activation):
    if not activation or len(activation) < 8:
        raise ValueError("Use an activation key of at least 8 characters")
    # Enforce explicit rights acknowledgement for assembling a public collection.
    temp = Path(destination).with_suffix(".temporary.zip")
    songs = 0
    total = 0
    try:
        with zipfile.ZipFile(temp, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for name, data in choose(collect(source)):
                total += len(data)
                if total > MAX_TOTAL:
                    raise ValueError("Song bundle exceeds 1 GB expanded data")
                archive.writestr(name, data)
                if Path(name).suffix.lower() != ".lrc":
                    songs += 1
        length = temp.stat().st_size
        if length <= 0 or length > MAX_PACK:
            raise ValueError("Pack exceeds 768 MB compressed limit")
        salt = os.urandom(16)
        nonce_prefix = os.urandom(8)
        header = MAGIC + salt + nonce_prefix + struct.pack(">Q", length)
        aes = AESGCM(hashlib.pbkdf2_hmac("sha256", activation.encode("utf-8"), salt, ITERATIONS, 32))
        with open(destination, "wb") as output, open(temp, "rb") as raw:
            output.write(header)
            index = 0
            while frame := raw.read(CHUNK):
                iv = nonce_prefix + struct.pack(">I", index)
                output.write(aes.encrypt(iv, frame, header + struct.pack(">I", index)))
                index += 1
    finally:
        temp.unlink(missing_ok=True)
    return songs


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create an encrypted KantaGo song pack")
    parser.add_argument("--source", required=True, help="Folder of songs, ZIP files and/or 7z files")
    parser.add_argument("--output", required=True, help="Single resulting .kgpack file")
    parser.add_argument("--activation-key", required=True)
    parser.add_argument("--confirm-rights", action="store_true",
                        help="Confirm that you are authorized to distribute ALL media in this pack")
    args = parser.parse_args()
    if not args.confirm_rights:
        parser.error("--confirm-rights is required to package music for distribution")
    print(f"Packaged {build(args.source, args.output, args.activation_key)} original/authorized songs")
