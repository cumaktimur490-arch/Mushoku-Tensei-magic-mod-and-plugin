package com.mushokumagic.client;

import com.mushokumagic.MushokuMagic;
import net.minecraft.class_310;

/** Client-only endpoint for the optional AAA Particles effect layer. */
public final class MagicAaaParticlesClient {
    private static final String[] EFFECTS = {
            "mushoku_magic:explosion/main",
            "mushoku_magic:explosion_mini/blue",
            "mushoku_magic:explosion_mini/yellow"
    };

    private MagicAaaParticlesClient() {
    }

    public static void play(int effectKind, double x, double y, double z, float scale) {
        if (effectKind < 0 || effectKind >= EFFECTS.length) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null) {
            return;
        }
        AaaParticlesBridge.play(client.field_1687, EFFECTS[effectKind], x, y, z, scale);
    }

    private static final class AaaParticlesBridge {
        private static Api api;
        private static boolean checked;
        private static boolean warned;

        private AaaParticlesBridge() {
        }

        private static void play(Object level, String effectId, double x, double y, double z, float scale) {
            Api loaded = api();
            if (loaded == null) {
                return;
            }
            try {
                Object id = loaded.resourceLocationConstructor().newInstance(effectId);
                Object emitter = loaded.createEmitter().invoke(null, level, id);
                loaded.setPosition().invoke(emitter, x, y, z);
                loaded.setScale().invoke(emitter, Math.max(0.2f, Math.min(4.0f, scale)));
                loaded.addParticle().invoke(null, level, true, emitter);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                api = null;
                if (!warned) {
                    warned = true;
                    MushokuMagic.LOGGER.warn(
                            "AAA Particles spell effects are unavailable; keeping vanilla spell particles.",
                            exception
                    );
                }
            }
        }

        private static Api api() {
            if (checked) {
                return api;
            }
            checked = true;
            try {
                ClassLoader loader = AaaParticlesBridge.class.getClassLoader();
                Class<?> levelType = Class.forName("net.minecraft.world.level.Level", false, loader);
                Class<?> resourceLocationType = Class.forName("net.minecraft.resources.ResourceLocation", false, loader);
                Class<?> emitterInfoType = Class.forName(
                        "mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo", true, loader);
                Class<?> aaaLevelType = Class.forName("mod.chloeprime.aaaparticles.api.common.AAALevel", true, loader);

                api = new Api(
                        resourceLocationType.getConstructor(String.class),
                        emitterInfoType.getMethod("create", levelType, resourceLocationType),
                        emitterInfoType.getMethod("position", double.class, double.class, double.class),
                        emitterInfoType.getMethod("scale", float.class),
                        aaaLevelType.getMethod("addParticle", levelType, boolean.class, emitterInfoType)
                );
            } catch (ClassNotFoundException exception) {
                // AAA Particles is client-optional; vanilla spell visuals are the normal fallback.
                api = null;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                api = null;
                if (!warned) {
                    warned = true;
                    MushokuMagic.LOGGER.warn(
                            "Could not initialize the optional AAA Particles compatibility bridge.",
                            exception
                    );
                }
            }
            return api;
        }
    }

    private record Api(
            java.lang.reflect.Constructor<?> resourceLocationConstructor,
            java.lang.reflect.Method createEmitter,
            java.lang.reflect.Method setPosition,
            java.lang.reflect.Method setScale,
            java.lang.reflect.Method addParticle
    ) {
    }
}
