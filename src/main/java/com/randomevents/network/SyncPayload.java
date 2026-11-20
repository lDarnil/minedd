package com.randomevents.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Server -> client snapshot of one player's event state (sent about once per second). */
public record SyncPayload(boolean running, boolean hudEnabled, int ticksUntilNext, boolean hasLast,
						boolean lastPositive, String lastName, String lastDescription,
						List<Entry> active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("random_events", "sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
		CustomPacketPayload.codec(SyncPayload::write, SyncPayload::new);

	public record Entry(String name, String description, boolean positive, int remainingTicks, int totalTicks) {
	}

	public SyncPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
			buf.readUtf(), buf.readUtf(), readEntries(buf));
	}

	private static List<Entry> readEntries(RegistryFriendlyByteBuf buf) {
		int n = buf.readVarInt();
		List<Entry> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
		}
		return list;
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(running);
		buf.writeBoolean(hudEnabled);
		buf.writeVarInt(ticksUntilNext);
		buf.writeBoolean(hasLast);
		buf.writeBoolean(lastPositive);
		buf.writeUtf(lastName);
		buf.writeUtf(lastDescription);
		buf.writeVarInt(active.size());
		for (Entry e : active) {
			buf.writeUtf(e.name());
			buf.writeUtf(e.description());
			buf.writeBoolean(e.positive());
			buf.writeVarInt(e.remainingTicks());
			buf.writeVarInt(e.totalTicks());
		}
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
