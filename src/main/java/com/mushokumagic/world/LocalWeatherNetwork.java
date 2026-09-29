package com.mushokumagic.world;

import com.mushokumagic.MushokuMagic;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

/** Sends authoritative local-storm regions to Fabric clients for native weather rendering. */
public final class LocalWeatherNetwork {
    private static final int MAX_STORMS = 8;
    private static final int MAX_DIMENSION_KEY_BYTES = 128;

    private LocalWeatherNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                StormSnapshotMessage.TYPE,
                StormSnapshotMessage.CODEC);
    }

    public static void syncToPlayer(
            class_3222 player,
            class_3218 level,
            List<LocalStormManager.StormSnapshot> storms) {
        if (ServerPlayNetworking.canSend(player, StormSnapshotMessage.TYPE)) {
            ServerPlayNetworking.send(
                    player,
                    new StormSnapshotMessage(level.method_27983().toString(), storms));
        }
    }

    public static final class StormSnapshotMessage implements class_8710 {
        public static final class_8710.class_9154<StormSnapshotMessage> TYPE =
                new class_8710.class_9154<>(MushokuMagic.id("local_weather"));
        public static final class_9139<class_9129, StormSnapshotMessage> CODEC =
                class_9139.method_56438(
                        (message, buffer) -> message.encode(buffer),
                        StormSnapshotMessage::decode);

        private final String dimension;
        private final List<LocalStormManager.StormSnapshot> storms;

        public StormSnapshotMessage(String dimension, List<LocalStormManager.StormSnapshot> storms) {
            if (dimension == null || dimension.isBlank()
                    || dimension.getBytes(StandardCharsets.UTF_8).length > MAX_DIMENSION_KEY_BYTES) {
                throw new IllegalArgumentException("Invalid local weather dimension key");
            }
            if (storms == null || storms.size() > MAX_STORMS) {
                throw new IllegalArgumentException("Invalid local weather storm count");
            }
            for (LocalStormManager.StormSnapshot storm : storms) {
                if (storm.widthChunks() != StormSector.CUMULONIMBUS_WIDTH_CHUNKS
                        || storm.endTick() <= storm.startTick()) {
                    throw new IllegalArgumentException("Invalid local weather storm region");
                }
            }
            this.dimension = dimension;
            this.storms = List.copyOf(storms);
        }

        public String dimension() {
            return this.dimension;
        }

        public List<LocalStormManager.StormSnapshot> storms() {
            return this.storms;
        }

        @Override
        public class_8710.class_9154<? extends class_8710> method_56479() {
            return TYPE;
        }

        private void encode(class_9129 buffer) {
            byte[] dimensionBytes = this.dimension.getBytes(StandardCharsets.UTF_8);
            buffer.writeInt(dimensionBytes.length);
            buffer.writeBytes(dimensionBytes);
            buffer.writeInt(this.storms.size());
            for (LocalStormManager.StormSnapshot storm : this.storms) {
                buffer.writeInt(storm.minChunkX());
                buffer.writeInt(storm.minChunkZ());
                buffer.writeInt(storm.widthChunks());
                buffer.writeLong(storm.startTick());
                buffer.writeLong(storm.endTick());
            }
        }

        private static StormSnapshotMessage decode(class_9129 buffer) {
            int dimensionLength = buffer.readInt();
            if (dimensionLength <= 0 || dimensionLength > MAX_DIMENSION_KEY_BYTES) {
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
    }
}
