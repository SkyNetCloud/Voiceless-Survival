package com.armilp.ezvcsurvival.compat.audio.modifier;
//
//import net.minecraft.util.math.Vec3d;
//
//public class RealAudioModifier implements IAudioModifier {
//
//    private final MobAudioModifier audioModifier;
//
//    public RealAudioModifier(double occlusion, String sound, Vec3d playerPosition, Vec3d senderPos) {
//        // Creates the actual instance of the audio modifier
//        this.audioModifier = new MobAudioModifier(occlusion, sound);
//
//        // Calculate the direct vector
//        Vec3d directVector = playerPosition.subtract(senderPos);
//        if (directVector.length() == 0) {
//            directVector = new Vec3d(1, 0, 0);
//        }
//
//        // Add the "airspaces"
//        this.audioModifier.addDirectAirspace(directVector);
//        this.audioModifier.addSharedAirspace(new Vec3d(0, 1, 0), 5.0);
//    }
//
//    @Override
//    public double computeModifiedRange(double baseRange) {
//        return this.audioModifier.computeModifiedRange(baseRange);
//    }
//}