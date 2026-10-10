package justfatlard.baby_skeletons.gametest;

import java.util.ArrayList;
import java.util.List;

import justfatlard.baby_skeletons.Babies;
import justfatlard.baby_skeletons.Jockeys;
import justfatlard.baby_skeletons.Jockeys.Mount;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Models;
import justfatlard.pandorical.gametest.Pictures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.phys.Vec3;

import static justfatlard.baby_skeletons.gametest.Kit.check;

/**
 * Every jockey, side by side, for the eye: the five kinds on chickens at the back, and each kind's
 * own mount in front. Taken as {@code baby-jockeys}; with {@code -Ppictures} it is kept in this
 * repo's {@code screenshots/} too.
 */
public final class JockeysPictured implements FabricClientGameTest {

	/** Blocks between one jockey and the next. */
	private static final double GAP = 1.5;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isContentReady(connection.getServerPlayer()));
			server.runCommand("difficulty normal");
			Pictures.stage(context, server, 18000);
			server.runCommand("effect give @a minecraft:night_vision infinite 0 true");

			BlockPos base = server.computeOnServer(s -> Pictures.ground(s.overworld(),
				connection.getServerPlayer().getBlockX(), connection.getServerPlayer().getBlockZ()));

			List<Integer> riders = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				List<Integer> out = new ArrayList<>();
				// At the back, every kind on vanilla's chicken.
				for (int i = 0; i < Kit.FAMILY.size(); i++) {
					out.add(pose(level, Kit.FAMILY.get(i), Mount.CHICKEN, base, (i - 2) * GAP, -2.2));
				}
				// In front, each kind's own.
				List<Mount> own = List.of(Mount.CAVE_SPIDER, Mount.GOAT_KID, Mount.FROG, Mount.MOOSHROOM_CALF,
					Mount.ARMADILLO, Mount.BABY_HOGLIN);
				for (int i = 0; i < own.size(); i++) {
					Mount mount = own.get(i);
					out.add(pose(level, BabiesRideJockeys.RIDER.get(mount), mount, base, (i - 2.5) * GAP * 1.15, 0));
				}
				return out;
			});

			String mounted = server.computeOnServer(s -> {
				List<String> wrong = new ArrayList<>();
				for (int id : riders) {
					var rider = s.overworld().getEntity(id);
					if (rider == null || rider.getVehicle() == null) wrong.add(id + " is not riding");
				}
				return String.join("; ", wrong);
			});
			check(mounted.isEmpty(), "posing: " + mounted);

			context.waitFor(client -> riders.stream().allMatch(id -> client.level.getEntity(id) != null
				&& client.level.getEntity(id).getVehicle() != null && Models.assigned(id) != null));
			context.waitTicks(5);

			Vec3 centre = Vec3.atBottomCenterOf(base).add(0, 0, -1.1);
			Pictures.look(server, centre.add(0, 0.9, 7.2), centre.add(0, 0.55, 0));
			var shot = Pictures.shoot(context, connection, "baby-jockeys");
			System.out.println("[baby-skeletons test] picture " + shot.toAbsolutePath());
		}
		Kit.passed("JockeysPictured");
	}

	/** A baby of this kind on this mount, standing still, facing the camera. */
	private static int pose(ServerLevel level, EntityType<? extends AbstractSkeleton> kind, Mount mount, BlockPos base,
			double dx, double dz) {
		AbstractSkeleton baby = kind.create(level, EntitySpawnReason.TRIGGERED);
		BlockPos at = base.offset((int) Math.floor(dx), 0, (int) Math.floor(dz));
		baby.snapTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, 0F, 0F);
		// As a skeleton trap's riders spawn: its gear, and none of the night's rolls.
		baby.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.TRIGGERED, null);
		Babies.become(baby);
		baby.setNoAi(true);
		baby.setPersistenceRequired();
		Mob ride = Jockeys.bring(baby, level, level.getCurrentDifficultyAt(at), mount, level.getRandom());
		if (ride == null) throw new AssertionError("no " + mount);
		if (mount.baby && !ride.isBaby()) throw new AssertionError(mount + " is not a baby");
		ride.snapTo(baby.getX(), baby.getY(), baby.getZ(), 0F, 0F);
		ride.setYHeadRot(0F);
		ride.setYBodyRot(0F);
		ride.setNoAi(true);
		ride.setPersistenceRequired();
		if (ride instanceof Hoglin hoglin) hoglin.setImmuneToZombification(true);
		baby.setYHeadRot(0F);
		baby.setYBodyRot(0F);
		level.addFreshEntity(baby);
		return baby.getId();
	}
}
