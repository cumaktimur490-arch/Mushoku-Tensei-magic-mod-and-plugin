package dev.terrarealis.fabric.worldgen;

import dev.terrarealis.fabric.config.RealisConfig;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.TerraKernel;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the live {@link TerraKernel} for the running server.
 *
 * <p>The kernel is expensive to build (it calibrates its hypsometry and allocates its noise fields) and
 * is shared by the chunk generator, the biome source and the commands, so there is exactly one instance
 * per (seed, parameter fingerprint). The world seed is captured by
 * {@code RealisChunkGenerator#createState}, which Minecraft calls once per world load before any chunk
 * is generated; until then a documented fallback seed is used so that nothing can observe a half-built
 * state.
 */
public final class KernelHolder {

    private static final AtomicLong SEED = new AtomicLong(0x5EED1234L);
    private static volatile TerraKernel kernel;
    private static volatile long fingerprint;

    private KernelHolder() {}

    /** Called by the chunk generator once the world seed is known. */
    public static void noteSeed(long seed) {
        SEED.set(seed);
    }

    public static long seed() {
        return SEED.get();
    }

    public static TerraKernel kernel() {
        GenParams p = RealisConfig.get();
        long fp = p.fingerprint();
        TerraKernel k = kernel;
        if (k == null || fp != fingerprint) {
            synchronized (KernelHolder.class) {
                k = kernel;
                if (k == null || fp != fingerprint) {
                    k = new TerraKernel(SEED.get(), p.copy());
                    kernel = k;
                    fingerprint = fp;
                }
            }
        }
        return k;
    }
}
