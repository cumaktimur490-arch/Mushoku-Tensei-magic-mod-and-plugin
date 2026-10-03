package com.mushokumagic.weather.world;

import com.mushokumagic.weather.MushokuWeather;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Sends compact authoritative local weather fields for client-side volumetric rendering. */
public final class LocalWeatherNetwork {
    private static final String PROTOCOL = "2";
    private static final int MAX_LOCAL_STORMS = 8;
    private static final int MAX_SEVERE_STORMS = 10;
    private static final int MAX_DIMENSION_KEY_BYTES = 128;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new class_2960(MushokuWeather.MOD_ID, "weather_state_v2"),
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

    /** Periodically sends nearby regional climate and all local storm states to each client. */
    public static void syncServerSnapshots(MinecraftServer server) {
        List<class_3222> players = server.method_3760().method_14571();
        for (class_3218 level : server.method_3738()) {
            if (level.method_8510() % 20L != 0L) {
                continue;
            }
            List<LocalStormManager.StormSnapshot> localStorms = LocalStormManager.snapshots(level);
            List<SevereWeatherManager.StormSnapshot> severeStorms = SevereWeatherManager.snapshots(level);
            for (class_3222 player : players) {
                if (player.method_51469() == level) {
                    LocalWeatherNetwork.syncToPlayer(
                            player,
                            level,
                            localStorms,
                            severeStorms,
                            RegionalWeatherManager.snapshotAt(level, player));
                }
            }
        }
    }

    public static void syncToPlayer(
            class_3222 player,
            class_3218 level,
            List<LocalStormManager.StormSnapshot> localStorms) {
        LocalWeatherNetwork.syncToPlayer(
                player,
                level,
                localStorms,
                SevereWeatherManager.snapshots(level),
                RegionalWeatherManager.snapshotAt(level, player));
    }

    public static void syncToPlayer(
            class_3222 player,
            class_3218 level,
            List<LocalStormManager.StormSnapshot> localStorms,
            List<SevereWeatherManager.StormSnapshot> severeStorms,
            RegionalWeatherManager.RegionalSnapshot regional) {
        String dimension = level.method_27983().toString();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StormSnapshotMessage(dimension, localStorms, severeStorms, regional));
    }

    private static final class StormSnapshotMessage {
        private final String dimension;
        private final List<LocalStormManager.StormSnapshot> localStorms;
        private final List<SevereWeatherManager.StormSnapshot> severeStorms;
        private final RegionalWeatherManager.RegionalSnapshot regional;

        private StormSnapshotMessage(
                String dimension,
                List<LocalStormManager.StormSnapshot> localStorms,
                List<SevereWeatherManager.StormSnapshot> severeStorms,
                RegionalWeatherManager.RegionalSnapshot regional) {
            if (dimension == null || dimension.isBlank()
                    || dimension.getBytes(StandardCharsets.UTF_8).length > MAX_DIMENSION_KEY_BYTES
                    || localStorms == null || localStorms.size() > MAX_LOCAL_STORMS
                    || severeStorms == null || severeStorms.size() > MAX_SEVERE_STORMS
                    || regional == null) {
                throw new IllegalArgumentException("Invalid weather snapshot counts or dimension key");
            }
            for (LocalStormManager.StormSnapshot storm : localStorms) {
                if (storm.widthChunks() != StormSector.CUMULONIMBUS_WIDTH_CHUNKS
                        || !Double.isFinite(storm.baseY())
                        || storm.endTick() <= storm.startTick()) {
                    throw new IllegalArgumentException("Invalid local weather storm region");
                }
            }
            for (SevereWeatherManager.StormSnapshot storm : severeStorms) {
                if (storm.kind() == null || storm.kind() == SevereWeatherModel.Kind.NONE
                        || storm.endTick() <= storm.startTick()
                        || !finite(storm.x(), storm.baseY(), storm.z(), storm.travelX(), storm.travelZ(),
                                storm.travelSpeed(), storm.radius(), storm.baseIntensity(), storm.strengtheningBoost(), storm.phase())) {
                    throw new IllegalArgumentException("Invalid severe weather state");
                }
            }
            if (!finite(regional.x(), regional.y(), regional.z(), regional.cloudCover(), regional.precipitationIntensity(),
                    regional.temperature(), regional.windX(), regional.windZ(), regional.windStrength())) {
                throw new IllegalArgumentException("Invalid regional weather state");
            }
            this.dimension = dimension;
            this.localStorms = List.copyOf(localStorms);
            this.severeStorms = List.copyOf(severeStorms);
            this.regional = regional;
        }

        private static void encode(StormSnapshotMessage message, class_2540 buffer) {
            byte[] dimensionBytes = message.dimension.getBytes(StandardCharsets.UTF_8);
            buffer.writeInt(dimensionBytes.length);
            buffer.writeBytes(dimensionBytes);
            buffer.writeInt(message.localStorms.size());
            for (LocalStormManager.StormSnapshot storm : message.localStorms) {
                buffer.writeInt(storm.minChunkX());
                buffer.writeInt(storm.minChunkZ());
                buffer.writeInt(storm.widthChunks());
                buffer.writeDouble(storm.baseY());
                buffer.writeLong(storm.startTick());
                buffer.writeLong(storm.endTick());
            }
            buffer.writeInt(message.severeStorms.size());
            for (SevereWeatherManager.StormSnapshot storm : message.severeStorms) {
                buffer.writeInt(storm.kind().ordinal());
                buffer.writeDouble(storm.x());
                buffer.writeDouble(storm.baseY());
                buffer.writeDouble(storm.z());
                buffer.writeDouble(storm.travelX());
                buffer.writeDouble(storm.travelZ());
                buffer.writeDouble(storm.travelSpeed());
                buffer.writeDouble(storm.radius());
                buffer.writeInt(storm.height());
                buffer.writeDouble(storm.baseIntensity());
                buffer.writeDouble(storm.strengtheningBoost());
                buffer.writeDouble(storm.phase());
                buffer.writeLong(storm.startTick());
                buffer.writeLong(storm.endTick());
                buffer.writeLong(storm.snapshotTick());
            }
            buffer.writeDouble(message.regional.x());
            buffer.writeDouble(message.regional.y());
            buffer.writeDouble(message.regional.z());
            buffer.writeDouble(message.regional.cloudCover());
            buffer.writeDouble(message.regional.precipitationIntensity());
            buffer.writeDouble(message.regional.temperature());
            buffer.writeDouble(message.regional.windX());
            buffer.writeDouble(message.regional.windZ());
            buffer.writeDouble(message.regional.windStrength());
            buffer.writeBoolean(message.regional.thunderstorm());
        }

        private static StormSnapshotMessage decode(class_2540 buffer) {
            int dimensionLength = buffer.readInt();
            if (dimensionLength <= 0 || dimensionLength > MAX_DIMENSION_KEY_BYTES) {
                throw new IllegalArgumentException("Invalid local weather dimension key length");
            }
            byte[] dimensionBytes = new byte[dimensionLength];
            buffer.readBytes(dimensionBytes);
            String dimension = new String(dimensionBytes, StandardCharsets.UTF_8);
            int localCount = buffer.readInt();
            if (localCount < 0 || localCount > MAX_LOCAL_STORMS) {
                throw new IllegalArgumentException("Invalid local storm count");
            }
            ArrayList<LocalStormManager.StormSnapshot> localStorms = new ArrayList<>(localCount);
            for (int index = 0; index < localCount; index++) {
                int minChunkX = buffer.readInt();
                int minChunkZ = buffer.readInt();
                int widthChunks = buffer.readInt();
                double baseY = buffer.readDouble();
                long startTick = buffer.readLong();
                long endTick = buffer.readLong();
                if (widthChunks != StormSector.CUMULONIMBUS_WIDTH_CHUNKS
                        || !Double.isFinite(baseY)
                        || endTick <= startTick) {
                    throw new IllegalArgumentException("Invalid local weather storm region");
                }
                localStorms.add(new LocalStormManager.StormSnapshot(
                        minChunkX, minChunkZ, widthChunks, baseY, startTick, endTick));
            }
            int severeCount = buffer.readInt();
            if (severeCount < 0 || severeCount > MAX_SEVERE_STORMS) {
                throw new IllegalArgumentException("Invalid severe weather count");
            }
            ArrayList<SevereWeatherManager.StormSnapshot> severeStorms = new ArrayList<>(severeCount);
            SevereWeatherModel.Kind[] kinds = SevereWeatherModel.Kind.values();
            for (int index = 0; index < severeCount; index++) {
                int kindIndex = buffer.readInt();
                if (kindIndex <= 0 || kindIndex >= kinds.length) {
                    throw new IllegalArgumentException("Invalid severe weather kind");
                }
                severeStorms.add(new SevereWeatherManager.StormSnapshot(
                        kinds[kindIndex],
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readInt(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readLong(),
                        buffer.readLong(),
                        buffer.readLong()));
            }
            RegionalWeatherManager.RegionalSnapshot regional = new RegionalWeatherManager.RegionalSnapshot(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBoolean());
            return new StormSnapshotMessage(dimension, localStorms, severeStorms, regional);
        }

        private static boolean finite(double... values) {
            for (double value : values) {
                if (!Double.isFinite(value)) {
                    return false;
                }
            }
            return true;
        }

        private static void handle(StormSnapshotMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientStormWeather.applySnapshot(
                            message.dimension, message.localStorms, message.severeStorms, message.regional)));
            context.setPacketHandled(true);
        }
    }
}
