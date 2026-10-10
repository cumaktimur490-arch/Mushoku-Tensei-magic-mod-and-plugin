package dev.terrarealis.fabric;

import dev.terrarealis.kernel.TerraKernel;
import dev.terrarealis.fabric.worldgen.KernelHolder;
import dev.terrarealis.kernel.math.Hash;
import dev.terrarealis.kernel.math.Interp;
import dev.terrarealis.kernel.math.Noise;

/**
 * Public API for other mods — in particular the Mushoku magic mod this repository also contains.
 *
 * <p>The generator exposes a smooth, deterministic <b>mana affinity</b> field over the world: high where
 * the crust is young and fractured (volcanic provinces, rift zones, karst), high along deep groundwater,
 * and low over old cratons. It is a pure function of coordinates, so a spell can sample it without
 * touching a chunk, and it is stable for the life of the world.
 *
 * <p>Nothing in the terrain depends on this field. It is a courtesy to magic systems, not a physics.
 */
public final class LeyLines {

    private static final Noise LEY = new Noise(0x1E11AE5L);

    private LeyLines() {}

    /**
     * 0..1 mana affinity at a block position.
     *
     * @param y used to boost affinity near the water table and near magma level
     */
    public static double manaAffinity(int x, int y, int z) {
        TerraKernel k = KernelHolder.kernel();
        double tectonic = 0.5 + 0.5 * LEY.fbm2(x / 2600.0 + 91.0, z / 2600.0 - 47.0, 3, 2.0, 0.5);
        dev.terrarealis.kernel.geo.Tectonics.Setting s = new dev.terrarealis.kernel.geo.Tectonics.Setting();
        k.tectonics().sampleInto(x, z, s);
        double young = Interp.clamp(s.rift * 0.8 + s.arc * 0.6 + s.fault * 0.5, 0.0, 1.0);
        double deep = Interp.smoothstep(k.params().seaLevel - 10, k.params().lavaLevel + 20, y);
        double volcanic = 0.0;
        dev.terrarealis.kernel.geo.Volcanism.Edifice e = new dev.terrarealis.kernel.geo.Volcanism.Edifice();
        k.volcanism().sample(x, z, s.arc, s.rift, e);
        volcanic = e.fresh * 0.7 + e.vent * 0.5;
        return Interp.clamp(0.25 * tectonic + 0.35 * young + 0.20 * deep
                + Interp.clamp(volcanic, 0.0, 1.0) * 0.45, 0.0, 1.0);
    }

    /** Deterministic per-place "ley node" flag: rare points where affinity peaks. */
    public static boolean isLeyNode(int x, int z) {
        long h = Hash.hash(0x1E11AEL, x >> 4, z >> 4);
        return Hash.unit(h) > 0.996 && manaAffinity(x, 64, z) > 0.55;
    }
}
