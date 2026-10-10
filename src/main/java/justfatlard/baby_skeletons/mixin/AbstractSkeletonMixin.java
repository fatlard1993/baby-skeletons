package justfatlard.baby_skeletons.mixin;

import justfatlard.baby_skeletons.Babies;
import justfatlard.baby_skeletons.Jockeys;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a baby is born, what it rides, how fast it goes, and how hard it draws a bow.
 */
@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {

	/**
	 * The one-in-twenty roll, as the skeleton finishes spawning.
	 *
	 * <p>On the skeleton's own {@code finalizeSpawn} rather than {@code Mob}'s, because the skeleton
	 * hands out its weapon after {@code Mob}'s has returned, and a baby wither skeleton's smaller sword
	 * has to replace the one it was given rather than be replaced by it.
	 */
	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void babySkeletons$maybeBorn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (!Babies.bornNow(reason, level.getRandom())) return;
		AbstractSkeleton baby = (AbstractSkeleton) (Object) this;
		Babies.become(baby);
		// And, as a baby zombie does at the same moment, the jockey roll.
		Jockeys.maybeRide(baby, level, difficulty, level.getRandom());
	}

	/**
	 * A baby's speed follows its weapon. The game asks which attack goal a skeleton gets every time
	 * its hands change, here; the speed is asked at the same moment, so it costs nothing on any other
	 * tick.
	 */
	@Inject(method = "reassessWeaponGoal", at = @At("RETURN"))
	private void babySkeletons$paceToWeapon(CallbackInfo ci) {
		AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
		if (skeleton.level() != null && !skeleton.level().isClientSide()) Babies.applySpeed(skeleton);
	}

	/** A baby's arrow leaves the bow slower, which carries less far and hits less hard. */
	@ModifyArg(method = "performRangedAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"),
		index = 6)
	private float babySkeletons$weakDraw(float velocity) {
		return Babies.is((AbstractSkeleton) (Object) this) ? velocity * Babies.DRAW : velocity;
	}
}
