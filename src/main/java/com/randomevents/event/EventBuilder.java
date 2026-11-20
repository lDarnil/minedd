package com.randomevents.event;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/** Small fluent builder so every event is a few lines instead of a whole class. */
public final class EventBuilder {
	public interface StartAction {
		void run(ServerPlayer player, int ticks);
	}

	public interface TickAction {
		void run(ServerPlayer player, int elapsedTicks, int durationTicks);
	}

	private final String id;
	private final String name;
	private final String description;
	private final EventType type;
	private final int seconds;
	private StartAction start = (p, d) -> {
	};
	private TickAction tick;
	private int interval;
	private Consumer<ServerPlayer> end = p -> {
	};
	private StartAction resume = (p, remaining) -> {
	};

	private EventBuilder(EventType type, String id, String name, String description, int seconds) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.description = description;
		this.seconds = seconds;
	}

	public static EventBuilder positive(String id, String name, String description, int seconds) {
		return new EventBuilder(EventType.POSITIVE, id, name, description, seconds);
	}

	public static EventBuilder negative(String id, String name, String description, int seconds) {
		return new EventBuilder(EventType.NEGATIVE, id, name, description, seconds);
	}

	public EventBuilder onStart(StartAction action) {
		this.start = action;
		return this;
	}

	public EventBuilder onTick(int everyTicks, TickAction action) {
		this.interval = everyTicks;
		this.tick = action;
		return this;
	}

	public EventBuilder onEnd(Consumer<ServerPlayer> action) {
		this.end = action;
		return this;
	}

	public EventBuilder onResume(StartAction action) {
		this.resume = action;
		return this;
	}

	/** Event made only of potion effects: applied on start, re-applied on resume, removed on end. */
	public EventBuilder effects(Fx.Spec... specs) {
		StartAction apply = (p, ticks) -> {
			for (Fx.Spec spec : specs) {
				Fx.apply(p, spec, ticks);
			}
		};
		this.start = apply;
		this.resume = apply;
		this.end = p -> {
			for (Fx.Spec spec : specs) {
				Fx.remove(p, spec);
			}
		};
		return this;
	}

	public RandomEvent build() {
		final StartAction s = start;
		final TickAction t = tick;
		final int iv = interval;
		final Consumer<ServerPlayer> e = end;
		final StartAction r = resume;
		final String fId = id;
		final String fName = name;
		final String fDesc = description;
		final EventType fType = type;
		final int fSeconds = seconds;
		return new RandomEvent() {
			@Override
			public String id() {
				return fId;
			}

			@Override
			public String displayName() {
				return fName;
			}

			@Override
			public String description() {
				return fDesc;
			}

			@Override
			public EventType type() {
				return fType;
			}

			@Override
			public int defaultDurationSeconds() {
				return fSeconds;
			}

			@Override
			public void start(ServerPlayer player, int durationTicks) {
				s.run(player, durationTicks);
			}

			@Override
			public int tickInterval() {
				return t == null ? 0 : iv;
			}

			@Override
			public void tick(ServerPlayer player, int elapsedTicks, int durationTicks) {
				if (t != null) {
					t.run(player, elapsedTicks, durationTicks);
				}
			}

			@Override
			public void end(ServerPlayer player) {
				e.accept(player);
			}

			@Override
			public void resume(ServerPlayer player, int remainingTicks) {
				r.run(player, remainingTicks);
			}
		};
	}
}
