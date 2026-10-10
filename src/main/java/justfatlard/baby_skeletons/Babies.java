package justfatlard.baby_skeletons;

import java.util.Set;

import justfatlard.pandorical.api.PandoricalApi;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Which skeletons are babies, and what being one does to them.
 *
 * <p>A baby is the skeleton the game already has, at half the size. The scale attribute is part of
 * the vanilla protocol, so every client draws a half-size skeleton with its bow and hitbox halved to
 * match, and that is what a vanilla client sees. A client with Pandorical draws it with this mod's baby
 * model instead: the skeleton's own parts resized to a baby zombie's proportions, at a baby
 * zombie's size. Nothing here is a new entity type, which is why a baby stray still fires
 * slowness, a baby bogged still poisons and a baby wither skeleton still withers: those are the
 * stray's, the bogged's and the wither skeleton's own code, and the baby is one.
 *
 * <p>The whole skeleton family, by class rather than by list: anything that is an
 * {@link AbstractSkeleton} - skeleton, stray, bogged, parched and wither skeleton in 26.3 - can be
 * a baby, and the next one Mojang adds will be without anybody remembering to say so. The skeleton
 * horse is in the game's {@code #skeletons} tag but is not an AbstractSkeleton, which is the line
 * this mod wants: it is a horse.
 *
 * <p>Which skeleton is a baby is written on it as a scoreboard tag, the way beets-bears marks its
 * bears. Tags ride in the entity's own save data, are carried across the skeleton-to-stray freeze by
 * the game itself, and can be read and set with {@code /tag}.
 */
public final class Babies {
	private Babies() {}

	/** What a baby wears to say it is one. */
	public static final String TAG = "baby_skeleton";

	/** Half a grown one: a baby zombie's hitbox, and what a client without Pandorical draws a baby at. */
	public static final float SCALE = 0.5F;

	/**
	 * Half again as fast, for a baby that fights hand to hand. The baby zombie's own number
	 * ({@code Zombie.SPEED_MODIFIER_BABY}), applied the same way, because what makes a baby zombie
	 * frightening is that it closes the distance before you can back away. An archer has no
	 * distance to close: it stops at {@link #REACH} and shoots, so a baby with a bow walks at a
	 * grown skeleton's pace. See {@link #applySpeed}.
	 */
	public static final double SPEED_BONUS = 0.5;

	/** One in twenty: {@code Zombie.getSpawnAsBabyOdds}. */
	public static final float SPAWN_CHANCE = 0.05F;

	/** A baby zombie is worth two and a half times a grown one, and so is this. */
	public static final double XP_MULTIPLIER = 2.5;

	/**
	 * How hard a baby draws a bow, against a grown skeleton's. The arrow leaves at three quarters
	 * of the speed, and an arrow's damage goes with its speed, so it hits about a quarter softer and
	 * drops short of anything a grown skeleton could reach. The aim is still a grown skeleton's, which
	 * assumes the full draw: past about ten blocks the arrows land in front of you.
	 */
	public static final float DRAW = 0.75F;

	/**
	 * Where a baby stops to shoot, in blocks. A grown skeleton stops at fifteen; the baby comes in to
	 * ten because its weak draw does not carry further.
	 */
	public static final float REACH = 10F;

	/**
	 * Spawns that never come out as babies. A conversion keeps whatever it converts, so a frozen baby
	 * skeleton is a baby stray and a grown one is not suddenly small; the skeleton trap's riders
	 * ({@code TRIGGERED}) sit horses, and a baby in the saddle would be a novelty, not a threat.
	 * {@code JOCKEY} is here for anything that finishes a skeleton as a jockey's, but vanilla does not:
	 * the skeleton on a spider and the parched on a husk's camel are finished with the spider's or
	 * husk's own reason, and are rolled like any other spawn.
	 */
	private static final Set<EntitySpawnReason> NEVER = Set.of(
		EntitySpawnReason.CONVERSION, EntitySpawnReason.JOCKEY, EntitySpawnReason.TRIGGERED,
		EntitySpawnReason.LOAD, EntitySpawnReason.DIMENSION_TRAVEL);

	private static final Identifier SIZE = Identifier.fromNamespaceAndPath(Main.MOD_ID, "baby_size");
	private static final Identifier SPEED = Identifier.fromNamespaceAndPath(Main.MOD_ID, "baby_speed");

	/** Whether this is a baby skeleton: a skeleton of any kind, wearing the tag. */
	public static boolean is(@Nullable Entity entity) {
		return entity instanceof AbstractSkeleton && entity.entityTags().contains(TAG);
	}

	/**
	 * Whether a skeleton coming into the world this way should be a baby.
	 *
	 * <p>Rolled for every spawn the game finalizes - natural, spawner, trial spawner, structure,
	 * spawn egg and {@code /summon} alike - which is where a baby zombie's roll happens too, so an egg
	 * and a command behave exactly as the night does.
	 */
	public static boolean bornNow(EntitySpawnReason reason, RandomSource random) {
		return !NEVER.contains(reason) && random.nextFloat() < SPAWN_CHANCE;
	}

	/**
	 * Make this skeleton a baby, for good. There is no growing up: a baby skeleton stays one.
	 *
	 * <p>Safe to call on one that already is; nothing stacks.
	 */
	public static void become(AbstractSkeleton skeleton) {
		skeleton.addTag(TAG);
		applySize(skeleton);
		skeleton.setHealth(skeleton.getMaxHealth());
		smallerWeapon(skeleton);
	}

	/**
	 * The size, health and speed, written into the skeleton's own attributes.
	 *
	 * <p>Health goes with size, the way it does for beets-bears' bears: half the skeleton, half the
	 * skeleton to break. Said again every time a baby loads, because the tag is the truth and the
	 * attributes follow it - which is what makes a baby that froze into a stray (the conversion copies
	 * tags but not attribute modifiers) or one summoned with the tag already on it come out the right
	 * size. Each modifier has a fixed id, so saying it twice replaces rather than stacks.
	 *
	 * <p>Here too it is given its baby model for Pandorical to draw, which Pandorical keeps only
	 * while the skeleton is loaded: every way a baby comes into the world - born, hatched, summoned,
	 * frozen into a stray, read back from the save - passes through here. The model is a baby's
	 * proportions and is small already, so it is drawn at twice the scale attribute's half, which
	 * puts it at a baby zombie's size; the server keeps the half, for the hitbox and every other client.
	 */
	public static void applySize(AbstractSkeleton skeleton) {
		if (!is(skeleton)) return;
		PandoricalApi.entityModels().set(skeleton, MODEL, null, 1F / SCALE);
		permanent(skeleton.getAttribute(Attributes.SCALE), SIZE, SCALE - 1F);
		permanent(skeleton.getAttribute(Attributes.MAX_HEALTH), SIZE, SCALE - 1F);
		applySpeed(skeleton);
		if (skeleton.getHealth() > skeleton.getMaxHealth()) skeleton.setHealth(skeleton.getMaxHealth());
		// The game would shrink the hitbox on the skeleton's next tick; now means a baby being placed
		// into the world is checked for room at its own size and not a grown one's.
		skeleton.refreshDimensions();
	}

	/**
	 * The speed goes with how the skeleton fights: half again as fast with a blade or bare hands,
	 * a grown one's pace with a bow.
	 *
	 * <p>Decided by the same thing the game decides it with. A skeleton holding a bow is given the
	 * bow goal and anything else is given the melee one, and it asks again every time its hands
	 * change ({@code AbstractSkeleton.reassessWeaponGoal}, from equipping, loading and spawning).
	 * This is said there too, so a wither skeleton that picks up a bow slows down, and a skeleton
	 * that picks up a sword speeds up, the moment it does, and nothing is checked on any other tick.
	 *
	 * <p>Anything that is not a melee baby has the bonus taken away, which is what a baby saved by
	 * 1.0.0 needs: every baby carried it then, archers included, and it is in their save.
	 */
	public static void applySpeed(AbstractSkeleton skeleton) {
		AttributeInstance speed = skeleton.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		if (is(skeleton) && fightsHandToHand(skeleton)) permanent(speed, SPEED, SPEED_BONUS);
		else speed.removeModifier(SPEED);
	}

	/** The game's own test for which attack goal a skeleton gets: a bow in the hand that would draw it. */
	public static boolean fightsHandToHand(AbstractSkeleton skeleton) {
		return !skeleton.getItemInHand(ProjectileUtil.getWeaponHoldingHand(skeleton, Items.BOW)).is(Items.BOW);
	}

	/**
	 * The baby model: the skeleton's own parts, with its own texture, moved and resized to where a
	 * 26.3 baby zombie has them - a head three quarters of a grown one's on a body and arms 5/12 as
	 * tall and half as wide, on legs a third as long, 15 pixels from crown to sole. In
	 * {@code assets/baby-skeletons-justfatlard/pandorical/entity_models/baby_skeleton.json}. One for
	 * every kind: they share the skeleton's limbs, and the wither skeleton is drawn a fifth larger by
	 * the root they hang from, so its baby is a fifth larger too.
	 */
	public static final Identifier MODEL = Identifier.fromNamespaceAndPath(Main.MOD_ID, "baby_skeleton");

	private static void permanent(@Nullable AttributeInstance attribute, Identifier id, double amount) {
		if (attribute == null) return;
		attribute.addOrReplacePermanentModifier(
			new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
	}

	/**
	 * A wooden sword where a grown wither skeleton carries stone: a smaller blade for a smaller hand,
	 * a point less damage, and the wither on every hit all the same, because that is the wither
	 * skeleton's and not the sword's. Only the sword the game hands out is swapped - a baby that has
	 * picked up something better keeps it.
	 */
	private static void smallerWeapon(AbstractSkeleton skeleton) {
		if (!(skeleton instanceof WitherSkeleton)) return;
		if (!skeleton.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.STONE_SWORD)) return;
		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
	}
}
