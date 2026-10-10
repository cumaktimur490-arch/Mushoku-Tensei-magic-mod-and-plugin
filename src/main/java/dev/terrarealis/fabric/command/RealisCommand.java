package dev.terrarealis.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.terrarealis.fabric.config.RealisConfig;
import dev.terrarealis.fabric.worldgen.KernelHolder;
import dev.terrarealis.kernel.Column;
import dev.terrarealis.kernel.TerraKernel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * {@code /terra_realis} — the in-game window into the generator.
 *
 * <ul>
 *   <li>{@code /terra_realis here} — the full column diagnosis at the cursor: biome, rock, soil, soil
 *       thickness, climate, aridity, landform flags. This is the tool that makes the geology legible
 *       while standing in it.</li>
 *   <li>{@code /terra_realis profile <n>} — a vertical profile of {@code n} blocks under the cursor,
 *       listing each material change. Useful for checking strata and the water table without digging.</li>
 *   <li>{@code /terra_realis stats} — tile-cache hit rate and kernel identity.</li>
 *   <li>{@code /terra_realis reload} — re-read {@code config/terra_realis.json}. New chunks use the new
 *       parameters; already generated chunks do not, which is stated in the reply.</li>
 * </ul>
 */
public final class RealisCommand {

    private RealisCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                build(dispatcher, registryAccess));
    }

    private static void build(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        d.register(Commands.literal("terra_realis")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("here").executes(RealisCommand::here))
                .then(Commands.literal("profile")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(4, 128))
                                .executes(RealisCommand::profile)))
                .then(Commands.literal("stats").executes(RealisCommand::stats))
                .then(Commands.literal("reload").executes(RealisCommand::reload)));
    }

    private static int here(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        TerraKernel k = KernelHolder.kernel();
        Column col = new Column();
        int x = (int) src.getPosition().x;
        int z = (int) src.getPosition().z;
        k.column(x, z, col);
        dev.terrarealis.kernel.Stratigrapher.decideSurface(col, k.params());
        src.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Terra Realis @ %d,%d  biome=%s  rock=%s  soil=%s  soil %.1f blk  regolith %.1f blk",
                x, z, col.biome.label, col.rock.name(), col.soil.name(),
                col.soilThickness, col.regolith)), false);
        src.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "  T=%.1f C  P=%.0f mm  AI=%.2f  aridity=%.2f  elev=%.0f m  snowline=%.0f m  treeline=%.0f m",
                col.tempC, col.precipMm, col.aridity * 3.2 + 0.55, col.aridity, col.elevationM,
                col.snowLineM, col.treeLineM)), false);
        src.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "  slope=%.0f deg  aspect=%.0f deg  channel=%.2f  fan=%.2f  playa=%.2f  dune=%.1f  karst=%.2f  lava=%.2f",
                col.slopeDeg, Math.toDegrees(col.aspect), col.channel, col.fan, col.playa,
                col.duneHeight, col.karst, col.lavaFlow)), false);
        src.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "  surface=%s  filler=%s  subsoil=%s  water=%s  waterTable=%s  painted=%.2f  riparian=%.2f",
                col.surface.id, col.filler.id, col.subsoil.id,
                col.waterY == Column.NO_WATER ? "none" : ("y" + col.waterY),
                col.waterTableY == Column.NO_WATER ? "none" : ("y" + col.waterTableY),
                col.painted, col.riparian)), false);
        return 1;
    }

    private static int profile(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        int n = IntegerArgumentType.getInteger(c, "blocks");
        TerraKernel k = KernelHolder.kernel();
        Column col = new Column();
        int x = (int) src.getPosition().x;
        int z = (int) src.getPosition().z;
        k.column(x, z, col);
        dev.terrarealis.kernel.Material[] stack =
                new dev.terrarealis.kernel.Material[k.params().maxY - k.params().minY + 1];
        dev.terrarealis.kernel.Stratigrapher.fill(k, col, x, z, stack, k.new DirectCaves());
        int shown = 0;
        dev.terrarealis.kernel.Material prev = null;
        for (int y = col.surfaceY; y > col.surfaceY - n && y >= k.params().minY; y--) {
            dev.terrarealis.kernel.Material m = stack[y - k.params().minY];
            if (m != prev) {
                int yy = y;
                src.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                        "  y=%4d  %s", yy, m.id)), false);
                prev = m;
                if (++shown >= 24) {
                    break;
                }
            }
        }
        return shown;
    }

    private static int stats(CommandContext<CommandSourceStack> c) {
        TerraKernel k = KernelHolder.kernel();
        long[] s = k.tileCache().stats();
        double hit = s[0] + s[1] == 0 ? 0 : 100.0 * s[0] / (s[0] + s[1]);
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Terra Realis kernel seed=0x%X  tiles cached=%d  hits=%d misses=%d (%.1f%%)  tile=%d blocks",
                KernelHolder.seed(), s[2], s[0], s[1], hit, k.tileBlocks())), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        RealisConfig.invalidate();
        c.getSource().sendSuccess(() -> Component.literal(
                "Terra Realis: config reloaded. New chunks use the new parameters; existing chunks do not."),
                false);
        return 1;
    }
}
