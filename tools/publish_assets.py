#!/usr/bin/env python3
"""Build, sign and optionally publish the remote asset manifest.

The manifest maps logical deploy names (what run.sh expects) to release asset
URLs plus sha256/size. The app verifies the ECDSA signature with its embedded
public key before trusting it.
"""
import argparse
import datetime
import hashlib
import json
import pathlib
import subprocess
import sys


def sha256(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def entry(name: str, asset: str, src: pathlib.Path, base: str):
    path = src / asset
    if not path.is_file():
        sys.exit(f"missing artifact: {path}")
    return {
        "name": name,
        "url": f"{base}/{asset}",
        "sha256": sha256(path),
        "size": path.stat().st_size,
        "_asset": asset,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, type=pathlib.Path)
    ap.add_argument("--out", required=True, type=pathlib.Path)
    ap.add_argument("--config", default="tools/managers.json", type=pathlib.Path)
    ap.add_argument("--key", type=pathlib.Path, help="ECDSA P-256 private key (PEM)")
    ap.add_argument("--repo", default="Yunnijian/OnePlus16-Jailbreak")
    ap.add_argument("--tag", default="assets-latest")
    ap.add_argument("--base-url", help="override base URL (e.g. http://127.0.0.1:8765)")
    ap.add_argument("--upload", action="store_true")
    args = ap.parse_args()

    cfg = json.loads(args.config.read_text(encoding="utf-8"))
    base = args.base_url or f"https://github.com/{args.repo}/releases/download/{args.tag}"

    assets = []
    shared = [entry(e["name"], e["asset"], args.src, base) for e in cfg["shared"]]
    managers = {}
    for key, m in cfg["managers"].items():
        files = [entry(e["name"], e["asset"], args.src, base) for e in m["files"]]
        managers[key] = {"label": m["label"], "version": m["version"], "files": files}
        assets += files
    assets += shared

    manifest = {
        "schema": 1,
        "kernel": cfg["kernel"],
        "generated": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "shared": shared,
        "managers": managers,
    }
    args.out.mkdir(parents=True, exist_ok=True)
    text = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    out = args.out / "manifest.json"
    out.write_text(text, encoding="utf-8")
    print(f"manifest: {out} ({len(text)} bytes, kernel {cfg['kernel']})")

    if args.key:
        sig = args.out / "manifest.sig"
        subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(args.key), "-out", str(sig), str(out)],
            check=True,
        )
        print(f"signature: {sig} ({sig.stat().st_size} bytes)")
    else:
        print("no --key given: manifest left unsigned (test manifests only)")

    if args.upload:
        names = [a["_asset"] for a in assets]
        listing = subprocess.run(
            ["gh", "release", "view", args.tag, "-R", args.repo],
            capture_output=True, text=True,
        )
        if listing.returncode != 0:
            subprocess.run(
                [
                    "gh", "release", "create", args.tag, "-R", args.repo,
                    "--latest=false",
                    "--title", f"Toolchain bundle for kernel {cfg['kernel']}",
                    "--notes",
                    "Machine-published ko/ksud/syms bundle for the OnePlus 16 jailbreak app. "
                    "The app verifies sha256 values from a signed manifest.",
                ],
                check=True,
            )
        files = [str(args.src / n) for n in names]
        subprocess.run(
            ["gh", "release", "upload", args.tag, *files, "--clobber", "-R", args.repo],
            check=True,
        )
        print(f"uploaded {len(files)} assets to {args.repo}@{args.tag}")

    for a in assets:
        print(f"  {a['name']:20s} <- {a['_asset']:24s} {a['size']:>9d} B  {a['sha256'][:16]}…")


if __name__ == "__main__":
    main()
