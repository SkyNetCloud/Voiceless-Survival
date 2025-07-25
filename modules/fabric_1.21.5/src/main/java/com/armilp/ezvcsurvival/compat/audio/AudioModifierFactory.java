package com.armilp.ezvcsurvival.compat.audio;

import com.armilp.ezvcsurvival.compat.audio.modifier.IAudioModifier;
import com.armilp.ezvcsurvival.compat.audio.modifier.NoOpAudioModifier;
import com.armilp.ezvcsurvival.compat.audio.modifier.RealAudioModifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

public class AudioModifierFactory {
    public static IAudioModifier createAudioModifier(double occlusion, String sound, Vec3d playerPosition, Vec3d senderPos) {
        // Si no está cargado el mod, usamos la versión no-op
        if (!FabricLoader.getInstance().isModLoaded("sound-physics-remastered")) {
            return new NoOpAudioModifier();
        }
        // Si está cargado, usamos la implementación real
        return new RealAudioModifier(occlusion, sound, playerPosition, senderPos);
    }
}
