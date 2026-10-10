package justfatlard.baby_skeletons.gametest;

import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * This mod loads, its mixins bind, and a world comes up with it installed.
 *
 * <p>Nothing is asserted beyond getting there, and getting there proves most of it: a mixin that no
 * longer applies refuses the mod outright, and a registration that throws takes the client down. What
 * the babies actually do is the other three tests.
 */
public final class SmokeTest implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, "baby-skeletons-justfatlard", session -> {});
		Kit.passed("SmokeTest");
	}
}
