package justfatlard.baby_skeletons.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import justfatlard.baby_skeletons.Babies;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** A baby archer comes in to {@link Babies#REACH} before it stops to shoot, because its weak draw does not carry. */
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin {

	@Shadow @Final private Monster mob;

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/ai/goal/RangedBowAttackGoal;attackRadiusSqr:F"))
	private float babySkeletons$reach(float original) {
		return Babies.is(mob) ? Babies.REACH * Babies.REACH : original;
	}
}
