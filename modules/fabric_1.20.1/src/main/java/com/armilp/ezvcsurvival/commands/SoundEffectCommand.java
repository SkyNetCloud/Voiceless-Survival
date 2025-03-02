package com.armilp.ezvcsurvival.commands;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;

public class SoundEffectCommand {

    public static void applyEffect(ServerPlayerEntity player) {
        // Check if "death_angels" mod is loaded
        if (!FabricLoader.getInstance().isModLoaded("death_angels")) {
            return;
        }

        ServerWorld world = player.getServerWorld(); // Get the world the player is in

        if (world != null) {
            // Create a silent command source
            ServerCommandSource source = new ServerCommandSource(
                    player,
                    player.getPos(),  // Player position
                    player.getRotationClient(), // Player rotation
                    player.getServerWorld(), // The world the player is in
                    4, // Permission level (4 = Admin)
                    player.getName().getString(), // Player's name
                    player.getDisplayName(),
                    player.getServer(),
                    player
            ).withSilent();

//            Vec3d position = player.getPos();

            String command = "effect give " + player.getGameProfile().getName() + " death_angels:sound_effect 2 1 true";

            Objects.requireNonNull(player.getServer()).getCommandManager().executeWithPrefix(source, command);
        }
    }
}
