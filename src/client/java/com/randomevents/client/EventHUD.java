package com.randomevents.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Draws (1) a compact panel at the top-left and (2) the animated centre-screen announcement.
 * Green = positive, red = negative. Only the local player's own data is ever shown, because the
 * server only sends each player their own state.
 */
public final class EventHUD {
	private static final int GREEN = 0x55FF55;
	private static final int RED = 0xFF5555;
	private static final int GRAY = 0xAAAAAA;
	private static final int WHITE = 0xFFFFFF;

	private static final long POPUP_TOTAL_MS = 4500;
	private static final long FADE_IN_MS = 350;
	private static final long FADE_OUT_MS = 800;

	private EventHUD() {
	}

	private static int argb(int alpha, int rgb) {
		return (Mth.clamp(alpha, 5, 255) << 24) | (rgb & 0xFFFFFF);
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientEventState.hudEnabled) {
			return;
		}
		Font font = mc.font;
		renderPanel(graphics, font);
		renderPopup(graphics, font);
	}

	private static String clock(long millis) {
		long s = Math.max(0, (millis + 999) / 1000);
		return String.format("%d:%02d", s / 60, s % 60);
	}

	private static void renderPanel(GuiGraphicsExtractor graphics, Font font) {
		long now = System.currentTimeMillis();
		List<ClientEventState.ActiveView> active = ClientEventState.active;

		int x = 6;
		int y = 6;
		int width = 170;
		int lines = 2 + (ClientEventState.hasLast ? 1 : 0) + active.size() * 2;
		int height = 8 + lines * 10;
		graphics.fill(x, y, x + width, y + height, 0x90000000);
		graphics.fill(x, y, x + 2, y + height, argb(255, ClientEventState.hasLast
			? (ClientEventState.lastPositive ? GREEN : RED) : GRAY));

		int ty = y + 5;
		String next = ClientEventState.running
			? "Urmatorul eveniment: " + clock(ClientEventState.nextEventAtMillis - now)
			: "Sistem oprit";
		graphics.text(font, next, x + 7, ty, argb(255, WHITE));
		ty += 10;
		graphics.text(font, ClientEventState.hasLast ? "Ultimul:" : "Ultimul: -", x + 7, ty, argb(255, GRAY));
		ty += 10;
		if (ClientEventState.hasLast) {
			graphics.text(font, ClientEventState.lastName + (ClientEventState.lastPositive ? "  [+]" : "  [-]"),
				x + 7, ty, argb(255, ClientEventState.lastPositive ? GREEN : RED));
			ty += 10;
		}
		for (ClientEventState.ActiveView a : active) {
			int color = a.positive() ? GREEN : RED;
			long remaining = a.endsAtMillis() - now;
			graphics.text(font, a.name() + "  " + clock(remaining), x + 7, ty, argb(255, color));
			ty += 10;
			// progress bar of the remaining time
			float frac = a.totalMillis() <= 0 ? 0f : Mth.clamp(remaining / (float) a.totalMillis(), 0f, 1f);
			graphics.fill(x + 7, ty + 1, x + width - 7, ty + 4, 0x80000000);
			graphics.fill(x + 7, ty + 1, x + 7 + (int) ((width - 14) * frac), ty + 4, argb(255, color));
			ty += 10;
		}
	}

	private static void renderPopup(GuiGraphicsExtractor graphics, Font font) {
		long start = ClientEventState.popupStartMillis;
		if (start < 0) {
			return;
		}
		long age = System.currentTimeMillis() - start;
		if (age > POPUP_TOTAL_MS) {
			ClientEventState.popupStartMillis = -1;
			return;
		}
		float alpha = 1f;
		float scale = 1f;
		if (age < FADE_IN_MS) {
			float t = age / (float) FADE_IN_MS;
			alpha = t;
			scale = 0.6f + 0.4f * easeOutBack(t); // pops in with a small overshoot
		} else if (age > POPUP_TOTAL_MS - FADE_OUT_MS) {
			alpha = (POPUP_TOTAL_MS - age) / (float) FADE_OUT_MS;
		}
		int a = (int) (alpha * 255);
		int color = ClientEventState.popupPositive ? GREEN : RED;

		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 3;

		// dark band behind the text
		int halfW = 150;
		graphics.fill(cx - halfW, cy - 22, cx + halfW, cy + 28, argb((int) (alpha * 140), 0x000000));
		graphics.fill(cx - halfW, cy - 22, cx + halfW, cy - 20, argb(a, color));
		graphics.fill(cx - halfW, cy + 26, cx + halfW, cy + 28, argb(a, color));

		// title, scaled
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx, cy - 6);
		graphics.pose().scale(scale * 2.2f);
		graphics.centeredText(font, ClientEventState.popupName.toUpperCase(), 0, -4, argb(a, color));
		graphics.pose().popMatrix();

		graphics.centeredText(font, ClientEventState.popupDescription, cx, cy + 12, argb(a, WHITE));
	}

	private static float easeOutBack(float t) {
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		float p = t - 1f;
		return 1f + c3 * p * p * p + c1 * p * p;
	}
}
