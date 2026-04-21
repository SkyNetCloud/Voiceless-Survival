package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

import static net.minecraft.server.permissions.PermissionLevel.OWNERS;

public class EZVCCommands {


    public static void commandInit(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ezvcsurvival").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reloadconfig")
                                .executes(EZVCCommands::executeReload)
                        )
                        .then(Commands.literal("config")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        // Send packet to open config editor
                                        OpenConfigEditorPayload.sendToClient(player);

                                        context.getSource().sendSystemMessage( Component.literal("Opening EZVCSurvival config editor..."));
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(Component.literal("This command can only be used by players"));
                                        return 0;
                                    }
                                })
                        )
        );
    }



    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            source.sendSystemMessage(Component.literal("[EZVCSurvival] Reloading configuration files..."));

            try {
                reloadEntityVoiceConfig();
                source.sendSystemMessage(Component.literal("[EZVCSurvival] ✓ EntityVoiceConfig reloaded"));
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] ✗ Failed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                reloadGeneralSoundsConfig();
                source.sendSystemMessage(Component.literal("[EZVCSurvival] ✓ GeneralSoundConfig reloaded"));
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] ✗ Failed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                SoundConfig.loadConfigs();
                source.sendSystemMessage(
                         Component.literal("[EZVCSurvival] ✓ SoundConfig reloaded")
                );
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] ✗ Failed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                // VoiceConfig.reload(); // Uncomment if you have this method
                source.sendSystemMessage(
                         Component.literal("[EZVCSurvival] ✓ VoiceConfig reloaded")
                );
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] ✗ Failed to reload VoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                MobGoalInjector.refreshAll();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendSystemMessage(
                     Component.literal("[EZVCSurvival] ✅ All configurations reloaded successfully!")
            );
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("[EZVCSurvival] §cAn unexpected error occurred: " + e.getMessage()));
            if (VoiceConfig.DEBUG.get()) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    private static void reloadEntityVoiceConfig() throws Exception {
        Method loadOrCreateMethod = EntityVoiceConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private static void reloadGeneralSoundsConfig() throws Exception {
        Method loadOrCreateMethod = GeneralSoundsConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

}

