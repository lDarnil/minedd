package com.randomevents.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The 15 positive events. */
public final class PositiveEvents {
	private PositiveEvents() {
	}

	/** XP baseline per player for Double XP (cleared in onEnd, so nothing leaks). */
	private static final Map<UUID, Integer> XP_BASELINE = new HashMap<>();

	private record Loot(Item item, int min, int max) {
	}

	private static final Loot[] LUCKY_POOL = {
		new Loot(Items.DIAMOND, 1, 3), new Loot(Items.EMERALD, 3, 8), new Loot(Items.GOLD_INGOT, 4, 10),
		new Loot(Items.IRON_INGOT, 6, 14), new Loot(Items.GOLDEN_APPLE, 1, 2), new Loot(Items.ENDER_PEARL, 2, 4),
		new Loot(Items.EXPERIENCE_BOTTLE, 4, 8), new Loot(Items.COOKED_BEEF, 6, 12)
	};

	private static final Loot[] RAIN_POOL = {
		new Loot(Items.BREAD, 2, 4), new Loot(Items.COOKED_BEEF, 2, 4), new Loot(Items.IRON_INGOT, 1, 3),
		new Loot(Items.GOLD_INGOT, 1, 2), new Loot(Items.ARROW, 4, 8), new Loot(Items.TORCH, 4, 8),
		new Loot(Items.COAL, 3, 6), new Loot(Items.EXPERIENCE_BOTTLE, 1, 2)
	};

	private static int roll(RandomSource random, Loot loot) {
		return loot.min() + random.nextInt(loot.max() - loot.min() + 1);
	}

	public static void registerAll() {
		// 1
		EventRegistry.register(EventBuilder.positive("lucky_day", "Lucky Day",
				"Ai primit obiecte valoroase aleatorii!", 0)
			.onStart((p, t) -> {
				RandomSource random = p.getRandom();
				boolean[] used = new boolean[LUCKY_POOL.length];
				int given = 0;
				while (given < 4) {
					int i = random.nextInt(LUCKY_POOL.length);
					if (used[i]) {
						continue;
					}
					used[i] = true;
					Fx.give(p, LUCKY_POOL[i].item(), roll(random, LUCKY_POOL[i]));
					given++;
				}
			}).build());

		// 2
		EventRegistry.register(EventBuilder.positive("regeneration", "Regeneration",
				"Viata ti se regenereaza rapid.", 45)
			.effects(Fx.fx(MobEffects.REGENERATION, 1)).build());

		// 3
		EventRegistry.register(EventBuilder.positive("super_speed", "Super Speed",
				"Te misti mult mai repede.", 60)
			.effects(Fx.fx(MobEffects.SPEED, 2)).build());

		// 4
		EventRegistry.register(EventBuilder.positive("jump_boost", "Jump Boost",
				"Poti sari mult mai sus.", 60)
			.effects(Fx.fx(MobEffects.JUMP_BOOST, 2)).build());

		// 5 - doubles every XP gain (detected as a positive change of the player's total XP)
		EventRegistry.register(EventBuilder.positive("double_xp", "Double XP",
				"Primesti dublu experienta.", 90)
			.onStart((p, t) -> XP_BASELINE.put(p.getUUID(), p.totalExperience))
			.onResume((p, t) -> XP_BASELINE.put(p.getUUID(), p.totalExperience))
			.onTick(2, (p, elapsed, total) -> {
				int now = p.totalExperience;
				Integer before = XP_BASELINE.get(p.getUUID());
				if (before != null && now > before) {
					p.giveExperiencePoints(now - before);
					now = p.totalExperience;
				}
				XP_BASELINE.put(p.getUUID(), now);
			})
			.onEnd(p -> XP_BASELINE.remove(p.getUUID()))
			.build());

		// 6 - items fall from the sky, one every half second, around the player
		EventRegistry.register(EventBuilder.positive("treasure_rain", "Treasure Rain",
				"Obiecte folositoare cad din cer in jurul tau!", 10)
			.onTick(10, (p, elapsed, total) -> {
				if (elapsed > 100) {
					return;
				}
				ServerLevel level = Fx.level(p);
				RandomSource random = p.getRandom();
				double x = p.getX() + (random.nextDouble() - 0.5) * 8.0;
				double z = p.getZ() + (random.nextDouble() - 0.5) * 8.0;
				double y = p.getY() + 6.0;
				if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
					y = p.getY() + 2.0; // low ceiling (cave): drop closer
				}
				Loot loot = RAIN_POOL[random.nextInt(RAIN_POOL.length)];
				level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(loot.item(), roll(random, loot))));
			}).build());

		// 7
		EventRegistry.register(EventBuilder.positive("full_health", "Full Health",
				"Viata si foamea ti-au fost refacute complet.", 0)
			.onStart((p, t) -> {
				p.setHealth(p.getMaxHealth());
				p.getFoodData().setFoodLevel(20);
			}).build());

		// 8
		EventRegistry.register(EventBuilder.positive("haste", "Haste",
				"Spargi blocurile mult mai repede.", 60)
			.effects(Fx.fx(MobEffects.HASTE, 2)).build());

		// 9
		EventRegistry.register(EventBuilder.positive("night_vision", "Night Vision",
				"Vezi clar chiar si in intuneric.", 120)
			.effects(Fx.fx(MobEffects.NIGHT_VISION, 0)).build());

		// 10
		EventRegistry.register(EventBuilder.positive("diamond_gift", "Diamond Gift",
				"Ai primit diamante!", 0)
			.onStart((p, t) -> Fx.give(p, Items.DIAMOND, 3 + p.getRandom().nextInt(4))).build());

		// 11
		EventRegistry.register(EventBuilder.positive("resistance", "Resistance",
				"Primesti mult mai putine daune.", 60)
			.effects(Fx.fx(MobEffects.RESISTANCE, 1)).build());

		// 12
		EventRegistry.register(EventBuilder.positive("hero_power", "Hero Power",
				"O combinatie de puteri: viteza, forta, saritura, regenerare si rezistenta.", 90)
			.effects(Fx.fx(MobEffects.SPEED, 1), Fx.fx(MobEffects.JUMP_BOOST, 1), Fx.fx(MobEffects.STRENGTH, 1),
				Fx.fx(MobEffects.REGENERATION, 1), Fx.fx(MobEffects.HASTE, 1), Fx.fx(MobEffects.RESISTANCE, 0),
				Fx.fx(MobEffects.ABSORPTION, 1)).build());

		// 13
		EventRegistry.register(EventBuilder.positive("friendly_animals", "Friendly Animals",
				"Au aparut animale prietenoase langa tine.", 0)
			.onStart((p, t) -> {
				EntityType<?>[] animals = {EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN};
				for (int i = 0; i < 5; i++) {
					Fx.spawnAround(p, animals[p.getRandom().nextInt(animals.length)], 3, 7);
				}
			}).build());

		// 14
		EventRegistry.register(EventBuilder.positive("golden_touch", "Golden Touch",
				"Ai primit lingouri de aur!", 0)
			.onStart((p, t) -> Fx.give(p, Items.GOLD_INGOT, 8 + p.getRandom().nextInt(9))).build());

		// 15 - removes hostile mobs near the player now and keeps the area clear for the duration
		EventRegistry.register(EventBuilder.positive("peaceful_moment", "Peaceful Moment",
				"Monstrii ostili din apropiere au disparut.", 30)
			.onStart((p, t) -> {
				for (Mob mob : Fx.hostilesAround(p, 48.0)) {
					mob.discard();
				}
			})
			.onTick(20, (p, elapsed, total) -> {
				for (Mob mob : Fx.hostilesAround(p, 32.0)) {
					mob.discard();
				}
			}).build());
	}
}
