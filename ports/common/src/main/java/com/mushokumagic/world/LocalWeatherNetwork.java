package com.mushokumagic.world;

import com.mushokumagic.MushokuMagic;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Sends authoritative local-storm regions to clients for native weather rendering. */
public final class LocalWeatherNetwork {
    private static final String PROTOCOL = "1";
    private static final int MAX_STORMS = 8;
    private static final int MAX_DIMENSION_KEY_BYTES = 128;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new class_2960(MushokuMagic.MOD_ID, "local_weather"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static boolean registered;

    private LocalWeatherNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(
                0,
                StormSnapshotMessage.class,
                StormSnapshotMessage::encode,
                StormSnapshotMessage::decode,
                StormSnapshotMessage::handle);
        registered = true;
    }

    public static void syncToPlayer(class_3222 player, class_3218 level, List<LocalStormManager.StormSnapshot> storms) {
        String dimension = level.method_27983().toString();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StormSnapshotMessage(dimension, storms));
    }

    private static final class StormSnapshotMessage {
        private final String dimension;
        private final List<LocalStormManager.StormSnapshot> storms;

        private StormSnapshotMessage(String dimension, List<LocalStormManager.StormSnapshot> storms) {
            this.dimension = dimension;
            this.storms = List.copyOf(storms);
        }

        private static void encode(StormSnapshotMessage message, class_2540 buffer) {
            byte[] dimensionBytes = message.dimension.getBytes(StandardCharsets.UTF_8);
            buffer.writeInt(dimensionBytes.length);
            buffer.writeBytes(dimensionBytes);
            int count = Math.min(MAX_STORMS, message.storms.size());
            buffer.writeInt(count);
            for (int index = 0; index < count; index++) {
                LocalStormManager.StormSnapshot storm = message.storms.get(index);
                buffer.writeInt(storm.minChunkX());
                buffer.writeInt(storm.minChunkZ());
                buffer.writeInt(storm.widthChunks());
                buffer.writeLong(storm.startTick());
                buffer.writeLong(storm.endTick());
            }
        }

        private static StormSnapshotMessage decode(class_2540 buffer) {
            int dimensionLength = buffer.readInt();
            if (dimensionLength < 0 || dimensionLength > MAX_DIMENSION_KEY_BYTES) {
                throw new IllegalArgumentException("Invalid local weather dimension key length");
            }
            byte[] dimensionBytes = new byte[dimensionLength];
            buffer.readBytes(dimensionBytes);
            String dimension = new String(dimensionBytes, StandardCharsets.UTF_8);
            int count = buffer.readInt();
            if (count < 0 || count > MAX_STORMS) {
                throw new IllegalArgumentException("Invalid local weather storm count");
            }
            ArrayList<LocalStormManager.StormSnapshot> storms = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                int minChunkX = buffer.readInt();
                int minChunkZ = buffer.readInt();
                int widthChunks = buffer.readInt();
                long startTick = buffer.readLong();
                long endTick = buffer.readLong();
                if (widthChunks != StormSector.CUMULONIMBUS_WIDTH_CHUNKS || endTick <= startTick) {
                    throw new IllegalArgumentException("Invalid local weather storm region");
                }
                storms.add(new LocalStormManager.StormSnapshot(
                        minChunkX, minChunkZ, widthChunks, startTick, endTick));
            }
            return new StormSnapshotMessage(dimension, storms);
        }

        private static void handle(StormSnapshotMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientStormWeather.applySnapshot(message.dimension, message.storms)));
            context.setPacketHandled(true);
        }
    }
}
