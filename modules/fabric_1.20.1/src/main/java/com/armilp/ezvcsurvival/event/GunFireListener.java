package com.armilp.ezvcsurvival.event;

import com.armilp.ezvcsurvival.data.GunshotData;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GunFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000;
    private static final boolean TACZ_LOADED = FabricLoader.getInstance().isModLoaded("tacz");

    public static final Event<GunFireCallback> GUN_FIRE_EVENT = EventFactory.createArrayBacked(GunFireCallback.class,
            (listeners) -> (player, position) -> {
                for (GunFireCallback listener : listeners) {
                    listener.onGunFire(player, position);
                }
            }
    );

    public interface GunFireCallback {
        void onGunFire(ServerPlayerEntity player, Vec3d position);
    }

    public static void init() {
        if (TACZ_LOADED) {
            GUN_FIRE_EVENT.register(GunFireListener::onGunFire);
        }
    }

    private static void onGunFire(ServerPlayerEntity player, Vec3d position) {
        gunshotPositions.add(new GunshotData(position, System.currentTimeMillis()));
    }

    public static Vec3d getLastGunshotPosition() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp() > EXPIRATION_TIME_MS);
        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1).position();
    }
}
