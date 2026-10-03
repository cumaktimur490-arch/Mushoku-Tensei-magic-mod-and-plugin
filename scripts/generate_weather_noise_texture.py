#!/usr/bin/env python3
"""Generate the original seamless RGB noise texture sampled by Weather's volume shader."""

from __future__ import annotations

import argparse
import random
import struct
import sys
import zlib
from pathlib import Path

SIZE = 512
GRIDS = (4, 8, 16, 32, 64)
OUTPUTS = (
    Path("weather/src/main/resources/assets/mushoku_weather/textures/cloud_noise.png"),
    Path("ports/weather-common/src/main/resources/assets/mushoku_weather/textures/cloud_noise.png"),
)


def smooth(value: float) -> float:
    return value * value * (3.0 - 2.0 * value)


def lerp(first: float, second: float, amount: float) -> float:
    return first + (second - first) * amount


def value_noise(size: int, grid: int, seed: int) -> bytearray:
    rng = random.Random(seed)
    lattice = [rng.randrange(256) / 255.0 for _ in range(grid * grid)]
    pixels = bytearray(size * size)
    scale = grid / size

    for y in range(size):
        gy = y * scale
        y0 = int(gy)
        fy = smooth(gy - y0)
        y1 = (y0 + 1) % grid
        row = y * size
        lattice_y0 = y0 * grid
        lattice_y1 = y1 * grid
        for x in range(size):
            gx = x * scale
            x0 = int(gx)
            fx = smooth(gx - x0)
            x1 = (x0 + 1) % grid
            low = lerp(lattice[lattice_y0 + x0], lattice[lattice_y0 + x1], fx)
            high = lerp(lattice[lattice_y1 + x0], lattice[lattice_y1 + x1], fx)
            pixels[row + x] = max(0, min(255, round(lerp(low, high, fy) * 255.0)))
    return pixels


def blend(layers: tuple[bytearray, ...], weights: tuple[float, ...]) -> bytearray:
    return bytearray(
        max(0, min(255, round(sum(layer[index] * weight for layer, weight in zip(layers, weights)))))
        for index in range(SIZE * SIZE)
    )


def generate_rgb() -> bytes:
    layers = tuple(value_noise(SIZE, grid, 0x4D5457 + grid * 97) for grid in GRIDS)
    channels = (
        blend(layers[:3], (0.68, 0.25, 0.07)),
        blend(layers[:4], (0.12, 0.48, 0.30, 0.10)),
        blend(layers[1:], (0.06, 0.25, 0.42, 0.27)),
    )
    output = bytearray(SIZE * SIZE * 3)
    for index in range(SIZE * SIZE):
        offset = index * 3
        output[offset] = channels[0][index]
        output[offset + 1] = channels[1][index]
        output[offset + 2] = channels[2][index]
    return bytes(output)


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def encode_png(rgb: bytes) -> bytes:
    rows = b"".join(b"\x00" + rgb[y * SIZE * 3 : (y + 1) * SIZE * 3] for y in range(SIZE))
    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(rows, level=9))
        + png_chunk(b"IEND", b"")
    )


def decode_png(path: Path) -> tuple[int, int, bytes]:
    data = path.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError(f"{path} is not a PNG")
    offset = 8
    compressed = bytearray()
    width = height = color_type = bit_depth = None
    while offset < len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4 : offset + 8]
        chunk = data[offset + 8 : offset + 8 + length]
        offset += 12 + length
        if kind == b"IHDR":
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", chunk)
            if compression or filtering or interlace:
                raise ValueError(f"{path} uses an unsupported PNG encoding")
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
    if (width, height, bit_depth, color_type) != (SIZE, SIZE, 8, 2):
        raise ValueError(f"{path} has unexpected image format or dimensions")
    raw = zlib.decompress(compressed)
    stride = SIZE * 3
    pixels = bytearray(SIZE * stride)
    source_offset = 0
    for y in range(SIZE):
        filter_type = raw[source_offset]
        source_offset += 1
        if filter_type != 0:
            raise ValueError(f"{path} contains an unsupported PNG filter")
        pixels[y * stride : (y + 1) * stride] = raw[source_offset : source_offset + stride]
        source_offset += stride
    return width, height, bytes(pixels)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify the checked-in copies match the generated pixels")
    args = parser.parse_args()
    expected = generate_rgb()

    if args.check:
        for path in OUTPUTS:
            if not path.is_file():
                raise SystemExit(f"Missing generated cloud texture: {path}")
            try:
                width, height, pixels = decode_png(path)
            except (OSError, ValueError, zlib.error) as error:
                raise SystemExit(str(error)) from error
            if (width, height) != (SIZE, SIZE) or pixels != expected:
                raise SystemExit(f"{path} does not match the reproducible cloud-noise texture")
            print(f"Verified original {SIZE}x{SIZE} cloud-noise texture: {path}")
        return 0

    image = encode_png(expected)
    for path in OUTPUTS:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(image)
        print(f"Wrote {path} ({len(image)} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
