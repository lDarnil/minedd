package com.randomevents.event;

public enum EventType {
	POSITIVE, NEGATIVE;

	public boolean isPositive() {
		return this == POSITIVE;
	}
}
