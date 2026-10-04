package com.mushokumagic.platform;

import com.mushokumagic.MushokuMagic;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Optional, range-limited server-to-client spell VFX transport. */
public final class MagicFxNetwork {
    private static final String PROTOCOL = "1";
    private static final double VIEW_RANGE = 192.0;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new class_2960(MushokuMagic.MOD_ID, "magic_fx"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static boolean registered;

    private MagicFxNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(
                0,
                SpawnPacket.class,
                SpawnPacket::encode,
                SpawnPacket::decode,
                SpawnPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registered = true;
    }

    public static void spawn(class_3218 level, int effectKind, double x, double y, double z, float scale) {
        if (!registered || effectKind < 0 || effectKind > 2
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(scale) || scale <= 0.0f) {
            return;
        }
        CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        x, y, z, VIEW_RANGE, level.method_27983())),
                new SpawnPacket(effectKind, x, y, z, Math.min(scale, 4.0f))
        );
    }

    private record SpawnPacket(int effectKind, double x, double y, double z, float scale) {
        private static void encode(SpawnPacket message, class_2540 buffer) {
            buffer.writeByte(message.effectKind());
            buffer.writeDouble(message.x());
            buffer.writeDouble(message.y());
            buffer.writeDouble(message.z());
            buffer.writeFloat(message.scale());
        }

        private static SpawnPacket decode(class_2540 buffer) {
            int effectKind = buffer.readUnsignedByte();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            float scale = buffer.readFloat();
            if (effectKind > 2 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(scale) || scale <= 0.0f || scale > 4.0f) {
                throw new IllegalArgumentException("Invalid optional spell effect packet");
            }
            return new SpawnPacket(effectKind, x, y, z, scale);
        }

        private static void handle(SpawnPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> com.mushokumagic.client.MagicAaaParticlesClient.play(
                            message.effectKind(), message.x(), message.y(), message.z(), message.scale())));
            context.setPacketHandled(true);
        }
    }
}
