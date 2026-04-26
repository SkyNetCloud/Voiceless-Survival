package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.packets.OpenConfigEditorPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.armilp.ezvcsurvival.goals.MobGoalInjector.acc;

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
                                        EZVCNetwork.ezvcNetworkService.sendToClient(new OpenConfigEditorPacket(), player);

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
        dispatcher.register(CommandManager.literal("goals")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    Entity target = getLookedAtEntity(player, 10);

                    if (target == null) {
                        player.sendMessage(Text.literal("§cNo entity in sight."), false);
                        return 0;
                    }

                    dumpGoals(player, target);
                    return 1;
                }));


    }


    private static Entity getLookedAtEntity(PlayerEntity player, double range) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.x * range, look.y * range, look.z * range);

        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);

        List<Entity> entities = player.getEntityWorld().getOtherEntities(player, box);

        Entity closest = null;
        double closestDist = range;

        for (Entity entity : entities) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> hit = entityBox.raycast(start, end);

            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }



    private static void dumpGoals(PlayerEntity player, Entity entity) {
        if (!(entity instanceof MobEntity mob)) {
            player.sendMessage(Text.literal("§cEntity has no AI goals."), false);
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("§6[").append(entity.getName().getString()).append("]\n");

        sb.append("§eGoals:\n");
        for (PrioritizedGoal goal : acc(mob).vs$getGoalSelector().getGoals()) {
            sb.append("§7")
                    .append(goal.getPriority())
                    .append(": ")
                    .append(goal.getGoal().getClass().getSimpleName());

            if (goal.isRunning()) {
                sb.append(" §a(running)");
            }

            sb.append("\n");
        }

        sb.append("§cTargets:\n");
        for (PrioritizedGoal goal : acc(mob).vs$getGoalSelector().getGoals()) {
            sb.append("§7")
                    .append(goal.getPriority())
                    .append(": ")
                    .append(goal.getGoal().getClass().getSimpleName());

            if (goal.isRunning()) {
                sb.append(" §a(running)");
            }

            sb.append("\n");
        }

        player.sendMessage(Text.literal(sb.toString()), false);
    }

    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            source.sendFeedback(() -> Text.literal("[EZVCSurvival] Reloading configuration files..."), false);

            // Entity Voice Config
            try {
                reloadEntityVoiceConfig();
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] Failed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // General Sound Config
            try {
                reloadGeneralSoundsConfig();
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] Failed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Sound Config
            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] Failed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Refresh Mob Goals
            try {
                MobGoalInjector.refreshAll();
            } catch (Exception e) {
                source.sendError(Text.literal("[EZVCSurvival] Failed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendFeedback(() -> Text.literal("[EZVCSurvival] All configurations reloaded successfully!"), false);
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

