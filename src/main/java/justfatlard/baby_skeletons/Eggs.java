package justfatlard.baby_skeletons;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A spawn egg used on a grown skeleton hatches a baby of its kind, the way an egg used on a grown
 * zombie hatches a baby zombie.
 *
 * <p>An egg used on the ground goes through the ordinary spawn and rolls the same one in twenty as
 * the night does. This is the way to get one for certain, and it is the gesture the game already
 * teaches.
 *
 * <p>Done here rather than by letting the game's own egg path through. That path makes the offspring
 * and asks it to be a baby, which a skeleton has no way to say yes to; and it never finishes spawning
 * the animal, which for a zombie costs nothing and for a skeleton means a baby with no bow. So the
 * baby is spawned properly, equipment and all, and then made small. It gets the jockey roll every
 * baby gets as it is born, so now and then one hatches on a chicken.
 */
public final class Eggs {
	private Eggs() {}

	public static void register() {
		UseEntityCallback.EVENT.register(Eggs::onUse);
	}

	private static InteractionResult onUse(Player player, Level level, InteractionHand hand,
			Entity entity, EntityHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
		if (!(entity instanceof AbstractSkeleton adult) || Babies.is(adult)) return InteractionResult.PASS;
		if (player.isSpectator()) return InteractionResult.PASS;

		ItemStack held = player.getItemInHand(hand);
		if (!(held.getItem() instanceof SpawnEggItem)) return InteractionResult.PASS;
		if (!SpawnEggItem.spawnsEntity(held, adult.getType())) return InteractionResult.PASS;

		if (!(adult.getType().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE) instanceof AbstractSkeleton baby)) {
			return InteractionResult.PASS;
		}
		baby.snapTo(adult.getX(), adult.getY(), adult.getZ(), adult.getYRot(), 0F);
		var difficulty = serverLevel.getCurrentDifficultyAt(baby.blockPosition());
		baby.finalizeSpawn(serverLevel, difficulty, EntitySpawnReason.SPAWN_ITEM_USE, null);
		boolean bornAlready = Babies.is(baby);
		Babies.become(baby);
		// The jockey roll a baby gets as it is born: already had if the spawn itself came out a baby.
		if (!bornAlready) Jockeys.maybeRide(baby, serverLevel, difficulty, serverLevel.getRandom());
		baby.applyComponentsFromItemStack(held);
		serverLevel.addFreshEntityWithPassengers(baby);
		held.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
