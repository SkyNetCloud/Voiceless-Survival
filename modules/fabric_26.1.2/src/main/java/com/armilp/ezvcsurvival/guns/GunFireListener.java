package com.armilp.ezvcsurvival.guns;

import com.armilp.ezvcsurvival.commands.EZVCCommands;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class GunFireListener {

    private static final long EXPIRATION_TIME_MS = 5000;
    private static final AtomicReference<GunshotData> lastShot = new AtomicReference<>(null);

    public static void onGunFire(GunFireEvent event) {
        LivingEntity shooter = event.getShooter();
        if (shooter == null) return;

        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);

        boolean silenced = isSilenced(shooter, gunStack);

        if (silenced && !SoundConfig.isSilencerModifiersEnabled()) return;

        Vec3d shooterPos = shooter.getEntityPos();
        GunTabType gunType = GunTabType.PISTOL;

        if (gun != null) {
            try {
                Identifier gunId = gun.getGunId(gunStack);
                String gunIdStr = gunId.toString().toLowerCase();

                CommonGunIndex commonGunIndex = CommonGunIndexRegistry.getCommonGunIndex(gunId);

                if (commonGunIndex != null) {
                    String typeStr = commonGunIndex.getType();
                    try {
                        gunType = GunTabType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        gunType = inferGunTypeFromId(gunIdStr);
                    }
                } else {
                    gunType = inferGunTypeFromId(gunIdStr);
                }
            } catch (Exception e) {
                gunType = GunTabType.PISTOL;
            }
        }

        if (shooter instanceof ServerPlayerEntity serverPlayer) {
            EZVCCommands.applyEffect(serverPlayer);
        }

        lastShot.set(new GunshotData(shooterPos, System.currentTimeMillis(), gunType, silenced));
    }

    private static GunTabType inferGunTypeFromId(String gunIdStr) {
        if (gunIdStr.contains("sniper") || gunIdStr.contains("awp") || gunIdStr.contains("barrett")) {
            return GunTabType.SNIPER;
        } else if (gunIdStr.contains("rifle") || gunIdStr.contains("ak") || gunIdStr.contains("m4") ||
                gunIdStr.contains("scar") || gunIdStr.contains("hk416")) {
            return GunTabType.RIFLE;
        } else if (gunIdStr.contains("shotgun") || gunIdStr.contains("spas") || gunIdStr.contains("m870")) {
            return GunTabType.SHOTGUN;
        } else if (gunIdStr.contains("smg") || gunIdStr.contains("mp5") || gunIdStr.contains("ump") ||
                gunIdStr.contains("vector") || gunIdStr.contains("uzi")) {
            return GunTabType.SMG;
        } else if (gunIdStr.contains("rpg") || gunIdStr.contains("rocket") || gunIdStr.contains("launcher")) {
            return GunTabType.RPG;
        } else if (gunIdStr.contains("mg") || gunIdStr.contains("lmg") || gunIdStr.contains("m249") ||
                gunIdStr.contains("minigun")) {
            return GunTabType.MG;
        }
        return GunTabType.PISTOL;
    }

    private static boolean isSilenced(LivingEntity entity, ItemStack gunStack) {
        try {
            IGunOperator operator = IGunOperator.fromLivingEntity(entity);
            if (operator != null) {
                AttachmentCacheProperty cacheProperty = operator.getCacheProperty();
                if (cacheProperty != null) {
                    Object raw = cacheProperty.getCache(SilenceModifier.ID);
                    if (raw instanceof Pair<?, ?> p) {
                        if (Boolean.TRUE.equals(p.right())) return true;
                    } else if (raw instanceof net.minecraft.util.Pair<?, ?> p) {
                        if (Boolean.TRUE.equals(p.getRight())) return true;
                    }
                }
            }
        } catch (Throwable t) {
        }

        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun != null) {
            return SoundConfig.isSilencedGun(gun.getGunId(gunStack));
        }
        return false;
    }

    public static GunshotData getLastGunshotData() {
        GunshotData data = lastShot.get();
        if (data == null) return null;
        if (System.currentTimeMillis() - data.timestamp() > EXPIRATION_TIME_MS) {
            lastShot.compareAndSet(data, null);
            return null;
        }
        return data;
    }

    public static class CommonGunIndexRegistry {
        private static final Map<Identifier, CommonGunIndex> registry = new HashMap<>();

        public static CommonGunIndex getCommonGunIndex(Identifier id) {
            return registry.get(id);
        }

        public static void registerCommonGunIndex(Identifier id, CommonGunIndex index) {
            registry.put(id, index);
        }
    }
}
