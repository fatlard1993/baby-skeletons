package justfatlard.baby_skeletons;

import java.util.List;

import justfatlard.baby_skeletons.mixin.AgeableMobAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Baby skeletons riding things, the way baby zombies ride chickens.
 *
 * <p>Vanilla's chicken jockey, from {@code Zombie.finalizeSpawn} in 26.3, copied step for step. As a
 * baby finishes spawning, one roll in twenty looks for a chicken within five blocks across and three
 * up or down that nobody is riding, and climbs on the first one found; if there is none, that is
 * the end of it. Failing that roll, another one in twenty brings a new chicken with it, spawned for
 * the purpose and finished as a jockey's mount. Either way the chicken is marked a jockey's
 * ({@code setChickenJockey}), which is what stops it laying eggs, makes it worth ten experience, and
 * lets it despawn as a monster does. Nothing else about it changes, and nothing changes back when its
 * rider dies.
 *
 * <p>Every baby rolls that, and where a kind has an animal of its own country to ride it rides that
 * instead, by the same rolls: see {@link Mount}. A chicken is what any baby rides anywhere else.
 *
 * <p>Steering is the game's, not this mod's. A mob riding a mob controls it: the rider's goals
 * path and move with the mount's navigation and move control ({@code Mob.getNavigation} hands over
 * the vehicle's), and the mount's own goals that move, look or jump are switched off for as long as
 * it is ridden ({@code Mob.updateControlFlags}). So the baby fights and the mount carries it where
 * the fight goes. That switch only reaches goals, and four of the mounts here think with a brain
 * instead, which it does not reach: a goat, a frog, an armadillo and a hoglin would go on wandering,
 * panicking and ramming under their rider. Their brain is rested while a baby rides them
 * ({@link #steered}), which is the chicken's treatment given to the mounts it would not otherwise
 * reach.
 */
public final class Jockeys {
	private Jockeys() {}

	/**
	 * What every jockey's mount wears, as vanilla's chicken wears {@code IsChickenJockey}: it
	 * despawns as a monster does, rather than staying for good as an animal would, once its rider
	 * is gone. A scoreboard tag, so it is saved with the mount and survives a conversion.
	 */
	public static final String MOUNT_TAG = "baby_skeleton_mount";

	/** One in twenty, twice over: {@code Zombie.finalizeSpawn}'s two rolls. */
	public static final float SEARCH_CHANCE = 0.05F;
	public static final float SPAWN_CHANCE = 0.05F;

	/** How far a baby looks for a mount to climb on: {@code inflate(5, 3, 5)}, as a baby zombie does. */
	private static final double REACH = 5, CLIMB = 3;

	/** Of the bogged that ride, how many ride a frog; the rest ride a mooshroom calf. */
	public static final float FROG_SHARE = 2F / 3F;

	/** What a baby rides, and the country that makes it the one. */
	public enum Mount {
		/** Anything, anywhere: vanilla's own. */
		CHICKEN(EntityTypes.CHICKEN, false),
		/** A skeleton where no sky light reaches: caves, mineshafts and dungeons. */
		CAVE_SPIDER(EntityTypes.CAVE_SPIDER, false),
		/** A stray where it is cold enough to snow. */
		GOAT_KID(EntityTypes.GOAT, true),
		/** A bogged where frogs live. */
		FROG(EntityTypes.FROG, false),
		/** A bogged where frogs live, the third of the time it does not take a frog. */
		MOOSHROOM_CALF(EntityTypes.MOOSHROOM, true),
		/** A parched on dry ground: where no rain falls under an open sky. */
		ARMADILLO(EntityTypes.ARMADILLO, false),
		/** A wither skeleton in the Nether. */
		BABY_HOGLIN(EntityTypes.HOGLIN, true);

		public final EntityType<? extends Mob> type;
		/** A mount that has to be young, and is kept that way while it is one. */
		public final boolean baby;

		Mount(EntityType<? extends Mob> type, boolean baby) {
			this.type = type;
			this.baby = baby;
		}

		/** Whether this one, already in the world, would do. */
		boolean suits(Entity entity) {
			if (entity.getType() != type || !(entity instanceof Mob mob) || !mob.isAlive()) return false;
			return !baby || mob.isBaby();
		}
	}

	/**
	 * Which mount a baby takes where it stands. Each kind has the animal that fits where it is
	 * found, and a chicken anywhere that animal would not be.
	 */
	public static Mount mountFor(AbstractSkeleton baby, ServerLevelAccessor level, RandomSource random) {
		BlockPos at = baby.blockPosition();
		Holder<Biome> biome = level.getBiome(at);
		boolean skylit = level.dimensionType().hasSkyLight();
		if (baby instanceof WitherSkeleton) {
			if (biome.is(BiomeTags.IS_NETHER)) return Mount.BABY_HOGLIN;
		} else if (baby instanceof Stray) {
			if (biome.value().coldEnoughToSnow(at, level.getSeaLevel())) return Mount.GOAT_KID;
		} else if (baby instanceof Bogged) {
			if (frogsLiveAt(level, at)) return random.nextFloat() < FROG_SHARE ? Mount.FROG : Mount.MOOSHROOM_CALF;
		} else if (baby instanceof Parched) {
			if (skylit && !biome.value().hasPrecipitation()) return Mount.ARMADILLO;
		} else if (baby instanceof Skeleton) {
			if (skylit && level.getBrightness(LightLayer.SKY, at) == 0) return Mount.CAVE_SPIDER;
		}
		return Mount.CHICKEN;
	}

	/**
	 * Where frogs come into the world on their own, asked of the world rather than a list of
	 * biomes: a swamp and a mangrove swamp in 26.3, and wherever a data pack puts them after.
	 */
	private static boolean frogsLiveAt(ServerLevelAccessor level, BlockPos at) {
		return level.getLevel().environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, at)
			.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
			.anyMatch(spawn -> spawn.value().type() == EntityTypes.FROG);
	}

	/**
	 * The jockey roll, as a baby finishes spawning: vanilla's two rolls, with the mount the baby's
	 * country gives it.
	 */
	public static void maybeRide(AbstractSkeleton baby, ServerLevelAccessor level, DifficultyInstance difficulty,
			RandomSource random) {
		if (baby.isPassenger()) return;
		Mount mount = mountFor(baby, level, random);
		if (random.nextFloat() < SEARCH_CHANCE) {
			climbNearby(baby, level, mount);
		} else if (random.nextFloat() < SPAWN_CHANCE) {
			bring(baby, level, difficulty, mount, random);
		}
	}

	/**
	 * Climb on the first mount of this kind nearby that nobody is riding. Nothing happens if there
	 * is none: the baby does not then bring one, as a baby zombie does not.
	 *
	 * @return the mount climbed on, or null
	 */
	public static @Nullable Mob climbNearby(AbstractSkeleton baby, ServerLevelAccessor level, Mount mount) {
		AABB around = baby.getBoundingBox().inflate(REACH, CLIMB, REACH);
		List<Mob> found = level.getEntitiesOfClass(Mob.class, around,
			entity -> EntitySelector.ENTITY_NOT_BEING_RIDDEN.test(entity) && mount.suits(entity) && entity != baby);
		if (found.isEmpty()) return null;
		Mob ride = found.get(0);
		if (!baby.startRiding(ride, false, false)) return null;
		saddle(ride);
		quiet(ride);
		return ride;
	}

	/**
	 * A new mount, brought with the baby, finished spawning as a jockey's and put in the world
	 * with the baby on it. The baby itself is put in the world by whatever is spawning it.
	 *
	 * @return the mount, or null if the game would not make one
	 */
	public static @Nullable Mob bring(AbstractSkeleton baby, ServerLevelAccessor level, DifficultyInstance difficulty,
			Mount mount, RandomSource random) {
		Mob ride = mount.type.create(baby.level(), EntitySpawnReason.JOCKEY);
		if (ride == null) return null;
		ride.snapTo(baby.getX(), baby.getY(), baby.getZ(), baby.getYRot(), 0F);
		ride.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
		if (mount.baby) ride.setBaby(true);
		if (ride instanceof MushroomCow calf) {
			// A bogged wears a red mushroom and a brown one; its calf is either, as often as each.
			calf.setComponent(DataComponents.MOOSHROOM_VARIANT,
				random.nextBoolean() ? MushroomCow.Variant.RED : MushroomCow.Variant.BROWN);
		}
		if (!baby.startRiding(ride, false, false)) {
			ride.discard();
			return null;
		}
		saddle(ride);
		level.addFreshEntity(ride);
		return ride;
	}

	/**
	 * Mark a mount as a jockey's: the chicken with the game's own flag, every one with the tag, and
	 * a young one held young.
	 *
	 * <p>Held young with the game's own age lock, the one a golden dandelion sets, so a calf, a kid
	 * or a hoglin piglet does not grow out from under its rider, and feeding it does not hurry it
	 * either. A player can still lift the lock with a dandelion, as with any locked baby.
	 */
	public static void saddle(Mob mount) {
		mount.addTag(MOUNT_TAG);
		if (mount instanceof Chicken chicken) chicken.setChickenJockey(true);
		if (mount instanceof AgeableMob young && young.isBaby()) ((AgeableMobAccess) young).babySkeletons$lockAge(true);
	}

	/** Whether this is a jockey's mount, by the tag. */
	public static boolean isMount(@Nullable Entity entity) {
		return entity != null && entity.entityTags().contains(MOUNT_TAG);
	}

	/**
	 * Whether a baby skeleton is steering this mob: the case where its brain is rested, so the
	 * baby's goals alone drive it.
	 *
	 * <p>By the rider, not the tag, so a jockey put together by {@code /summon} with passengers is
	 * steered the same as one the night made.
	 */
	public static boolean steered(Entity mount) {
		return mount.isVehicle() && mount instanceof Mob mob && Babies.is(mob.getControllingPassenger());
	}

	/**
	 * Whether a jockey's mount is carrying its rider. Such a mount does not despawn: it is the rider
	 * that despawns, as any monster does, and the mount, a jockey's once it is alone, follows. So the
	 * pair go when nobody is near, as a vanilla jockey does, and a baby is never left standing where
	 * its mount vanished from under it.
	 */
	public static boolean carrying(Mob mob) {
		return mob.isVehicle() && isMount(mob) && Babies.is(mob.getFirstPassenger());
	}

	/** Whether this is a baby riding a jockey's mount: a monster, despawning as one, though it rides. */
	public static boolean riding(Mob mob) {
		return Babies.is(mob) && isMount(mob.getVehicle());
	}

	/**
	 * Called as a baby climbs on a mob that was already about its own business, so nothing it had
	 * in mind - a walk somewhere, a ram lined up, a panic - goes on under the rider. A mob that
	 * thinks with goals has nothing here to stop; the game switches those off itself.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void quiet(Mob mount) {
		if (mount.level() instanceof ServerLevel level) {
			((Brain) mount.getBrain()).stopAll(level, mount);
			mount.getNavigation().stop();
		}
	}
}
