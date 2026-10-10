package justfatlard.baby_skeletons.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import justfatlard.baby_skeletons.Babies;
import justfatlard.baby_skeletons.Jockeys;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A baby is worth more experience, the way a baby zombie is; and a jockey despawns the way a
 * vanilla one does, without ever leaving its rider stranded.
 */
@Mixin(Mob.class)
public abstract class MobMixin {

	/**
	 * On {@code Mob} because no skeleton overrides the method; the baby check keeps it to babies.
	 * The zombie's version multiplies its stored reward in place every time it is asked, which
	 * compounds if anything asks twice; this multiplies the answer and leaves the skeleton alone.
	 */
	@ModifyReturnValue(method = "getBaseExperienceReward", at = @At("RETURN"))
	private int babySkeletons$bounty(int experience) {
		return Babies.is((Mob) (Object) this) ? (int) (experience * Babies.XP_MULTIPLIER) : experience;
	}

	/**
	 * Who is kept. Vanilla keeps the rider, because a passenger never despawns, and lets its chicken
	 * despawn as a monster would: so a chicken can go and leave its baby zombie on foot. Here it is
	 * the other way about. A jockey's mount carrying its baby is kept; the baby, riding one, despawns
	 * as the monster it is, and its mount then goes as a jockey's chicken does, by the next rule.
	 *
	 * <p>Read where {@code checkDespawn} asks, rather than by overriding the question, because every
	 * mount here answers it in its own class and the game only ever asks it here.
	 */
	@ModifyExpressionValue(method = "checkDespawn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Mob;requiresCustomPersistence()Z"))
	private boolean babySkeletons$keepTheMount(boolean kept) {
		Mob self = (Mob) (Object) this;
		if (Jockeys.carrying(self)) return true;
		if (Jockeys.riding(self)) return self.isLeashed();
		return kept;
	}

	/**
	 * A jockey's mount despawns as a monster does, as {@code Chicken.removeWhenFarAway} has a jockey's
	 * chicken do, whatever its kind would otherwise say: an animal never despawns, and a lone goat,
	 * frog or calf from a jockey would otherwise stand where its rider died for good.
	 */
	@ModifyExpressionValue(method = "checkDespawn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Mob;removeWhenFarAway(D)Z"))
	private boolean babySkeletons$mountsDespawn(boolean removable) {
		return removable || Jockeys.isMount((Mob) (Object) this);
	}
}
