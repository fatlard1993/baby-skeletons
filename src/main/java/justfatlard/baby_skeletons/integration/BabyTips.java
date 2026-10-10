package justfatlard.baby_skeletons.integration;

import justfatlard.baby_skeletons.Babies;
import justfatlard.block_tip.api.BlockTipApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Calling a baby a baby when you are looking at one.
 *
 * <p>A baby is the skeleton's own entity at half size, so the game introduces it by its type's name:
 * the half-size stray at your feet is a Stray. Everywhere else that is fine - a death message that
 * said "shot by Baby Stray" would need a custom name, and a custom name hangs in the air over the
 * mob, which makes a wild skeleton look like somebody's pet. The card is where a player is actually
 * asking what this is, so the card is where it says.
 *
 * <p>The type's name comes from the game rather than a table here, so every kind of skeleton is
 * named, including any added after this was written.
 *
 * <p>Compiled against block-tip's API and guarded at the call site by a mod-loaded check, so a
 * server without it never loads this class.
 */
public final class BabyTips {
	private BabyTips() {}

	public static void register() {
		BlockTipApi.nameEntity(BabyTips::name);
	}

	private static String name(Entity entity, ServerPlayer player) {
		if (!Babies.is(entity)) return null;
		return "Baby " + entity.getType().getDescription().getString();
	}
}
