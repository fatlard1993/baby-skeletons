package justfatlard.baby_skeletons.gametest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import justfatlard.pandorical.gametest.Smoke;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.levelgen.Heightmap;

/** What the tests share: the family, a night to put them in, and a way to ask the server things. */
final class Kit {
	private Kit() {}

	static final String MOD_ID = "baby-skeletons-justfatlard";

	/** Every skeleton-family hostile in 26.3, checked against the game's own #skeletons tag. */
	static final List<EntityType<? extends AbstractSkeleton>> FAMILY = List.of(
		EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.PARCHED,
		EntityTypes.WITHER_SKELETON);

	/** Night, on a difficulty where monsters are allowed to exist, so nothing burns or vanishes mid-test. */
	static void night(Smoke.Session session) {
		command(session, "difficulty normal");
		command(session, "time set midnight");
		session.waitTicks(2);
	}

	/**
	 * A command from the server's own console, standing where the player stands. Not the player's:
	 * the test world's player has no permission to run commands, and a refused command only says so
	 * in chat, where a test never looks.
	 */
	static void command(Smoke.Session session, String command) {
		session.onServer(server -> server.getCommands().performPrefixedCommand(
			server.createCommandSourceStack().withPosition(session.player().position()), command));
	}

	/** Ask the server something and bring the answer back to the test thread. */
	static <T> T ask(Smoke.Session session, Function<MinecraftServer, T> question) {
		AtomicReference<T> answer = new AtomicReference<>();
		session.onServer(server -> answer.set(question.apply(server)));
		return answer.get();
	}

	/** A spot on the ground this many blocks east and south of the player. */
	static BlockPos ground(Smoke.Session session, ServerLevel level, int east, int south) {
		BlockPos at = session.player().blockPosition().offset(east, 0, south);
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
	}

	/** A mob spawned the way the night spawns one, put in the world, and told to stay. */
	static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, BlockPos at, EntitySpawnReason reason) {
		T mob = type.create(level, reason);
		if (mob == null) throw new AssertionError("could not create " + type);
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), reason, null);
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		return mob;
	}

	/** A grown one, for certain: re-rolled on the one-in-twenty chance it came out a baby. */
	static <T extends AbstractSkeleton> T adult(ServerLevel level, EntityType<T> type, BlockPos at) {
		while (true) {
			T made = spawn(level, type, at, EntitySpawnReason.NATURAL);
			if (!justfatlard.baby_skeletons.Babies.is(made)) return made;
			if (made.getVehicle() != null) made.getVehicle().discard();
			made.discard();
		}
	}

	/**
	 * A baby, for certain, on foot: spawned with its kind's gear, then made a baby. Spawned as a
	 * skeleton trap's riders are, which the night's baby and jockey rolls never touch, so it is not
	 * already riding something; a test that wants it riding says what.
	 */
	static <T extends AbstractSkeleton> T baby(ServerLevel level, EntityType<T> type, BlockPos at) {
		T made = spawn(level, type, at, EntitySpawnReason.TRIGGERED);
		justfatlard.baby_skeletons.Babies.become(made);
		return made;
	}

	/** Said in the log, so a run that passed can be told from a run that tested nothing. */
	static void passed(String test) {
		System.out.println("[baby-skeletons test] PASSED " + test);
	}

	static void check(boolean holds, String otherwise) {
		if (!holds) throw new AssertionError(otherwise);
	}

	static boolean near(double actual, double expected) {
		return Math.abs(actual - expected) < 1.0E-4;
	}
}
