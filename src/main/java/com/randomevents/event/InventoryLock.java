package com.randomevents.event;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Players whose inventory is currently locked by "Lost Items". Server thread only. */
public final class InventoryLock {
	private static final Set<UUID> LOCKED = new HashSet<>();

	private InventoryLock() {
	}

	public static void lock(UUID id) {
		LOCKED.add(id);
	}

	public static void unlock(UUID id) {
		LOCKED.remove(id);
	}

	public static boolean isLocked(UUID id) {
		return !LOCKED.isEmpty() && LOCKED.contains(id);
	}

	public static void clear() {
		LOCKED.clear();
	}
}
