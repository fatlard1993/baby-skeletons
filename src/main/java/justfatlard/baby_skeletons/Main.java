package justfatlard.baby_skeletons;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baby skeletons, out of the skeletons the game already has.
 *
 * <p>Every kind of skeleton now and then comes out half-size: frailer, a weaker bow, quick only with a
 * blade, and once in a while riding something. See {@link Babies} for why a baby is not a new mob,
 * and {@link Jockeys} for what it rides.
 */
public class Main implements ModInitializer {
	public static final String MOD_ID = "baby-skeletons-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String BLOCK_TIP = "block-tip";

	@Override
	public void onInitialize() {
		Eggs.register();
		// The baby models, shipped to Pandorical clients with the mod's other assets.
		PandoricalApi.content().registerModAssets(MOD_ID);

		// The size is said again on every load, because a conversion copies the tag but not the
		// attributes, and so is the baby model Pandorical clients draw it with, which Pandorical
		// forgets when the skeleton unloads.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof AbstractSkeleton skeleton) Babies.applySize(skeleton);
		});

		// Isolated in its own class and only reached from here: it names block-tip's types directly,
		// so it must not load without that mod.
		if (FabricLoader.getInstance().isModLoaded(BLOCK_TIP)) {
			justfatlard.baby_skeletons.integration.BabyTips.register();
		}

		LOGGER.info("[{}] Baby skeletons, one in {}", MOD_ID, Math.round(1F / Babies.SPAWN_CHANCE));
	}
}
