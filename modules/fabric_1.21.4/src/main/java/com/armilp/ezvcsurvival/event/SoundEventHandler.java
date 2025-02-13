package com.armilp.ezvcsurvival.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public class SoundEventHandler {

    public static void onPlaySound(SoundInstance event) {
        // Check that the sound is an instance of SimpleSoundInstance (which has a position)
        if (MinecraftClient.getInstance().world == null) {
            return;
        }

        if (event instanceof SoundInstance sound) {
            // Extract the position of the sound
            double x = sound.getX();
            double y = sound.getY();
            double z = sound.getZ();
            Vec3d position = new Vec3d(x, y, z);

            // Get the identifier of the sound
            String  id = String.valueOf(sound.getId());
            SoundEventTracker.registerSound(id, position);
        }
    }
}

