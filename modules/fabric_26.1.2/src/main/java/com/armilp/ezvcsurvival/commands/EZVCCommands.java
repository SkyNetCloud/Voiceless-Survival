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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.armilp.ezvcsurvival.goals.MobGoalInjector.acc;

public class EZVCCommands {


    public static void commandInit(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ezvcsurvival").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                        .then(Commands.literal("reloadconfig")
                                .executes(EZVCCommands::executeReload)
                        )
                        .then(Commands.literal("config")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        EZVCNetwork.ezvcNetworkService.sendToClient(new OpenConfigEditorPacket(), player);

                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Opening EZVCSurvival config editor..."),
                                                false
                                        );
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be used by players")
                                        );
                                        return 0;
                                    }
                                })
                        )
        );
        dispatcher.register(Commands.literal("goals")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    Entity target = getLookedAtEntity(player, 10);

                    if (target == null) {
                        player.sendSystemMessage(Component.literal("§cNo entity in sight."), false);
                        return 0;
                    }

                    dumpGoals(player, target);
                    return 1;
                }));


    }


    private static Entity getLookedAtEntity(Player player, double range) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.x * range, look.y * range, look.z * range);

        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

        List<Entity> entities = player.level().getEntities(player, box);

        Entity closest = null;
        double closestDist = range;

        for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = entityBox.clip(start, end);

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



    private static void dumpGoals(Player player, Entity entity) {
        if (!(entity instanceof Mob mob)) {
            player.sendSystemMessage(Component.literal("§cEntity has no AI goals."));
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("§6[").append(entity.getName().getString()).append("]\n");

        sb.append("§eGoals:\n");
        for (WrappedGoal goal : acc(mob).vs$getGoalSelector().getAvailableGoals()) {
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
        for (WrappedGoal goal : acc(mob).vs$getGoalSelector().getAvailableGoals()) {
            sb.append("§7")
                    .append(goal.getPriority())
                    .append(": ")
                    .append(goal.getGoal().getClass().getSimpleName());

            if (goal.isRunning()) {
                sb.append(" §a(running)");
            }

            sb.append("\n");
        }

        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            source.sendSuccess(() -> Component.literal("[EZVCSurvival] Reloading configuration files..."), false);

            // Entity Voice Config
            try {
                reloadEntityVoiceConfig();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // General Sound Config
            try {
                reloadGeneralSoundsConfig();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Sound Config
            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Refresh Mob Goals
            try {
                MobGoalInjector.refreshAll();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendSuccess(() -> Component.literal("[EZVCSurvival] All configurations reloaded successfully!"), false);
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

    public static void applySoundEffect(ServerPlayer player) {
        Identifier effectId = Identifier.fromNamespaceAndPath("death_angels", "sound_effect");
        Holder<MobEffect> effectEntry = BuiltInRegistries.MOB_EFFECT.get(effectId).orElse(null);
        if (effectEntry == null) {
            return;
        }

        MobEffectInstance instance = new MobEffectInstance(effectEntry, 40, 1, false, false);
        player.addEffect(instance);
    }

    public static void applyAggroVoiceEffect(ServerPlayer player) {
        Identifier effectId = Identifier.fromNamespaceAndPath("quiet_place", "agrovoiceeffect");
        Holder<MobEffect> effectEntry = BuiltInRegistries.MOB_EFFECT.get(effectId).orElse(null);
        if (effectEntry == null) {
            return;
        }

        MobEffectInstance instance = new MobEffectInstance(effectEntry, 40, 1, false, false);
        player.addEffect(instance);
    }

    public static void applyEffect(ServerPlayer player) {
        Identifier effectId = Identifier.fromNamespaceAndPath("death_angels", "sound_effect");
        Holder<MobEffect> effectEntry = BuiltInRegistries.MOB_EFFECT.get(effectId).orElse(null);
        if (effectEntry == null) {
            return;
        }

        MobEffectInstance instance = new MobEffectInstance(effectEntry, 40, 1, false, false);
        player.addEffect(instance);
    }

}