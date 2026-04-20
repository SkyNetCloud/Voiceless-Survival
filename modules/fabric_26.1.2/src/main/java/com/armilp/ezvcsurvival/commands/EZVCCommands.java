package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.lang.reflect.Method;

public class EZVCCommands {


    public static void commandInit(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("ezvcsurvival").requires(source -> source.getPermissions().hasPermission(DefaultPermissions.OWNERS))
                        .then(CommandManager.literal("reloadconfig")
                                .executes(EZVCCommands::executeReload)
                        )
                        .then(CommandManager.literal("config")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                        // Send packet to open config editor
                                        OpenConfigEditorPayload.sendToClient(player);

                                        context.getSource().sendFeedback(
                                                () -> Text.literal("Opening EZVCSurvival config editor..."),
                                                false
                                        );
                                        return 1;
                                    } else {
                                        context.getSource().sendError(
                                                Text.literal("This command can only be used by players")
                                        );
                                        return 0;
                                    }
                                })
                        )
        );
    }



    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            source.sendFeedback(
                    () -> Text.literal("[EZVCSurvival] Reloading configuration files..."),
                    false
            );

            try {
                reloadEntityVoiceConfig();
                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] ✓ EntityVoiceConfig reloaded"),
                        false
                );
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] ✗ Failed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                reloadGeneralSoundsConfig();
                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] ✓ GeneralSoundConfig reloaded"),
                        false
                );
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] ✗ Failed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                SoundConfig.loadConfigs();
                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] ✓ SoundConfig reloaded"),
                        false
                );
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] ✗ Failed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                // VoiceConfig.reload(); // Uncomment if you have this method
                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] ✓ VoiceConfig reloaded"),
                        false
                );
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] ✗ Failed to reload VoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            try {
                MobGoalInjector.refreshAll();
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] Failed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendFeedback(
                    () -> Text.literal("[EZVCSurvival] ✅ All configurations reloaded successfully!"),
                    false
            );
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("[EZVCSurvival] §cAn unexpected error occurred: " + e.getMessage()));
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

