package justfatlard.baby_skeletons.mixin;

import justfatlard.baby_skeletons.Jockeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mount that thinks with a brain rests it while a baby steers.
 *
 * <p>The game already does this for a mount that thinks with goals: ridden by a mob, its goals that
 * move, look or jump are switched off ({@code Mob.updateControlFlags}), and the rider's goals drive
 * it through the mount's own navigation. A brain has no such switch, so a goat, frog, armadillo or
 * hoglin would go on strolling, panicking, ramming and rolling up underneath its rider, and pull
 * the navigation the rider is steering with off to wherever it fancied. Resting the brain is the
 * chicken's treatment for those four; the mount's body - its navigation and move control, which is
 * what the rider steers - goes on ticking. Its brain picks up again the moment its rider is gone.
 */
@Mixin(Brain.class)
public abstract class BrainMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void babySkeletons$restWhileSteered(ServerLevel level, LivingEntity body, CallbackInfo ci) {
		if (Jockeys.steered(body)) ci.cancel();
	}
}
