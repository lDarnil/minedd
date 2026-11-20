package com.randomevents.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: an event just started for this player (drives the centre-screen animation). */
public record TriggerPayload(String name, String description, boolean positive, int durationTicks)
	implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TriggerPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("random_events", "trigger"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> CODEC =
		CustomPacketPayload.codec(TriggerPayload::write, TriggerPayload::new);

	public TriggerPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt());
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeUtf(name);
		buf.writeUtf(description);
		buf.writeBoolean(positive);
		buf.writeVarInt(durationTicks);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
