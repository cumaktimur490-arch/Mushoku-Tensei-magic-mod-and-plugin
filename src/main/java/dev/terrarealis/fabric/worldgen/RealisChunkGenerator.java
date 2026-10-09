package dev.terrarealis.fabric.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.GenParams;
import dev.terrarealis.kernel.Material;
import dev.terrarealis.kernel.Stratigrapher;
import dev.terrarealis.kernel.TerraKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Blender;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureManager;
import net.minecraft.world.level.levelgen.chunk.ChunkGenerator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The chunk generator.
 *
 * <p>All terrain comes from {@link TerraKernel}; this class only translates a {@link Column} and a
 * {@link Material} stack into block states and heightmaps. It overrides the whole generation pipeline:
 * {@code fillFromNoise} places every block including caves and water, {@code applyCarvers} and
 * {@code buildSurface} are deliberate no-ops because there is nothing left for them to do, and
 * {@code getBaseHeight}/{@code getBaseColumn} answer from the cheap analytic uplift field so that
 * structure placement never triggers a tile simulation.
 *
 * <p>No mixins are used anywhere in the mod: everything here is ordinary subclassing of the vanilla
 * {@code ChunkGenerator} and {@code BiomeSource} extension points.
 */
public class RealisChunkGenerator extends ChunkGenerator {

    public static final MapCodec<RealisChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i
            .group(RegistryOps.retrieveGetter(Registries.BIOME))
            .apply(i, RealisChunkGenerator::new));

    private static final ThreadLocal<Column[]> COLUMNS = ThreadLocal.withInitial(() -> {
        Column[] c = new Column[256];
        for (int i = 0; i < 256; i++) {
            c[i] = new Column();
        }
        return c;
    });
    private static final ThreadLocal<Material[]> STACK =
            ThreadLocal.withInitial(() -> new Material[512]);
    private static final ThreadLocal<TerraKernel.LatticeCaves[]> CAVES = new ThreadLocal<>();

    private volatile long seedHint = Long.MIN_VALUE;

    public RealisChunkGenerator(HolderGetter<net.minecraft.world.level.biome.Biome> biomes) {
        super(new RealisBiomeSource(biomes));
    }

    public RealisChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.level.levelgen.chunk.ChunkGeneratorStructureState createState(
            net.minecraft.core.HolderLookup<net.minecraft.world.level.levelgen.structure.StructureSet> structures,
            RandomState randomState, long seed) {
        KernelHolder.noteSeed(seed);
        this.seedHint = seed;
        return super.createState(structures, randomState, seed);
    }

    /** Deliberate no-op: caves are placed by the kernel inside {@link #fillFromNoise}. */
    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk) {
        KernelHolder.noteSeed(seed);
    }

    /** Deliberate no-op: surfaces are placed by the kernel inside {@link #fillFromNoise}. */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        GenParams p = KernelHolder.kernel().params();
        return p.maxY - p.minY;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        TerraKernel kernel = KernelHolder.kernel();
        GenParams p = kernel.params();
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        int minY = p.minY;
        int height = p.maxY - minY + 1;

        Column[] cols = COLUMNS.get();
        for (int cz = 0; cz < 16; cz++) {
            for (int cx = 0; cx < 16; cx++) {
                kernel.column(minX + cx, minZ + cz, cols[cz * 16 + cx]);
            }
        }

        TerraKernel.LatticeCaves[] cavesBox = CAVES.get();
        if (cavesBox == null) {
            cavesBox = new TerraKernel.LatticeCaves[1];
            CAVES.set(cavesBox);
        }
        if (cavesBox[0] == null) {
            cavesBox[0] = new TerraKernel.LatticeCaves(kernel);
        }
        cavesBox[0].prepare(minX, minZ, cols, minY, p.maxY);

        Material[] stack = STACK.get();
        if (stack.length < height) {
            stack = new Material[height];
            STACK.set(stack);
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int cz = 0; cz < 16; cz++) {
            for (int cx = 0; cx < 16; cx++) {
                Column col = cols[cz * 16 + cx];
                Stratigrapher.fill(kernel, col, minX + cx, minZ + cz, stack, cavesBox[0]);
                int snowRun = 0;
                for (int y = minY; y <= p.maxY; y++) {
                    Material m = stack[y - minY];
                    if (m == Material.AIR) {
                        continue;
                    }
                    BlockState state;
                    if (m == Material.SNOW_LAYER) {
                        snowRun++;
                        continue;
                    }
                    if (snowRun > 0) {
                        // Flush the pending snow as a single layered block on top of the ground.
                        int sy = y - snowRun;
                        BlockState snow = MaterialBlocks.snowLayers(snowRun);
                        cursor.set(minX + cx, sy, minZ + cz);
                        chunk.setBlockState(cursor, snow, false);
                        worldSurface.update(cx, sy - minY, cz, snow);
                        snowRun = 0;
                    }
                    state = MaterialBlocks.state(m);
                    cursor.set(minX + cx, y, minZ + cz);
                    chunk.setBlockState(cursor, state, false);
                    oceanFloor.update(cx, y - minY, cz, state);
                    worldSurface.update(cx, y - minY, cz, state);
                }
                if (snowRun > 0) {
                    int sy = p.maxY + 1 - snowRun;
                    BlockState snow = MaterialBlocks.snowLayers(snowRun);
                    cursor.set(minX + cx, sy, minZ + cz);
                    chunk.setBlockState(cursor, snow, false);
                    worldSurface.update(cx, sy - minY, cz, snow);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return KernelHolder.kernel().params().seaLevel;
    }

    @Override
    public int getMinY() {
        return KernelHolder.kernel().params().minY;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level,
                             RandomState randomState) {
        TerraKernel kernel = KernelHolder.kernel();
        int surface = (int) Math.floor(kernel.upliftHeight(x, z));
        if (type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG) {
            return Math.max(surface, kernel.params().seaLevel - 1) + 1;
        }
        if (type == Heightmap.Types.MOTION_BLOCKING || type == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES) {
            return Math.max(surface, kernel.params().seaLevel) + 1;
        }
        return surface + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        TerraKernel kernel = KernelHolder.kernel();
        GenParams p = kernel.params();
        int surface = (int) Math.floor(kernel.upliftHeight(x, z));
        int sea = p.seaLevel;
        BlockState stone = MaterialBlocks.state(Material.STONE);
        BlockState water = MaterialBlocks.state(Material.WATER);
        BlockState air = MaterialBlocks.state(Material.AIR);
        BlockState[] states = new BlockState[p.maxY - p.minY + 1];
        for (int y = p.minY; y <= p.maxY; y++) {
            if (y <= surface) {
                states[y - p.minY] = stone;
            } else if (y <= sea) {
                states[y - p.minY] = water;
            } else {
                states[y - p.minY] = air;
            }
        }
        return new NoiseColumn(p.minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        TerraKernel kernel = KernelHolder.kernel();
        Column col = new Column();
        kernel.column(pos.getX(), pos.getZ(), col);
        info.add("Terra Realis: " + col.biome.label
                + "  elev " + (int) col.elevationM + " m"
                + "  T " + String.format(java.util.Locale.ROOT, "%.1f", col.tempC) + " C"
                + "  P " + (int) col.precipMm + " mm"
                + "  rock " + col.rock.name()
                + "  soil " + col.soil.name());
        info.add("Terra Realis: slope " + String.format(java.util.Locale.ROOT, "%.0f", col.slopeDeg)
                + " deg  aridity " + String.format(java.util.Locale.ROOT, "%.2f", col.aridity)
                + "  seed 0x" + Long.toHexString(KernelHolder.seed())
                + "  tiles " + kernel.tileCache().stats()[2]);
    }

}
