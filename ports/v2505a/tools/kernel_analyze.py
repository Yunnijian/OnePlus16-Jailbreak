#!/usr/bin/env python3
import argparse
from bisect import bisect_right

from capstone import CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN, Cs
from elftools.elf.elffile import ELFFile


class KernelImage:
    def __init__(self, path: str):
        self.file = open(path, "rb")
        self.elf = ELFFile(self.file)
        self.kernel = self.elf.get_section_by_name(".kernel")
        self.symtab = self.elf.get_section_by_name(".symtab")
        if self.kernel is None or self.symtab is None:
            raise SystemExit("ELF is missing .kernel or .symtab")
        self.symbols = {}
        self.functions = []
        for symbol in self.symtab.iter_symbols():
            name = symbol.name
            address = symbol["st_value"]
            if not name or not address:
                continue
            self.symbols.setdefault(name, address)
            if symbol["st_info"]["type"] == "STT_FUNC":
                self.functions.append((address, name))
        self.functions.sort()
        self.function_addresses = [address for address, _ in self.functions]

    def nearest_function(self, address: int):
        index = bisect_right(self.function_addresses, address) - 1
        if index < 0:
            return None, None
        start, name = self.functions[index]
        return name, address - start

    def function_range(self, name: str):
        start = self.symbols[name]
        index = bisect_right(self.function_addresses, start)
        if index >= len(self.functions):
            end = self.kernel["sh_addr"] + self.kernel["sh_size"]
        else:
            end = self.functions[index][0]
        return start, end

    def read(self, address: int, size: int) -> bytes:
        base = self.kernel["sh_addr"]
        offset = address - base
        if offset < 0 or offset + size > self.kernel["sh_size"]:
            raise ValueError(f"address outside .kernel: {address:#x}")
        data = self.kernel.data()
        return data[offset : offset + size]


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect recovered kernel symbols and calls")
    parser.add_argument("elf")
    parser.add_argument("--symbols", default="")
    parser.add_argument("--calls", default="")
    parser.add_argument("--limit", type=lambda value: int(value, 0), default=0x40000)
    args = parser.parse_args()

    image = KernelImage(args.elf)
    for name in (n.strip() for n in args.symbols.split(",") if n.strip()):
        address = image.symbols.get(name)
        print(f"SYMBOL {name} {address:#x}" if address else f"SYMBOL {name} MISSING")

    for name in (n.strip() for n in args.calls.split(",") if n.strip()):
        if name not in image.symbols:
            print(f"CALLS {name} MISSING")
            continue
        start, end = image.function_range(name)
        end = min(end, start + args.limit)
        print(f"CALLS {name} START={start:#x} END={end:#x}")
        disassembler = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
        disassembler.detail = True
        for insn in disassembler.disasm(image.read(start, end - start), start):
            if insn.mnemonic not in ("bl", "b") or not insn.operands:
                continue
            target = insn.operands[0].imm & ((1 << 64) - 1)
            target_name, delta = image.nearest_function(target)
            suffix = f"{target_name}+{delta:#x}" if target_name else "?"
            print(
                f"  {insn.address:#x} {insn.mnemonic} {target:#x} "
                f"{suffix} RETURN={insn.address + insn.size:#x} "
                f"LOW21={(insn.address + insn.size) & 0x1FFFFF:#x}"
            )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
