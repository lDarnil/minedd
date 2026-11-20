package com.randomevents.event;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers shared by the events. Every direct vanilla call that is likely to change between
 * Minecraft versions lives here or in the event classes, so a version bump touches few places.
 */
public final class Fx {
	private Fx() {
	}

	public record Spec(Holder<MobEffect> effect, int amplifier) {
	}

	public static Spec fx(Holder<MobEffect> effect, int amplifier) {
		return new Spec(effect, amplifier);
	}

	public static void apply(ServerPlayer player, Spec spec, int ticks) {
		player.addEffect(new MobEffectInstance(spec.effect(), Math.max(ticks, 20), spec.amplifier()));
	}

	public static void remove(ServerPlayer player, Spec spec) {
		player.removeEffect(spec.effect());
	}

	public static ServerLevel level(ServerPlayer player) {
		return (ServerLevel) player.level();
	}

	/** Gives items; whatever does not fit in the inventory is dropped at the player's feet. */
	public static void give(ServerPlayer player, Item item, int count) {
		ItemStack stack = new ItemStack(item, count);
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			player.drop(stack, false, true);
		}
	}

	/** Hostile mobs (MobCategory.MONSTER) within {@code radius} blocks of the player. */
	public static List<Mob> hostilesAround(ServerPlayer player, double radius) {
		List<Mob> result = new ArrayList<>();
		for (Mob mob : level(player).getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
			if (mob.getType().getCategory() == MobCategory.MONSTER) {
				result.add(mob);
			}
		}
		return result;
	}

	/**
	 * Spawns one entity at a random free spot between minRadius and maxRadius around the player.
	 * Returns null when no free spot was found (nothing is spawned in that case).
	 */
	public static <T extends Entity> T spawnAround(ServerPlayer player, EntityType<T> type, int minRadius, int maxRadius) {
		ServerLevel level = level(player);
		T entity = type.create(level, EntitySpawnReason.EVENT);
		if (entity == null) {
			return null;
		}
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double dist = minRadius + random.nextDouble() * (maxRadius - minRadius);
			double x = player.getX() + Math.cos(angle) * dist;
			double z = player.getZ() + Math.sin(angle) * dist;
			for (int dy = -2; dy <= 2; dy++) {
				entity.setPos(x, player.getY() + dy, z);
				if (level.noCollision(entity)) {
					level.addFreshEntity(entity);
					return entity;
				}
			}
		}
		entity.discard();
		return null;
	}
}
