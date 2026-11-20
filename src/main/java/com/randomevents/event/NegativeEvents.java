package com.randomevents.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The 15 negative events. */
public final class NegativeEvents {
	private NegativeEvents() {
	}

	private static final Identifier GRAVITY_ID = Identifier.fromNamespaceAndPath("random_events", "gravity_trouble");

	/** Effects picked by Bad Luck per player, so only those are removed at the end. */
	private static final Map<UUID, List<Fx.Spec>> BAD_LUCK = new HashMap<>();

	private static void setJumpPenalty(ServerPlayer p, boolean on) {
		AttributeInstance jump = p.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump == null) {
			return;
		}
		jump.removeModifier(GRAVITY_ID);
		if (on) {
			// base jump strength is 0.42 -> 0.20: a full block can no longer be climbed by jumping
			jump.addTransientModifier(new AttributeModifier(GRAVITY_ID, -0.22, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	private static List<Fx.Spec> badLuckPool() {
		return new ArrayList<>(List.of(
			Fx.fx(MobEffects.BLINDNESS, 0), Fx.fx(MobEffects.SLOWNESS, 1), Fx.fx(MobEffects.WEAKNESS, 1),
			Fx.fx(MobEffects.MINING_FATIGUE, 1), Fx.fx(MobEffects.POISON, 0), Fx.fx(MobEffects.DARKNESS, 0),
			Fx.fx(MobEffects.HUNGER, 1)));
	}

	public static void registerAll() {
		// 1
		EventRegistry.register(EventBuilder.negative("darkness", "Darkness",
				"Lumea se intuneca in jurul tau.", 60)
			.effects(Fx.fx(MobEffects.DARKNESS, 0)).build());

		// 2
		EventRegistry.register(EventBuilder.negative("slowness", "Slowness",
				"Te misti mult mai incet.", 45)
			.effects(Fx.fx(MobEffects.SLOWNESS, 1)).build());

		// 3
		EventRegistry.register(EventBuilder.negative("weakness", "Weakness",
				"Atacurile tale sunt mai slabe.", 60)
			.effects(Fx.fx(MobEffects.WEAKNESS, 1)).build());

		// 4
		EventRegistry.register(EventBuilder.negative("hunger", "Hunger",
				"Ai pierdut din sațietate si flamanzesti mai repede.", 30)
			.onStart((p, t) -> {
				FoodData food = p.getFoodData();
				food.setFoodLevel(Math.max(0, food.getFoodLevel() - 8));
				Fx.apply(p, Fx.fx(MobEffects.HUNGER, 1), t);
			})
			.onResume((p, t) -> Fx.apply(p, Fx.fx(MobEffects.HUNGER, 1), t))
			.onEnd(p -> Fx.remove(p, Fx.fx(MobEffects.HUNGER, 1)))
			.build());

		// 5 - weather is global in Minecraft, so this storm is visible to everybody in the world.
		EventRegistry.register(EventBuilder.negative("thunderstorm", "Thunderstorm",
				"A inceput o furtuna cu tunete!", 75)
			.onStart((p, t) -> Fx.level(p).getServer().runCommand("weather thunder " + Math.max(1, t / 20) + "s"))
			.build());

		// 6
		EventRegistry.register(EventBuilder.negative("blindness", "Blindness",
				"Vederea ta este puternic limitata.", 30)
			.effects(Fx.fx(MobEffects.BLINDNESS, 0)).build());

		// 7
		EventRegistry.register(EventBuilder.negative("mining_fatigue", "Mining Fatigue",
				"Spargi blocurile mult mai greu.", 60)
			.effects(Fx.fx(MobEffects.MINING_FATIGUE, 1)).build());

		// 8
		EventRegistry.register(EventBuilder.negative("mob_invasion", "Mob Invasion",
				"Monstri ostili te inconjoara!", 0)
			.onStart((p, t) -> {
				EntityType<?>[] mobs = {EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER,
					EntityType.CREEPER, EntityType.SKELETON, EntityType.ZOMBIE};
				for (EntityType<?> type : mobs) {
					var entity = Fx.spawnAround(p, type, 6, 11);
					if (entity instanceof Mob mob) {
						if (type == EntityType.SKELETON) {
							mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
						}
						mob.setTarget(p);
					}
				}
			}).build());

		// 9
		EventRegistry.register(EventBuilder.negative("poison", "Poison",
				"Esti otravit (nu poate sa te omoare).", 30)
			.effects(Fx.fx(MobEffects.POISON, 0)).build());

		// 10 - jumping is much weaker for the duration (attribute modifier, removed at the end)
		EventRegistry.register(EventBuilder.negative("gravity_trouble", "Gravity Trouble",
				"Sariturile tale sunt mult mai slabe.", 45)
			.onStart((p, t) -> setJumpPenalty(p, true))
			.onResume((p, t) -> setJumpPenalty(p, true))
			.onEnd(p -> setJumpPenalty(p, false))
			.build());

		// 11 - the inventory is locked by ServerGamePacketListenerImplMixin; nothing is deleted or dropped
		EventRegistry.register(EventBuilder.negative("lost_items", "Lost Items",
				"Inventarul tau este blocat temporar (nu pierzi nimic).", 45)
			.onStart((p, t) -> InventoryLock.lock(p.getUUID()))
			.onResume((p, t) -> InventoryLock.lock(p.getUUID()))
			.onEnd(p -> InventoryLock.unlock(p.getUUID()))
			.build());

		// 12 - nearby hostile mobs get Speed II + Strength II, refreshed every 2 seconds
		EventRegistry.register(EventBuilder.negative("cursed_night", "Cursed Night",
				"Monstrii din jurul tau sunt mai rapizi si mai puternici.", 90)
			.onTick(40, (p, elapsed, total) -> {
				for (Mob mob : Fx.hostilesAround(p, 40.0)) {
					mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
					mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1));
				}
			}).build());

		// 13 - flame particles around the player; the player is set on fire for 2s every 8s.
		// No fire block is ever placed, so buildings are never burned.
		EventRegistry.register(EventBuilder.negative("firestorm", "Firestorm",
				"Flacari te inconjoara! (constructiile nu iau foc)", 30)
			.onTick(10, (p, elapsed, total) -> {
				ServerLevel level = Fx.level(p);
				level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 20, 3.0, 1.5, 3.0, 0.02);
				level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1.5, p.getZ(), 4, 2.5, 1.0, 2.5, 0.0);
				if (elapsed % 160 == 0) {
					p.setRemainingFireTicks(40);
				}
			}).build());

		// 14 - three different random negative effects
		EventRegistry.register(EventBuilder.negative("bad_luck", "Bad Luck",
				"Mai multe efecte negative aleatorii te lovesc.", 30)
			.onStart((p, t) -> {
				List<Fx.Spec> pool = badLuckPool();
				Collections.shuffle(pool, new java.util.Random(p.getRandom().nextLong()));
				List<Fx.Spec> chosen = new ArrayList<>(pool.subList(0, 3));
				BAD_LUCK.put(p.getUUID(), chosen);
				for (Fx.Spec spec : chosen) {
					Fx.apply(p, spec, t);
				}
			})
			.onResume((p, t) -> {
				List<Fx.Spec> chosen = BAD_LUCK.get(p.getUUID());
				if (chosen != null) {
					for (Fx.Spec spec : chosen) {
						Fx.apply(p, spec, t);
					}
				}
			})
			.onEnd(p -> {
				List<Fx.Spec> chosen = BAD_LUCK.remove(p.getUUID());
				if (chosen != null) {
					for (Fx.Spec spec : chosen) {
						Fx.remove(p, spec);
					}
				}
			}).build());

		// 15 - Wither can kill: lower the duration in the config if you find it too harsh
		EventRegistry.register(EventBuilder.negative("wither_curse", "Wither Curse",
				"Esti afectat de blestemul Wither (poate fi letal!).", 30)
			.effects(Fx.fx(MobEffects.WITHER, 0)).build());
	}
}
