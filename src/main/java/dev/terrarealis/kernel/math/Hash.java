/*
 * Terra Realis — ultra-realistic world generation.
 * Copyright (c) 2026 Terra Realis contributors. MIT License.
 */
package dev.terrarealis.kernel.math;

/**
 * Coordinate hashing primitives.
 *
 * <p>Everything in the kernel is a pure function of {@code (seed, x, y, z)}.  Minecraft's own
 * {@code WorldgenRandom} is a stateful PRNG which is awkward for a generator that has to answer
 * "what is at this block?" out of order, from several threads, and from a headless preview tool.
 * These hashes are stateless, allocation free, decorrelated and stable across JVM versions.
 */
public final class Hash {

    /** Golden-ratio derived odd constant; multiplying by it spreads low bits across the word. */
    public static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final long MIX_A = 0xBF58476D1CE4E5B9L;
    private static final long MIX_B = 0x94D049BB133111EBL;
    private static final long MIX_C = 0xC2B2AE3D27D4EB4FL;

    private Hash() {}

    /** splitmix64 finaliser. Excellent avalanche, 3 multiplies, no branches. */
    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * MIX_A;
        z = (z ^ (z >>> 27)) * MIX_B;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, long a) {
        return mix64(seed ^ (a * GOLDEN));
    }

    public static long hash(long seed, long a, long b) {
        return mix64(mix64(seed ^ (a * GOLDEN)) ^ (b * MIX_C));
    }

    public static long hash(long seed, long a, long b, long c) {
        long h = mix64(seed ^ (a * GOLDEN));
        h = mix64(h ^ (b * MIX_C));
        return mix64(h ^ (c * 0xD6E8FEB86659FD93L));
    }

    /** Uniform in (0, 1), never exactly 0 or 1 — safe for {@code log} and division. */
    public static double unit(long h) {
        return ((h >>> 11) + 1) * 0x1.0p-53;
    }

    /** Uniform in (-1, 1). */
    public static double signed(long h) {
        return (h >>> 11) * 0x1.0p-52 - 1.0;
    }

    /** Uniform integer in {@code [0, bound)}. */
    public static int range(long h, int bound) {
        return (int) Math.floorMod(h, bound);
    }

    /** Deterministic 32-bit identity for a chunk coordinate pair. */
    public static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }
}
