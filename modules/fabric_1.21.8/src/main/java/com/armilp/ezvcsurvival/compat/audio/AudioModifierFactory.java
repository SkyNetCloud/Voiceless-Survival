package com.armilp.ezvcsurvival.compat.audio;
//
//import com.armilp.ezvcsurvival.compat.audio.modifier.IAudioModifier;
//import com.armilp.ezvcsurvival.compat.audio.modifier.NoOpAudioModifier;
//import com.armilp.ezvcsurvival.compat.audio.modifier.RealAudioModifier;
//import net.fabricmc.loader.api.FabricLoader;
//import net.minecraft.util.math.Vec3d;
//
//public class AudioModifierFactory {
//    public static IAudioModifier createAudioModifier(double occlusion, String sound, Vec3d playerPosition, Vec3d senderPos) {
//        // If the mod is not loaded, we use the no-op version
//        if (!FabricLoader.getInstance().isModLoaded("sound_physics_remastered")) {
//            return new NoOpAudioModifier();
//        }
//        // If loaded, we use the actual implementation
//        return new RealAudioModifier(occlusion, sound, playerPosition, senderPos);
//    }
//}
