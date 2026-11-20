package com.randomevents;

import com.randomevents.command.EventCommands;
import com.randomevents.config.EventConfig;
import com.randomevents.event.EventManager;
import com.randomevents.event.EventRegistry;
import com.randomevents.event.EventScheduler;
import com.randomevents.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomEventsMod implements ModInitializer {
	public static final String MOD_ID = "random_events";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EventRegistry.registerBuiltIn();      // 15 positive + 15 negative
		EventConfig.load();                   // needs the registry (fills in per-event defaults)
		NetworkHandler.registerPayloads();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
			EventCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(EventManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(EventManager::onServerStopping);
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> EventManager.saveAll(server));
		ServerTickEvents.END_SERVER_TICK.register(EventScheduler::tick);

		ServerPlayerEvents.JOIN.register(EventManager::onJoin);
		ServerPlayerEvents.LEAVE.register(EventManager::onLeave);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> EventManager.onRespawn(newPlayer, alive));

		LOGGER.info("Random Events loaded with {} events.", EventRegistry.all().size());
	}
}
