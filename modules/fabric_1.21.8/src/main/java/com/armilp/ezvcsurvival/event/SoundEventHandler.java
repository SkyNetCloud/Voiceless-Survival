package com.armilp.ezvcsurvival.event;



import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class SoundEventHandler {

    public static void onPlaySound(SoundInstance event) {
        // Check that the sound is an instance of SimpleSoundInstance (which has a position)
        if (MinecraftClient.getInstance().world == null) {
            return;
        }

        if (event instanceof SoundInstance sound) {
            Vec3d position = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
            Identifier soundId = sound.getId();
            SoundEventTracker.registerSound(soundId, position);
        }
    }
}

