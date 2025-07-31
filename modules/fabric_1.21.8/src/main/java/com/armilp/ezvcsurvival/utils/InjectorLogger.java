package com.armilp.ezvcsurvival.utils;

import com.armilp.ezvcsurvival.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InjectorLogger {
    public static void logInfo(Class<?> clazz, String message) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.info(message);
    }



    @SuppressWarnings("unused")
    public static void logDebug(Class<?> clazz, String message) {
        if (Plugin.DEBUG) {
            Logger logger = LoggerFactory.getLogger(clazz);
            logger.debug(message);
        }
    }

    public static void logError(Class<?> clazz, String message, Throwable throwable) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message, throwable);
    }

}
