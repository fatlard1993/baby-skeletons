package justfatlard.baby_skeletons.gametest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import justfatlard.baby_skeletons.Babies;
import justfatlard.baby_skeletons.Jockeys;
import justfatlard.baby_skeletons.Jockeys.Mount;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import static justfatlard.baby_skeletons.gametest.Kit.check;

/**
 * Babies ride: vanilla's chicken jockey for every kind, and each kind's own mount where it fits.
 * About as many jockeys among the night's spawns as vanilla makes of baby zombies; each pairing
 * spawns, stays on, and is carried toward what the baby is fighting; the mounts do not turn on
 * their riders, throw them, or vanish from under them; and a calf stays a calf.
 */
public final class BabiesRideJockeys implements FabricClientGameTest {

	/** Per kind. A twentieth are babies and about a twentieth of those ride: about 47 jockeys in all. */
	private static final int SPAWNS_EACH = 4000;

	/** Which kind of baby rides each mount. */
	static final Map<Mount, EntityType<? extends AbstractSkeleton>> RIDER = new EnumMap<>(Map.of(
		Mount.CHICKEN, EntityTypes.SKELETON,
		Mount.CAVE_SPIDER, EntityTypes.SKELETON,
		Mount.GOAT_KID, EntityTypes.STRAY,
		Mount.FROG, EntityTypes.BOGGED,
		Mount.MOOSHROOM_CALF, EntityTypes.BOGGED,
		Mount.ARMADILLO, EntityTypes.PARCHED,
		Mount.BABY_HOGLIN, EntityTypes.WITHER_SKELETON));

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, Kit.MOD_ID, session -> {
			Kit.night(session);
			// Out of the fight: a creative player is nobody's target, so the babies fight what they are given.
			Kit.command(session, "gamemode creative @a");
			Kit.command(session, "gamerule spawn_mobs false");

			rates(session);
			habitats(session);
			steering(session);
			goatsDoNotRamTheirRiders(session);
			armadillosDoNotRollUp(session);
			calvesStayCalves(session);
			despawning(session);
			summoned(session);
			Kit.passed("BabiesRideJockeys");
		});
	}

	/** About vanilla's share: among natural spawns, as many jockeys as a baby zombie's rolls make. */
	private static void rates(Smoke.Session session) {
		int[] counts = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			BlockPos at = Kit.ground(session, level, 0, -40);
			// Nothing to climb on but what the babies bring: the first roll finds nothing, as on open ground.
			level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(16)).forEach(Entity::discard);
			int babies = 0, jockeys = 0, spawned = 0;
			for (EntityType<? extends AbstractSkeleton> type : Kit.FAMILY) {
				for (int i = 0; i < SPAWNS_EACH; i++) {
					AbstractSkeleton skeleton = type.create(level, EntitySpawnReason.NATURAL);
					skeleton.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
					skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
					spawned++;
					if (Babies.is(skeleton)) babies++;
					Entity mount = skeleton.getVehicle();
					if (mount != null) {
						jockeys++;
						if (!Babies.is(skeleton)) throw new AssertionError("a grown skeleton came out riding " + mount);
						mount.discard();
					}
					skeleton.discard();
				}
			}
			return new int[] {spawned, babies, jockeys};
		});
		// Vanilla's two rolls on open ground: 1/20 of 19/20 of the babies bring a mount, and the
		// babies are 1/20 of the spawns. 20000 spawns, so about 47, with a spread of about 7.
		double expected = counts[0] * Babies.SPAWN_CHANCE * (1 - Jockeys.SEARCH_CHANCE) * Jockeys.SPAWN_CHANCE;
		System.out.println("[baby-skeletons test] jockeys: " + counts[2] + " of " + counts[0] + " natural spawns ("
			+ counts[1] + " babies), vanilla's rolls expect " + Math.round(expected));
		check(counts[2] >= 20 && counts[2] <= 85, "jockeys among " + counts[0] + " natural spawns: " + counts[2]
			+ ", expected about " + Math.round(expected));

		// The first roll: a baby climbs on the right mount already standing near it, and only that.
		String climbed = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			BlockPos at = Kit.ground(session, level, 0, -40);
			Goat grown = Kit.spawn(level, EntityTypes.GOAT, at.east(2), EntitySpawnReason.NATURAL);
			grown.setBaby(false);
			AbstractSkeleton stray = Kit.baby(level, EntityTypes.STRAY, at);
			if (Jockeys.climbNearby(stray, level, Mount.GOAT_KID) != null) return "climbed on a grown goat";
			Goat kid = Kit.spawn(level, EntityTypes.GOAT, at.west(2), EntitySpawnReason.NATURAL);
			kid.setBaby(true);
			Mob ride = Jockeys.climbNearby(stray, level, Mount.GOAT_KID);
			if (ride != kid) return "did not climb on the goat kid beside it: " + ride;
			if (!Jockeys.isMount(kid) || !kid.isAgeLocked()) return "the kid climbed on is not made a jockey's";
			Chicken hen = Kit.spawn(level, EntityTypes.CHICKEN, at.north(2), EntitySpawnReason.NATURAL);
			AbstractSkeleton skeleton = Kit.baby(level, EntityTypes.SKELETON, at.south(1));
			if (Jockeys.climbNearby(skeleton, level, Mount.CHICKEN) != hen) return "did not climb on the chicken beside it";
			if (!hen.isChickenJockey()) return "the chicken is not marked a jockey's, as vanilla marks it";
			for (Entity e : List.of(grown, stray, kid, hen, skeleton)) e.discard();
			return "ok";
		});
		check("ok".equals(climbed), "climbing on: " + climbed);
	}

	/** Each kind takes the mount its country gives it, and a chicken anywhere else. */
	private static void habitats(Smoke.Session session) {
		// Close by, where the chunks are loaded, which /fillbiome asks.
		BlockPos base = Kit.ask(session, server -> Kit.ground(session, server.overworld(), -24, 8));
		// Four patches of country, side by side, and a sealed stone room for a cave.
		String[] biomes = {"minecraft:snowy_plains", "minecraft:swamp", "minecraft:desert", "minecraft:crimson_forest"};
		for (int i = 0; i < biomes.length; i++) {
			BlockPos from = base.offset(i * 8, -3, 0);
			Kit.command(session, "fillbiome %d %d %d %d %d %d %s".formatted(
				from.getX(), from.getY(), from.getZ(), from.getX() + 7, from.getY() + 24, from.getZ() + 7, biomes[i]));
		}
		BlockPos cave = base.offset(36, 0, 4);
		Kit.command(session, "fill %d %d %d %d %d %d minecraft:stone hollow".formatted(
			cave.getX() - 2, cave.getY() - 1, cave.getZ() - 2, cave.getX() + 2, cave.getY() + 3, cave.getZ() + 2));
		session.waitTicks(20);

		String found = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			RandomSource random = level.getRandom();
			List<String> wrong = new ArrayList<>();
			Map<String, Mount> expect = new LinkedHashMap<>();
			BlockPos snow = base.offset(4, 0, 4), swamp = base.offset(12, 0, 4), desert = base.offset(20, 0, 4),
				nether = base.offset(28, 0, 4), open = base.offset(4, 0, -16);
			expect.put("stray in the snow", ask(level, EntityTypes.STRAY, snow, random));
			expect.put("parched in the desert", ask(level, EntityTypes.PARCHED, desert, random));
			expect.put("wither skeleton in the nether", ask(level, EntityTypes.WITHER_SKELETON, nether, random));
			expect.put("skeleton in a cave", ask(level, EntityTypes.SKELETON, cave, random));
			if (expect.get("stray in the snow") != Mount.GOAT_KID) wrong.add("a stray in the snow rides " + expect.get("stray in the snow"));
			if (expect.get("parched in the desert") != Mount.ARMADILLO) wrong.add("a parched in the desert rides " + expect.get("parched in the desert"));
			if (expect.get("wither skeleton in the nether") != Mount.BABY_HOGLIN) wrong.add("a wither skeleton in the nether rides " + expect.get("wither skeleton in the nether"));
			if (expect.get("skeleton in a cave") != Mount.CAVE_SPIDER) wrong.add("a skeleton in a cave rides " + expect.get("skeleton in a cave")
				+ " (sky light " + level.getBrightness(net.minecraft.world.level.LightLayer.SKY, cave) + ")");
			// The bogged picks frog or calf; asked often enough to see both, about two frogs to a calf.
			int frogs = 0, calves = 0;
			for (int i = 0; i < 600; i++) {
				Mount m = ask(level, EntityTypes.BOGGED, swamp, random);
				if (m == Mount.FROG) frogs++;
				else if (m == Mount.MOOSHROOM_CALF) calves++;
				else wrong.add("a bogged in the swamp rides " + m);
			}
			if (frogs < 330 || frogs > 470) wrong.add("of 600 bogged in a swamp " + frogs + " ride frogs and " + calves + " calves");
			System.out.println("[baby-skeletons test] bogged in a swamp: " + frogs + " frogs, " + calves + " calves of 600");
			// Out of their country, every one of them rides a chicken.
			for (EntityType<? extends AbstractSkeleton> type : Kit.FAMILY) {
				Mount m = ask(level, type, open, random);
				if (m != Mount.CHICKEN) wrong.add("a " + EntityType.getKey(type).getPath() + " on open plains rides " + m);
			}
			return wrong.isEmpty() ? "ok" : String.join("; ", wrong.stream().distinct().toList());
		});
		Kit.command(session, "fill %d %d %d %d %d %d minecraft:air".formatted(
			cave.getX() - 2, cave.getY(), cave.getZ() - 2, cave.getX() + 2, cave.getY() + 3, cave.getZ() + 2));
		check("ok".equals(found), "habitats: " + found);
	}

	private static Mount ask(ServerLevel level, EntityType<? extends AbstractSkeleton> type, BlockPos at, RandomSource random) {
		AbstractSkeleton skeleton = type.create(level, EntitySpawnReason.NATURAL);
		skeleton.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
		Mount mount = Jockeys.mountFor(skeleton, level, random);
		skeleton.discard();
		return mount;
	}

	/**
	 * Every pairing, given something to fight eighteen blocks off: after eight seconds the baby is
	 * still on, and its mount has carried it toward the fight. A frog does it slowest: on land it
	 * walks at a tenth of its speed and gets about by leaping, which its rider does not ask of it.
	 */
	private static void steering(Smoke.Session session) {
		Map<Mount, int[]> ids = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			Map<Mount, int[]> out = new EnumMap<>(Mount.class);
			int lane = 0;
			for (Mount mount : Mount.values()) {
				// Seven lanes across the player's own chunks, where mobs are ticked, clear of the player.
				BlockPos at = Kit.ground(session, level, -24 + lane * 7, -9);
				BlockPos targetAt = Kit.ground(session, level, -24 + lane * 7, 9);
				lane++;
				AbstractSkeleton baby = Kit.baby(level, RIDER.get(mount), at);
				Mob ride = Jockeys.bring(baby, level, level.getCurrentDifficultyAt(at), mount, level.getRandom());
				if (ride == null) throw new AssertionError("no " + mount + " was brought");
				ride.setPersistenceRequired();
				if (ride instanceof Hoglin hoglin) hoglin.setImmuneToZombification(true);
				IronGolem target = Kit.spawn(level, EntityTypes.IRON_GOLEM, targetAt, EntitySpawnReason.COMMAND);
				target.setNoAi(true);
				baby.setTarget(target);
				out.put(mount, new int[] {baby.getId(), ride.getId(), target.getId()});
			}
			return out;
		});
		Map<Mount, Double> start = Kit.ask(session, server -> distances(server.overworld(), ids));
		session.waitTicks(160);
		String moved = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			Map<Mount, Double> end = distances(level, ids);
			List<String> wrong = new ArrayList<>();
			for (Mount mount : Mount.values()) {
				int[] id = ids.get(mount);
				Entity baby = level.getEntity(id[0]);
				Entity ride = level.getEntity(id[1]);
				String name = EntityType.getKey(RIDER.get(mount)).getPath() + " on " + mount;
				if (baby == null || !baby.isAlive()) { wrong.add(name + ": the baby is gone"); continue; }
				if (ride == null || !ride.isAlive()) { wrong.add(name + ": the mount is gone"); continue; }
				if (baby.getVehicle() != ride) { wrong.add(name + ": the baby is off its mount"); continue; }
				if (mount.baby && !((AgeableMob) ride).isBaby()) wrong.add(name + ": the mount grew up");
				double closer = start.get(mount) - end.get(mount);
				System.out.printf("[baby-skeletons test] %s: %.1f blocks from its target, then %.1f%n", name, start.get(mount), end.get(mount));
				if (closer < 2) wrong.add(name + ": carried only %.1f blocks closer".formatted(closer));
			}
			return wrong.isEmpty() ? "ok" : String.join("; ", wrong);
		});
		check("ok".equals(moved), "steering: " + moved);
		session.onServer(server -> ids.values().forEach(id -> {
			for (int i : id) { Entity e = server.overworld().getEntity(i); if (e != null) e.discard(); }
		}));
	}

	private static Map<Mount, Double> distances(ServerLevel level, Map<Mount, int[]> ids) {
		Map<Mount, Double> out = new EnumMap<>(Mount.class);
		ids.forEach((mount, id) -> {
			Entity ride = level.getEntity(id[1]);
			Entity target = level.getEntity(id[2]);
			out.put(mount, ride == null || target == null ? Double.NaN : Math.sqrt(ride.distanceToSqr(target)));
		});
		return out;
	}

	/**
	 * A goat kid with a rider does not ram, so it neither throws nor hurts its rider. Shown against
	 * a kid with no rider, set up the same way, which does ram: otherwise the quiet one proves nothing.
	 */
	private static void goatsDoNotRamTheirRiders(Smoke.Session session) {
		int[] ids = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			int[] out = new int[5];
			for (int i = 0; i < 2; i++) {
				BlockPos at = Kit.ground(session, level, -20 + i * 14, -22);
				Goat kid = Kit.spawn(level, EntityTypes.GOAT, at, EntitySpawnReason.NATURAL);
				kid.setBaby(true);
				Pig pig = Kit.spawn(level, EntityTypes.PIG, at.south(5), EntitySpawnReason.COMMAND);
				pig.setNoAi(true);
				// Ready to ram now, rather than in the half-hour a goat waits between rams.
				kid.getBrain().eraseMemory(MemoryModuleType.RAM_COOLDOWN_TICKS);
				out[i * 2] = kid.getId();
				out[i * 2 + 1] = pig.getId();
				if (i == 0) {
					AbstractSkeleton stray = Kit.baby(level, EntityTypes.STRAY, at);
					stray.setNoAi(true);
					Jockeys.saddle(kid);
					stray.startRiding(kid, false, false);
					out[4] = stray.getId();
				}
			}
			return out;
		});
		session.waitTicks(200);
		String rammed = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			Pig ridersPig = (Pig) level.getEntity(ids[1]);
			Pig controlPig = (Pig) level.getEntity(ids[3]);
			AbstractSkeleton stray = (AbstractSkeleton) level.getEntity(ids[4]);
			Entity kid = level.getEntity(ids[0]);
			if (controlPig.getHealth() >= controlPig.getMaxHealth()) return "the riderless kid never rammed, so this proves nothing";
			if (stray == null || stray.getVehicle() != kid) return "the stray was thrown";
			if (stray.getHealth() < stray.getMaxHealth()) return "the stray was hurt: " + stray.getHealth();
			if (ridersPig.getHealth() < ridersPig.getMaxHealth()) return "the ridden kid rammed";
			return "ok";
		});
		check("ok".equals(rammed), "goat ram: " + rammed);
		session.onServer(server -> { for (int id : ids) { Entity e = server.overworld().getEntity(id); if (e != null) e.discard(); } });
	}

	/** An armadillo carrying a rider does not roll up when struck: the game's own rule, held. */
	private static void armadillosDoNotRollUp(Smoke.Session session) {
		int[] ids = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			BlockPos at = Kit.ground(session, level, 10, -22);
			AbstractSkeleton parched = Kit.baby(level, EntityTypes.PARCHED, at);
			parched.setNoAi(true);
			Mob ride = Jockeys.bring(parched, level, level.getCurrentDifficultyAt(at), Mount.ARMADILLO, level.getRandom());
			ride.setPersistenceRequired();
			ride.hurtServer(level, level.damageSources().playerAttack(session.player()), 1F);
			return new int[] {parched.getId(), ride.getId()};
		});
		session.waitTicks(40);
		String rolled = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			Armadillo armadillo = (Armadillo) level.getEntity(ids[1]);
			Entity parched = level.getEntity(ids[0]);
			if (armadillo.isScared() || armadillo.getState() != Armadillo.ArmadilloState.IDLE) return "rolled up: " + armadillo.getState();
			if (parched.getVehicle() != armadillo) return "the parched fell off";
			return "ok";
		});
		check("ok".equals(rolled), "armadillo: " + rolled);
		session.onServer(server -> { for (int id : ids) server.overworld().getEntity(id).discard(); });
	}

	/**
	 * A bogged's calf: shears, a bucket and a bowl do nothing to it, nor does wheat grow it, and
	 * lightning turns its colour, as it does any mooshroom's, with its rider still on.
	 */
	private static void calvesStayCalves(Smoke.Session session) {
		int[] ids = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			BlockPos at = Kit.ground(session, level, 18, -22);
			AbstractSkeleton bogged = Kit.baby(level, EntityTypes.BOGGED, at);
			bogged.setNoAi(true);
			// Through the lightning unharmed, so what is tested is the calf's part in it.
			bogged.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 400, 4));
			bogged.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 400, 0));
			Mob ride = Jockeys.bring(bogged, level, level.getCurrentDifficultyAt(at), Mount.MOOSHROOM_CALF, level.getRandom());
			ride.setPersistenceRequired();
			var player = session.player();
			for (var item : List.of(Items.SHEARS, Items.BUCKET, Items.BOWL, Items.WHEAT, Items.WHEAT, Items.WHEAT)) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
				player.interactOn(ride, InteractionHand.MAIN_HAND, ride.position());
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return new int[] {bogged.getId(), ride.getId(), ((MushroomCow) ride).getVariant().ordinal()};
		});
		BlockPos at = Kit.ask(session, server -> server.overworld().getEntity(ids[1]).blockPosition());
		Kit.command(session, "summon lightning_bolt %d %d %d".formatted(at.getX(), at.getY(), at.getZ()));
		session.waitTicks(10);
		String calf = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			Entity ride = level.getEntity(ids[1]);
			Entity bogged = level.getEntity(ids[0]);
			if (!(ride instanceof MushroomCow cow) || !cow.isAlive()) return "the calf is gone: " + ride;
			if (!cow.isBaby()) return "the calf grew up";
			if (cow.getVariant().ordinal() == ids[2]) return "lightning did not turn its colour";
			if (bogged == null || bogged.getVehicle() != cow) return "the bogged is off its calf";
			if (!level.getEntitiesOfClass(Cow.class, new AABB(cow.blockPosition()).inflate(4)).isEmpty()) return "a cow appeared";
			return "ok";
		});
		check("ok".equals(calf), "mooshroom calf: " + calf);
		session.onServer(server -> { for (int i = 0; i < 2; i++) server.overworld().getEntity(ids[i]).discard(); });
		Kit.command(session, "kill @e[type=item]");
	}

	/**
	 * A jockey goes when nobody is near, as a vanilla one does, but rider first: the mount is kept
	 * while it carries its baby, the baby goes as any monster does, and then the mount, alone,
	 * goes as a jockey's chicken does.
	 */
	private static void despawning(Smoke.Session session) {
		BlockPos far = Kit.ask(session, server -> session.player().blockPosition().offset(200, 0, 0));
		Kit.command(session, "forceload add %d %d".formatted(far.getX(), far.getZ()));
		session.waitTicks(5);
		String gone = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, far);
			// Not held by Kit.spawn's persistence: these are left to the game's own rules.
			AbstractSkeleton stray = EntityTypes.STRAY.create(level, EntitySpawnReason.NATURAL);
			stray.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
			Babies.become(stray);
			level.addFreshEntity(stray);
			Mob kid = Jockeys.bring(stray, level, level.getCurrentDifficultyAt(at), Mount.GOAT_KID, level.getRandom());
			Goat wild = EntityTypes.GOAT.create(level, EntitySpawnReason.NATURAL);
			wild.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
			level.addFreshEntity(wild);
			if (stray.isPersistenceRequired() || kid.isPersistenceRequired() || wild.isPersistenceRequired()) {
				return "something here is held by persistence, so this proves nothing";
			}
			kid.checkDespawn();
			if (kid.isRemoved()) return "the mount despawned from under its rider";
			stray.checkDespawn();
			if (!stray.isRemoved()) return "the rider far from anyone did not despawn";
			kid.checkDespawn();
			if (!kid.isRemoved()) return "the riderless jockey's mount stayed";
			wild.checkDespawn();
			if (wild.isRemoved()) return "an ordinary goat despawned";
			wild.discard();
			return "ok";
		});
		Kit.command(session, "forceload remove %d %d".formatted(far.getX(), far.getZ()));
		check("ok".equals(gone), "despawning: " + gone);
	}

	/** A jockey put together by /summon is steered just the same: its brain rests under the baby. */
	private static void summoned(Smoke.Session session) {
		Kit.command(session, "summon frog ~-20 ~ ~-20 {PersistenceRequired:1b,Passengers:[{id:\"minecraft:bogged\",Tags:[\"baby_skeleton\"]}]}");
		session.waitTicks(40);
		String steered = Kit.ask(session, server -> {
			ServerLevel level = server.overworld();
			var frogs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.frog.Frog.class,
				session.player().getBoundingBox().inflate(40), Entity::isVehicle);
			if (frogs.size() != 1) return "found " + frogs.size() + " ridden frogs";
			Entity rider = frogs.get(0).getFirstPassenger();
			if (!Babies.is(rider)) return "the bogged on it is not a baby";
			if (!Jockeys.steered(frogs.get(0))) return "the frog is not steered by its baby";
			frogs.get(0).ejectPassengers();
			rider.discard();
			frogs.get(0).discard();
			return "ok";
		});
		check("ok".equals(steered), "summoned jockey: " + steered);
	}
}
