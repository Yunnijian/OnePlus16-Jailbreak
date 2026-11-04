#!/usr/bin/env python3
import argparse
import struct
from pathlib import Path


BTF_MAGIC = b"\x9f\xeb\x01\x00\x18\x00\x00\x00"


def u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


class Btf:
    def __init__(self, data: bytes, offset: int, raw: bool):
        header = offset if raw else 0
        if data[header : header + 8] != BTF_MAGIC:
            raise ValueError("invalid BTF header")
        header_len = u32(data, header + 4)
        type_off = u32(data, header + 8)
        type_len = u32(data, header + 12)
        string_off = u32(data, header + 16)
        string_len = u32(data, header + 20)
        self.types_data = data[
            header + header_len + type_off : header + header_len + type_off + type_len
        ]
        self.strings = data[
            header + header_len + string_off : header + header_len + string_off + string_len
        ]
        self.types = {}
        self._parse_types()

    def string(self, offset: int) -> str:
        if not offset:
            return ""
        end = self.strings.find(b"\x00", offset)
        if end < 0:
            end = len(self.strings)
        return self.strings[offset:end].decode("utf-8", "replace")

    def _parse_types(self) -> None:
        offset = 0
        type_id = 1
        data = self.types_data
        while offset < len(data):
            if offset + 12 > len(data):
                raise ValueError("truncated BTF type")
            name_off, info, size_type = struct.unpack_from("<III", data, offset)
            kind = (info >> 24) & 0x1F
            kind_flag = (info >> 31) & 1
            vlen = info & 0xFFFF
            offset += 12
            members = []

            if kind in (4, 5):
                for _ in range(vlen):
                    member_name_off, member_type, member_offset = struct.unpack_from(
                        "<III", data, offset
                    )
                    offset += 12
                    if kind_flag:
                        bit_size = member_offset >> 24
                        bit_offset = member_offset & 0xFFFFFF
                    else:
                        bit_size = 0
                        bit_offset = member_offset
                    members.append(
                        {
                            "name": self.string(member_name_off),
                            "type": member_type,
                            "bit_offset": bit_offset,
                            "bit_size": bit_size,
                        }
                    )
            elif kind == 1:
                offset += 4
            elif kind == 3:
                offset += 12
            elif kind == 6:
                offset += vlen * 8
            elif kind == 13:
                offset += vlen * 8
            elif kind == 14:
                offset += 4
            elif kind == 15:
                offset += vlen * 12
            elif kind == 17:
                offset += 4
            elif kind == 19:
                offset += vlen * 12
            elif kind in (2, 7, 8, 9, 10, 11, 12, 16, 18):
                pass
            else:
                raise ValueError(f"unsupported BTF kind {kind} at type {type_id}")

            self.types[type_id] = {
                "kind": kind,
                "name": self.string(name_off),
                "size": size_type,
                "members": members,
            }
            type_id += 1

    def find(self, name: str):
        return [
            (type_id, value)
            for type_id, value in self.types.items()
            if value["name"] == name
        ]


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect BTF in a raw kernel image")
    parser.add_argument("image", type=Path)
    parser.add_argument("--types", default="")
    parser.add_argument("--members", default="")
    args = parser.parse_args()

    data = args.image.read_bytes()
    offsets = []
    start = 0
    while True:
        offset = data.find(BTF_MAGIC, start)
        if offset < 0:
            break
        offsets.append(offset)
        start = offset + 1
    if not offsets:
        raise SystemExit("BTF header not found")

    btf = Btf(data, offsets[0], raw=True)
    print(f"BTF_OFFSET={offsets[0]:#x} TYPES={len(btf.types)} STRINGS={len(btf.strings)}")

    wanted_members = {
        name.strip(): set(fields.split(","))
        for name in args.members.split(";")
        if name.strip() and ":" in name
        for fields in [name.split(":", 1)[1]]
    }
    for name in (n.strip() for n in args.types.split(",") if n.strip()):
        matches = btf.find(name)
        if not matches:
            print(f"TYPE {name} MISSING")
            continue
        for type_id, value in matches:
            print(
                f"TYPE {name} ID={type_id} KIND={value['kind']} "
                f"SIZE={value['size']:#x}"
            )
            fields = wanted_members.get(name)
            for member in value["members"]:
                if fields and member["name"] not in fields:
                    continue
                print(
                    f"  MEMBER {member['name']} TYPE={member['type']} "
                    f"BIT_OFFSET={member['bit_offset']:#x} "
                    f"BYTE_OFFSET={member['bit_offset'] // 8:#x} "
                    f"BIT_SIZE={member['bit_size']:#x}"
                )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
