package com.randomevents.mixin;

import com.randomevents.event.InventoryLock;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Lost Items": while the event is active the inventory cannot be changed (no clicks, no dropping,
 * no hand swapping). Nothing is deleted or dropped. {@code require = 0} means that if a future
 * Minecraft version renames a target method, the game still starts and only this one event
 * becomes inactive (see the notes in README).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true, require = 0)
	private void randomEvents$lockContainerClicks(ServerboundContainerClickPacket packet, CallbackInfo ci) {
		if (InventoryLock.isLocked(this.player.getUUID())) {
			ci.cancel();
			this.player.containerMenu.sendAllDataToRemote(); // put the client view back to the real state
		}
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true, require = 0)
	private void randomEvents$lockDropAndSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
		if (!InventoryLock.isLocked(this.player.getUUID())) {
			return;
		}
		ServerboundPlayerActionPacket.Action action = packet.getAction();
		if (action == ServerboundPlayerActionPacket.Action.DROP_ITEM
			|| action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
			|| action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			ci.cancel();
			this.player.containerMenu.sendAllDataToRemote();
		}
	}
}
