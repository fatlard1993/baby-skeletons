package justfatlard.baby_skeletons.gametest;

import java.util.ArrayList;
import java.util.List;

import justfatlard.baby_skeletons.Babies;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import static justfatlard.baby_skeletons.gametest.Kit.check;
import static justfatlard.baby_skeletons.gametest.Kit.near;

/**
 * A baby of every kind is half the size and half the health of a grown one of its kind, half again
 * as fast if it fights hand to hand and a grown one's pace if it shoots, has a hitbox half as wide, keeps its kind's weapon (a smaller one for the wither skeleton),
 * is worth more experience, and stays the size it is through a reload and a freeze into a stray.
 */
public final class BabiesAreSmall implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, Kit.MOD_ID, session -> {
			Kit.night(session);

			String measured = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				List<String> wrong = new ArrayList<>();
				int column = 0;
				for (EntityType<? extends AbstractSkeleton> type : Kit.FAMILY) {
					String name = EntityType.getKey(type).getPath();
					BlockPos adultAt = Kit.ground(session, level, 4 + column, 3);
					BlockPos babyAt = Kit.ground(session, level, 4 + column, -3);
					column += 3;
					AbstractSkeleton adult = Kit.adult(level, type, adultAt);
					AbstractSkeleton baby = Kit.baby(level, type, babyAt);
					adult.setNoAi(true);
					baby.setNoAi(true);

					double scale = baby.getAttributeValue(Attributes.SCALE);
					if (!near(scale, Babies.SCALE)) wrong.add(name + " scale " + scale);
					if (PandoricalApi.entityModels().get(baby) == null) wrong.add(name + " not given a baby model");
					if (PandoricalApi.entityModels().get(adult) != null) wrong.add("a grown " + name + " is given a baby model");
					if (!near(baby.getMaxHealth(), adult.getMaxHealth() * Babies.SCALE)) {
						wrong.add(name + " max health " + baby.getMaxHealth() + " against a grown one's " + adult.getMaxHealth());
					}
					if (!near(baby.getHealth(), baby.getMaxHealth())) wrong.add(name + " not born at full health");
					// Quick if it fights hand to hand, a grown one's pace with a bow: of the family as
					// it spawns, only the wither skeleton is quick.
					double speed = baby.getAttributeValue(Attributes.MOVEMENT_SPEED);
					double grownSpeed = adult.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
					double expected = baby instanceof WitherSkeleton ? grownSpeed * (1 + Babies.SPEED_BONUS) : grownSpeed;
					if (!near(speed, expected)) wrong.add(name + " speed " + speed + ", expected " + expected);
					if (!near(baby.getBbWidth(), adult.getBbWidth() * Babies.SCALE)) {
						wrong.add(name + " hitbox " + baby.getBbWidth() + " wide against " + adult.getBbWidth());
					}
					if (!near(baby.getBbHeight(), adult.getBbHeight() * Babies.SCALE)) {
						wrong.add(name + " hitbox " + baby.getBbHeight() + " tall against " + adult.getBbHeight());
					}
					if (baby instanceof WitherSkeleton) {
						if (!baby.getMainHandItem().is(Items.WOODEN_SWORD)) wrong.add(name + " holds " + baby.getMainHandItem());
					} else if (!baby.isHolding(Items.BOW)) {
						wrong.add(name + " has no bow");
					}

					int babyXp = baby.getExperienceReward(level, session.player());
					int adultXp = adult.getExperienceReward(level, session.player());
					// A grown skeleton is worth 5, plus 1-3 for each piece of gear it carries.
					if (babyXp < (int) (5 * Babies.XP_MULTIPLIER)) wrong.add(name + " worth " + babyXp + " xp, a grown one " + adultXp);

					// Saying the size again, as every load does, must not stack it.
					Babies.applySize(baby);
					Babies.applySize(baby);
					if (!near(baby.getAttributeValue(Attributes.SCALE), Babies.SCALE)) wrong.add(name + " grew on reload");
					if (!near(baby.getMaxHealth(), adult.getMaxHealth() * Babies.SCALE)) wrong.add(name + " health stacked on reload");
				}
				return wrong.isEmpty() ? "ok" : String.join("; ", wrong);
			});
			check("ok".equals(measured), measured);

			// The speed follows the weapon, both ways and at once: a wither skeleton handed a bow
			// slows to a walk, a skeleton handed a sword speeds up, and each goes back again.
			String swapped = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				List<String> wrong = new ArrayList<>();
				AbstractSkeleton wither = Kit.baby(level, EntityTypes.WITHER_SKELETON, Kit.ground(session, level, 4, 8));
				AbstractSkeleton archer = Kit.baby(level, EntityTypes.SKELETON, Kit.ground(session, level, 7, 8));
				wither.setNoAi(true);
				archer.setNoAi(true);
				double walk = 0.25;
				double run = walk * (1 + Babies.SPEED_BONUS);
				wither.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				if (!near(wither.getAttributeValue(Attributes.MOVEMENT_SPEED), walk)) wrong.add("a wither skeleton with a bow runs");
				if (!near(archer.getAttributeValue(Attributes.MOVEMENT_SPEED), run)) wrong.add("a skeleton with a sword walks");
				wither.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
				archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				if (!near(wither.getAttributeValue(Attributes.MOVEMENT_SPEED), run)) wrong.add("a wither skeleton given its sword back walks");
				if (!near(archer.getAttributeValue(Attributes.MOVEMENT_SPEED), walk)) wrong.add("a skeleton given its bow back runs");
				// A grown one is never quick, whatever it holds.
				AbstractSkeleton grown = Kit.adult(level, EntityTypes.SKELETON, Kit.ground(session, level, 10, 8));
				grown.setNoAi(true);
				grown.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				if (!near(grown.getAttributeValue(Attributes.MOVEMENT_SPEED), walk)) wrong.add("a grown skeleton with a sword runs");
				return wrong.isEmpty() ? "ok" : String.join("; ", wrong);
			});
			check("ok".equals(swapped), "weapon swaps: " + swapped);

			// A baby archer saved by 1.0.0, which gave every baby the bonus, loses it as it loads; a
			// baby wither skeleton saved the same way keeps it. Read from save data by /summon, the
			// way a chunk reads it, with a second modifier of the test's own beside it to show the
			// save data was read at all.
			String saved = "{Tags:[\"baby_skeleton\"],NoAI:1b,PersistenceRequired:1b,equipment:{mainhand:{id:\"%s\"}},"
				+ "attributes:[{id:\"minecraft:movement_speed\",base:0.25,modifiers:["
				+ "{id:\"baby-skeletons-justfatlard:baby_speed\",amount:0.5,operation:\"add_multiplied_base\"},"
				+ "{id:\"baby-skeletons-gametest:witness\",amount:0.0,operation:\"add_value\"}]}]}";
			Kit.command(session, "summon skeleton ~-6 ~ ~6 " + saved.formatted("minecraft:bow"));
			Kit.command(session, "summon wither_skeleton ~-9 ~ ~6 " + saved.formatted("minecraft:wooden_sword"));
			session.waitTicks(2);
			String loaded = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				List<String> wrong = new ArrayList<>();
				var speedId = net.minecraft.resources.Identifier.fromNamespaceAndPath(Kit.MOD_ID, "baby_speed");
				var witness = net.minecraft.resources.Identifier.fromNamespaceAndPath("baby-skeletons-gametest", "witness");
				List<AbstractSkeleton> found = level.getEntitiesOfClass(AbstractSkeleton.class,
					session.player().getBoundingBox().inflate(16),
					s -> s.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(witness) != null);
				if (found.size() != 2) return "found " + found.size() + " skeletons read from the old save";
				for (AbstractSkeleton skeleton : found) {
					String name = EntityType.getKey(skeleton.getType()).getPath();
					boolean bonus = skeleton.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(speedId) != null;
					if (!Babies.is(skeleton)) wrong.add(name + " is not a baby");
					if (skeleton instanceof WitherSkeleton && !bonus) wrong.add("the old baby wither skeleton lost its speed");
					if (!(skeleton instanceof WitherSkeleton) && bonus) {
						wrong.add("the old baby archer kept its speed: " + skeleton.getAttributeValue(Attributes.MOVEMENT_SPEED));
					}
				}
				return wrong.isEmpty() ? "ok" : String.join("; ", wrong);
			});
			check("ok".equals(loaded), "loaded from an old save: " + loaded);
			System.out.println("[baby-skeletons test] speed: archers walk, the wither skeleton runs, weapon swaps and old saves follow");

			// A baby skeleton that freezes in powder snow comes out a baby stray, the right size. It
			// stands in the snow a few ticks first: the freeze is called off on any tick the skeleton
			// does not yet know it is in snow, which is its first.
			BlockPos snow = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				BlockPos at = Kit.ground(session, level, -5, 0);
				level.setBlockAndUpdate(at, Blocks.POWDER_SNOW.defaultBlockState());
				level.setBlockAndUpdate(at.above(), Blocks.POWDER_SNOW.defaultBlockState());
				Kit.baby(level, EntityTypes.SKELETON, at);
				return at;
			});
			session.waitTicks(5);
			session.onServer(server -> server.overworld().getEntitiesOfClass(Skeleton.class,
				new AABB(snow).inflate(3), Babies::is).forEach(skeleton -> skeleton.startFreezeConversion(2)));
			session.waitTicks(20);
			String frozen = Kit.ask(session, server -> {
				ServerLevel level = server.overworld();
				// Searched for widely: a baby stray goes looking for a grown stray the moment it is one,
				// and there is one a few blocks off from the size checks above. Those all have their
				// brains off, which is how this one is told from them.
				List<Stray> strays = level.getEntitiesOfClass(Stray.class, new AABB(snow).inflate(24),
					stray -> !stray.isNoAi());
				if (strays.size() != 1) {
					List<String> around = level.getEntitiesOfClass(AbstractSkeleton.class, new AABB(snow).inflate(40))
						.stream().map(s -> s.getType().toShortString() + "@" + s.blockPosition().toShortString()
							+ (Babies.is(s) ? " baby" : "") + " snow=" + s.isInPowderSnow).toList();
					return "found " + strays.size() + " strays where the skeleton froze at " + snow.toShortString()
						+ "; skeletons about: " + around;
				}
				Stray stray = strays.get(0);
				if (!Babies.is(stray)) return "the stray is not a baby";
				if (PandoricalApi.entityModels().get(stray) == null) return "the baby stray is not given its baby model";
				if (!near(stray.getAttributeValue(Attributes.SCALE), Babies.SCALE)) {
					return "the baby stray is scale " + stray.getAttributeValue(Attributes.SCALE);
				}
				return "ok";
			});
			check("ok".equals(frozen), "freezing: " + frozen);
			Kit.passed("BabiesAreSmall");
		});
	}
}
