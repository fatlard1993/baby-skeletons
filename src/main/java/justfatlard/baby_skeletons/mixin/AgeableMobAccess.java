package justfatlard.baby_skeletons.mixin;

import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The age lock a golden dandelion sets, for a jockey's young mount. */
@Mixin(AgeableMob.class)
public interface AgeableMobAccess {

	@Invoker("setAgeLocked")
	void babySkeletons$lockAge(boolean locked);
}
