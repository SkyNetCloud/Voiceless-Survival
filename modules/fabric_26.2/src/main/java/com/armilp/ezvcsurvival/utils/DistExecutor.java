package com.armilp.ezvcsurvival.utils;

import com.google.common.base.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.Callable;

public final class DistExecutor {


    public static <T> void unsafeCallWhenOn(EnvType env, Supplier<Callable<T>> toRun) {
        if (env == FabricLoader.getInstance().getEnvironmentType()) {
            try {
                toRun.get().call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
