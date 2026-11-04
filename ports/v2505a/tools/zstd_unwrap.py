#!/usr/bin/env python3
import argparse
import hashlib
from pathlib import Path

import zstandard as zstd


def main() -> int:
    parser = argparse.ArgumentParser(description="Stream-unwrap a Zstandard frame")
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    args.output.parent.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256()
    total = 0
    first = b""

    with args.input.open("rb") as src, args.output.open("wb") as dst:
        compressed = src.read()
        if compressed[:4] != b"\x28\xb5\x2f\xfd":
            raise SystemExit(f"{args.input} is not a Zstandard frame")
        decompressor = zstd.ZstdDecompressor().decompressobj()
        data = decompressor.decompress(compressed)
        if not decompressor.eof:
            raise SystemExit("the first Zstandard frame is incomplete")
        trailing = len(decompressor.unused_data)
        if len(first) < 64:
            first = data[:64]
        dst.write(data)
        digest.update(data)
        total = len(data)

    print(f"INPUT={args.input}")
    print(f"OUTPUT={args.output}")
    print(f"SIZE={total}")
    print(f"SHA256={digest.hexdigest()}")
    print(f"HEAD={first[:32].hex(' ')}")
    print(f"ANDROID_BOOT_MAGIC={first.find(b'ANDROID!')}")
    print(f"TRAILING_BYTES={trailing}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
