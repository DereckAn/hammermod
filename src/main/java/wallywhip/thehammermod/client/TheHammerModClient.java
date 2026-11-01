package wallywhip.thehammermod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class TheHammerModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LevelRenderEvents.END_EXTRACTION.register(AoeHighlight::extract);
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(AoeHighlight::render);
	}
}
