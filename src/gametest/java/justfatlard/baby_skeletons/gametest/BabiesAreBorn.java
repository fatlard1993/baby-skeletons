package justfatlard.baby_skeletons.gametest;

import java.util.ArrayList;
import java.util.List;

import justfatlard.baby_skeletons.Babies;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import static justfatlard.baby_skeletons.gametest.Kit.check;

/**
 * Babies come into the world: about one skeleton in twenty, of every kind, and never by conversion;
 * for certain from a spawn egg used on a grown one; and from {@code /summon} with the tag on.
 */
public final class BabiesAreBorn implements FabricClientGameTest {

	/** Per kind of skeleton. Five kinds, so a thousand spawns: about fifty babies expected. */
	private static final int SPAWNS_EACH = 200;

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, Kit.MOD_ID, session -> {
			Kit.night(session);

			// The family the tests cover is the family the game has: everything in the game's own
			// #skeletons tag that is a skeleton and not a horse. A new kind of skeleton in a later
			// version fails this until somebody looks at it.
			String family = Kit.ask(session, server -> {
				List<String> found = new ArrayList<>();
				ServerLevel level = server.overworld();
				for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
					if (!type.builtInRegistryHolder().is(EntityTypeTags.SKELETONS)) continue;
					var made = type.create(level, EntitySpawnReason.COMMAND);
					if (made instanceof AbstractSkeleton) found.add(EntityType.getKey(type).toString());
					if (made != null) made.discard();
				}
				return String.join(",", found);
			});
			for (EntityType<? extends AbstractSkeleton> type : Kit.FAMILY) {
				check(family.contains(EntityType.getKey(type).toString()), "missing from the registry: " + type);
			}
			check(family.split(",").length == Kit.FAMILY.size(),
				"the registry's skeleton family is not the one tested: " + family);

			// Natural spawns: about one in twenty, every kind among them.
			int[] born = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				BlockPos at = Kit.ground(session, level, 0, 0);
				int[] counts = new int[Kit.FAMILY.size() + 1];
				for (int kind = 0; kind < Kit.FAMILY.size(); kind++) {
					for (int i = 0; i < SPAWNS_EACH; i++) {
						AbstractSkeleton skeleton = Kit.FAMILY.get(kind).create(level, EntitySpawnReason.NATURAL);
						skeleton.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
						skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
						if (Babies.is(skeleton)) counts[kind]++;
						skeleton.discard();
					}
				}
				// A conversion never makes a baby: a grown skeleton freezing is a grown stray.
				for (int i = 0; i < SPAWNS_EACH; i++) {
					AbstractSkeleton stray = EntityTypes.STRAY.create(level, EntitySpawnReason.CONVERSION);
					stray.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.CONVERSION, null);
					if (Babies.is(stray)) counts[Kit.FAMILY.size()]++;
					stray.discard();
				}
				return counts;
			});
			int total = 0;
			StringBuilder each = new StringBuilder();
			for (int kind = 0; kind < Kit.FAMILY.size(); kind++) {
				total += born[kind];
				each.append(EntityType.getKey(Kit.FAMILY.get(kind)).getPath()).append('=').append(born[kind]).append(' ');
			}
			// Expected 50 of 1000 with a spread of about 7: this band fails by chance about once in
			// several thousand runs, and fails every time if the roll is missing or wildly wrong.
			check(total >= 20 && total <= 90, "babies born out of " + SPAWNS_EACH * Kit.FAMILY.size()
				+ " natural spawns: " + total + " (" + each.toString().trim() + ")");
			check(born[Kit.FAMILY.size()] == 0, "a conversion came out a baby " + born[Kit.FAMILY.size()] + " times");
			System.out.println("[baby-skeletons test] natural spawns: " + total + " babies of "
				+ SPAWNS_EACH * Kit.FAMILY.size() + " (" + each.toString().trim() + ")");

			// A spawn egg used on a grown skeleton hatches a baby of its kind, bow and all.
			String hatched = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				BlockPos at = Kit.ground(session, level, 4, 0);
				Skeleton adult = Kit.adult(level, EntityTypes.SKELETON, at);
				adult.setNoAi(true);
				var player = session.player();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SKELETON_SPAWN_EGG));
				InteractionResult result = UseEntityCallback.EVENT.invoker()
					.interact(player, level, InteractionHand.MAIN_HAND, adult, null);
				List<Skeleton> babies = level.getEntitiesOfClass(Skeleton.class, new AABB(at).inflate(2),
					skeleton -> skeleton != adult && Babies.is(skeleton));
				if (!result.consumesAction()) return "the egg was not taken: " + result;
				if (babies.size() != 1) return "hatched " + babies.size() + " babies";
				if (!babies.get(0).isHolding(Items.BOW)) return "the hatched baby has no bow";
				if (babies.get(0).getAttributeValue(Attributes.SCALE) != Babies.SCALE) return "the hatched baby is full size";
				if (PandoricalApi.entityModels().get(babies.get(0)) == null) return "the hatched baby is not given its baby model";
				return "ok";
			});
			check("ok".equals(hatched), "spawn egg on a grown skeleton: " + hatched);

			// /summon with the tag on makes one for certain, sized on the way in.
			Kit.command(session, "summon stray ~6 ~ ~ {Tags:[\"baby_skeleton\"],PersistenceRequired:1b}");
			session.waitTicks(2);
			String summoned = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				List<Stray> strays = level.getEntitiesOfClass(Stray.class,
					session.player().getBoundingBox().inflate(10), Babies::is);
				if (strays.size() != 1) return "found " + strays.size() + " tagged strays";
				Stray stray = strays.get(0);
				if (stray.getAttributeValue(Attributes.SCALE) != Babies.SCALE) {
					return "scale " + stray.getAttributeValue(Attributes.SCALE);
				}
				if (stray.getHealth() > stray.getMaxHealth()) return "health above its maximum";
				if (PandoricalApi.entityModels().get(stray) == null) return "not given a baby model";
				return "ok";
			});
			check("ok".equals(summoned), "/summon with the tag: " + summoned);
			Kit.passed("BabiesAreBorn");
		});
	}
}
