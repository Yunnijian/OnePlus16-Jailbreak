#!/usr/bin/env python3
import argparse
import struct
from pathlib import Path

from payload_dumper import update_metadata_pb2 as pb


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect an A/B OTA payload")
    parser.add_argument("payload", type=Path)
    parser.add_argument("--partitions", default="")
    args = parser.parse_args()

    wanted = {p.strip() for p in args.partitions.split(",") if p.strip()}
    with args.payload.open("rb") as f:
        if f.read(4) != b"CrAU":
            raise SystemExit("not an Android OTA payload")
        version = struct.unpack(">Q", f.read(8))[0]
        manifest_size = struct.unpack(">Q", f.read(8))[0]
        signature_size = struct.unpack(">I", f.read(4))[0]
        manifest = pb.DeltaArchiveManifest()
        manifest.ParseFromString(f.read(manifest_size))

    print(
        f"VERSION={version} MANIFEST_SIZE={manifest_size} "
        f"SIGNATURE_SIZE={signature_size} BLOCK_SIZE={manifest.block_size} "
        f"PARTITIONS={len(manifest.partitions)}"
    )
    for part in manifest.partitions:
        if wanted and part.partition_name not in wanted:
            continue
        if part.HasField("new_partition_info"):
            size = part.new_partition_info.size
        else:
            size = sum(
                e.num_blocks * manifest.block_size
                for op in part.operations
                for e in op.dst_extents
            )
        operations = ",".join(
            f"{op.type}:{op.data_length}:{sum(e.num_blocks for e in op.dst_extents)}"
            for op in part.operations
        )
        print(f"{part.partition_name} SIZE={size} OPS={operations}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
