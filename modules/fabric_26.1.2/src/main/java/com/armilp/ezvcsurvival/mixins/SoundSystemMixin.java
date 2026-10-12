package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.events.SoundEventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundSystemMixin {

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD")
    )
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        SoundEventHandler.onPlaySound(sound);
    }
}
