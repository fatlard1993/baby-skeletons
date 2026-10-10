package justfatlard.baby_skeletons.gametest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import justfatlard.baby_skeletons.Babies;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Models;
import justfatlard.pandorical.gametest.Pictures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static justfatlard.baby_skeletons.gametest.Kit.check;

/**
 * On a client with Pandorical, a baby of every kind is drawn as a baby: its baby model, the size of
 * a real baby zombie standing beside it, and not a quarter of a grown skeleton. Measured off the
 * models the game would draw, posed as it would pose them, against the game's own baby zombie, and
 * photographed side by side.
 *
 * <p>Two numbers decide it. The height is the one that catches shrinking twice: a baby model drawn
 * at the half scale the server gives it for its hitbox stands half a baby zombie's height. The head
 * is the one that tells a baby from a grown skeleton at half size - which is what a vanilla client
 * draws, and stands nearly a baby zombie's height, but with a head a third smaller.
 */
public final class BabiesLookLikeBabies implements FabricClientGameTest {

	/** Within this fraction of the baby zombie: a pixel on a fifteen-pixel baby. */
	private static final double CLOSE = 0.04;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isContentReady(connection.getServerPlayer()));
			server.runCommand("difficulty normal");
			Pictures.stage(context, server, 18000);
			// Midnight, so nobody burns, and night vision on the camera, so it is all lit.
			server.runCommand("effect give @a minecraft:night_vision infinite 0 true");

			BlockPos base = server.computeOnServer(s -> Pictures.ground(s.overworld(),
				connection.getServerPlayer().getBlockX(), connection.getServerPlayer().getBlockZ()));

			// In the picture: a baby skeleton, stray and wither skeleton, and a real baby zombie.
			Map<String, Integer> ids = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				Map<String, Integer> out = new LinkedHashMap<>();
				Zombie zombie = place(level, EntityTypes.ZOMBIE, base, -2.4, 0);
				zombie.setBaby(true);
				out.put("baby zombie", zombie.getId());
				out.put("skeleton", baby(level, EntityTypes.SKELETON, base, -0.8, 0, Items.BOW));
				out.put("stray", baby(level, EntityTypes.STRAY, base, 0.8, 0, Items.BOW));
				out.put("wither_skeleton", baby(level, EntityTypes.WITHER_SKELETON, base, 2.4, 0, Items.WOODEN_SWORD));
				// Out of the picture: the other two kinds, and a grown skeleton at half scale, which is
				// what a client without Pandorical draws for any of them.
				out.put("bogged", baby(level, EntityTypes.BOGGED, base, -1, -6, Items.BOW));
				out.put("parched", baby(level, EntityTypes.PARCHED, base, 1, -6, Items.BOW));
				AbstractSkeleton halfGrown = place(level, EntityTypes.SKELETON, base, 3, -6);
				halfGrown.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).addOrReplacePermanentModifier(
					new net.minecraft.world.entity.ai.attributes.AttributeModifier(
						net.minecraft.resources.Identifier.fromNamespaceAndPath("baby-skeletons-gametest", "half"),
						Babies.SCALE - 1F, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
				out.put("half-scale grown skeleton", halfGrown.getId());
				return out;
			});
			// A baby made by /summon is given its model only when it loads, which is the path a baby
			// read back from the save takes.
			server.runCommand("summon bogged %d %d %d {Tags:[\"baby_skeleton\"],NoAI:1b,PersistenceRequired:1b}"
				.formatted(base.getX() - 3, base.getY(), base.getZ() - 6));
			ids.put("summoned bogged", server.computeOnServer(s -> s.overworld().getEntitiesOfClass(AbstractSkeleton.class,
				new net.minecraft.world.phys.AABB(base.offset(-3, 0, -6)).inflate(1), Babies::is).get(0).getId()));

			String onServer = server.computeOnServer(s -> {
				List<String> wrong = new ArrayList<>();
				for (Map.Entry<String, Integer> entry : ids.entrySet()) {
					LivingEntity entity = (LivingEntity) s.overworld().getEntity(entry.getValue());
					boolean baby = entity instanceof AbstractSkeleton && Babies.is(entity);
					var model = PandoricalApi.entityModels().get(entity);
					if (baby && model == null) wrong.add(entry.getKey() + " has no baby model");
					if (!baby && model != null) wrong.add(entry.getKey() + " has a model: " + model);
				}
				return String.join("; ", wrong);
			});
			check(onServer.isEmpty(), "on the server: " + onServer);

			context.waitFor(client -> ids.entrySet().stream().allMatch(e -> client.level.getEntity(e.getValue()) != null
				&& (e.getKey().contains("zombie") || e.getKey().contains("grown")
					|| Models.assigned(e.getValue()) != null)));
			context.waitTicks(5);

			String measured = context.computeOnClient(client -> {
				List<String> wrong = new ArrayList<>();
				Measure zombie = measureBabyZombie(client, ids.get("baby zombie"));
				System.out.println("[baby-skeletons test] baby zombie: " + zombie);
				for (Map.Entry<String, Integer> entry : ids.entrySet()) {
					String name = entry.getKey();
					if (name.contains("zombie") || name.contains("grown")) continue;
					Measure baby = measure(client, entry.getValue());
					System.out.println("[baby-skeletons test] baby " + name + ": " + baby);
					double size = name.equals("wither_skeleton") ? 1.2 : 1.0;
					if (baby.scale != zombie.scale) wrong.add(name + " drawn at scale " + baby.scale + ", a baby zombie at " + zombie.scale);
					if (!near(baby.height, zombie.height * size)) wrong.add(name + " stands " + baby.height + ", a baby zombie " + zombie.height);
					if (!near(baby.head, zombie.head * size)) wrong.add(name + "'s head is " + baby.head + ", a baby zombie's " + zombie.head);
				}
				Measure halfGrown = measure(client, ids.get("half-scale grown skeleton"));
				System.out.println("[baby-skeletons test] half-scale grown skeleton (vanilla clients): " + halfGrown);
				if (near(halfGrown.head, zombie.head)) wrong.add("a half-scale grown skeleton's head measures as a baby's, so the check proves nothing");
				return String.join("; ", wrong);
			});
			check(measured.isEmpty(), measured);

			Pictures.look(server, Vec3.atBottomCenterOf(base).add(0, 0.1, 3.6), Vec3.atBottomCenterOf(base).add(0, 0.55, 0));
			Path shot = Pictures.shoot(context, connection, "baby-skeleton-model");
			keep(shot, "baby-skeleton-model.png");

			// Armour, for the eye: the baby skeleton and the baby zombie in iron.
			server.runOnServer(s -> {
				for (String name : List.of("baby zombie", "skeleton")) {
					LivingEntity mob = (LivingEntity) s.overworld().getEntity(ids.get(name));
					mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
					mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
					mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
					mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
				}
			});
			Pictures.look(server, Vec3.atBottomCenterOf(base).add(-1.6, 0.1, 2.6), Vec3.atBottomCenterOf(base).add(-1.6, 0.55, 0));
			keep(Pictures.shoot(context, connection, "baby-skeleton-armor"), "baby-skeleton-armor.png");
		}
		Kit.passed("BabiesLookLikeBabies");
	}

	private record Measure(String model, float scale, double height, double head) {
		@Override
		public String toString() {
			return "%s at scale %.3f, %.4f blocks tall, head %.4f".formatted(model, scale, height, head);
		}
	}

	/** As the game draws it: its renderer's model, posed for its state, with whatever Pandorical lays over the pose. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Measure measure(Minecraft client, int id) {
		Entity entity = client.level.getEntity(id);
		LivingEntityRenderState state = state(client, entity);
		EntityModel model = ((LivingEntityRenderer) client.getEntityRenderDispatcher().getRenderer(entity)).getModel();
		Models.pose(model, state);
		return measure(model, state);
	}

	/** The game's own baby zombie model, from its own baby layer, posed for the real baby zombie's state. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Measure measureBabyZombie(Minecraft client, int id) {
		LivingEntityRenderState state = state(client, client.level.getEntity(id));
		if (!state.isBaby) throw new AssertionError("the baby zombie is not a baby on the client");
		EntityModel model = new BabyZombieModel(BabyZombieModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());
		model.setupAnim(state);
		return measure(model, state);
	}

	/**
	 * Placed as {@code LivingEntityRenderer.submit} places a model, in blocks: sole to crown, and the
	 * head's own height, from the first block of each main part. Hat layers are left out: a
	 * skeleton's is see-through, and a zombie's is not a separate part.
	 */
	private static Measure measure(EntityModel<?> model, LivingEntityRenderState state) {
		PoseStack pose = new PoseStack();
		pose.scale(state.scale, state.scale, state.scale);
		pose.scale(-1F, -1F, 1F);
		pose.translate(0F, -1.501F, 0F);
		double[] all = {Double.MAX_VALUE, -Double.MAX_VALUE};
		double[] head = {Double.MAX_VALUE, -Double.MAX_VALUE};
		List<String> main = List.of("/head", "/body", "/right_arm", "/left_arm", "/right_leg", "/left_leg");
		model.root().visit(pose, (p, path, index, cube) -> {
			if (index != 0 || !main.contains(path)) return;
			for (var polygon : cube.polygons) {
				for (var vertex : polygon.vertices()) {
					float y = p.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new org.joml.Vector3f()).y();
					all[0] = Math.min(all[0], y);
					all[1] = Math.max(all[1], y);
					if (path.equals("/head")) {
						head[0] = Math.min(head[0], y);
						head[1] = Math.max(head[1], y);
					}
				}
			}
		});
		return new Measure(model.getClass().getSimpleName(), state.scale, all[1] - all[0], head[1] - head[0]);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static LivingEntityRenderState state(Minecraft client, Entity entity) {
		EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
		return (LivingEntityRenderState) renderer.createRenderState(entity, 1F);
	}

	/** A mob standing still, facing south toward the camera. */
	private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, BlockPos base, double dx, double dz) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, 0F, 0F);
		mob.setYHeadRot(0F);
		mob.setYBodyRot(0F);
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		return mob;
	}

	private static int baby(ServerLevel level, EntityType<? extends AbstractSkeleton> type, BlockPos base, double dx, double dz, Item held) {
		AbstractSkeleton skeleton = place(level, type, base, dx, dz);
		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(held));
		Babies.become(skeleton);
		return skeleton.getId();
	}

	private static void keep(Path shot, String name) {
		System.out.println("[baby-skeletons test] picture " + shot.toAbsolutePath());
	}

	private static boolean near(double actual, double expected) {
		return Math.abs(actual - expected) <= expected * CLOSE;
	}
}
