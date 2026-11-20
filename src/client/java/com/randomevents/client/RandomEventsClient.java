package com.randomevents.client;

import com.randomevents.network.SyncPayload;
import com.randomevents.network.TriggerPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class RandomEventsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Receivers run on the render thread (documented by Fabric API), so touching state is safe.
		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE,
			(payload, context) -> ClientEventState.applySync(payload));
		ClientPlayNetworking.registerGlobalReceiver(TriggerPayload.TYPE,
			(payload, context) -> ClientEventState.applyTrigger(payload));

		// Forget everything when leaving a world/server so nothing from it shows in the next one.
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientEventState.reset());

		HudElementRegistry.attachElementAfter(VanillaHudElements.TITLE_AND_SUBTITLE,
			Identifier.fromNamespaceAndPath("random_events", "hud"), EventHUD::render);
	}
}
