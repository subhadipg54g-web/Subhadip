package com.example.afkprogression;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. AFK Progression is a <b>client-side</b> mod: it adds nothing to the server and registers
 * no blocks, items or packets. All behaviour lives in {@code client.AFKProgressionClient}; this class only
 * carries the mod id and logger so no client-only class is ever touched from a common context.
 */
public final class AFKProgression implements ModInitializer {
    public static final String MOD_ID = "afkprogression";
    public static final String NAME = "AFK Progression";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("[AFK] {} loaded - client-side automation for single-player, LAN and private test worlds", NAME);
    }
}
