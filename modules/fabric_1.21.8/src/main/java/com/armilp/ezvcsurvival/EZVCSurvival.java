package com.armilp.ezvcsurvival;




import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.utils.MobGoalManager;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPayload;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class EZVCSurvival implements ModInitializer {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_VERSION = "2.0.0";

    public static EZVCNetwork voiceNetwork;

    @Override
    public void onInitialize() {

        EZVCNetwork.registerCommon();

        System.out.println("EZVCSurvival Mod Initialized with ID: " + MOD_ID);

        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        SoundConfig.loadConfigs();



        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("ezvcsurvival")
                            .requires(source -> source.hasPermissionLevel(2)) // Ops only
                            .then(CommandManager.literal("reloadconfig")
                                    .executes(EZVCSurvival::executeReload)
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
        });

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
                // Replace MobGoalInjector.refreshAll() with MobGoalManager.refreshAllExistingMobs()
                int updatedCount = MobGoalManager.refreshAllExistingMobs(source.getServer());

                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] ✓ Updated goals for " + updatedCount + " existing mobs"),
                        false
                );

                // Optional: Also notify that new mobs will get updated goals automatically
                source.sendFeedback(
                        () -> Text.literal("[EZVCSurvival] §7New mobs will automatically have updated AI"),
                        false
                );

            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] ✗ Failed to refresh mob goals: " + e.getMessage()));
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
