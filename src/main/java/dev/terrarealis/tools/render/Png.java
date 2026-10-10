package dev.terrarealis.tools.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Minimal RGB8 PNG encoder.
 *
 * <p>The preview tool has to run without a Gradle build, without Maven Central and without any image
 * library, so the encoder is written out longhand: an IHDR, a single deflate-compressed IDAT with one
 * filter byte per scanline, and an IEND. That is all a truecolour PNG needs.
 */
public final class Png {

    private Png() {}

    public static void write(Path file, int width, int height, int[] rgb) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        ByteArrayOutputStream raw = new ByteArrayOutputStream(width * height * 3 + height);
        for (int y = 0; y < height; y++) {
            raw.write(0); // filter: none
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int c = rgb[row + x];
                raw.write((c >> 16) & 0xFF);
                raw.write((c >> 8) & 0xFF);
                raw.write(c & 0xFF);
            }
        }
        byte[] compressed = deflate(raw.toByteArray());

        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            byte[] ihdr = new byte[13];
            putInt(ihdr, 0, width);
            putInt(ihdr, 4, height);
            ihdr[8] = 8;   // bit depth
            ihdr[9] = 2;   // colour type: truecolour
            ihdr[10] = 0;  // compression
            ihdr[11] = 0;  // filter
            ihdr[12] = 0;  // interlace
            chunk(out, "IHDR", ihdr);
            chunk(out, "IDAT", compressed);
            chunk(out, "IEND", new byte[0]);
        }
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length / 2 + 64);
        Deflater def = new Deflater(7);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(bos, def, 1 << 15)) {
            dos.write(data);
        } finally {
            def.end();
        }
        return bos.toByteArray();
    }

    private static void chunk(OutputStream out, String type, byte[] data) throws IOException {
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] len = new byte[4];
        putInt(len, 0, data.length);
        out.write(len);
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        byte[] c = new byte[4];
        putInt(c, 0, (int) crc.getValue());
        out.write(c);
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }
}
