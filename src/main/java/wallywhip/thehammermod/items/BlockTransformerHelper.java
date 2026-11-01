package wallywhip.thehammermod.items;

import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Vanilla's data-driven right-click actions (strip/scrape/wax-off, flatten to path, till), which replaced AxeItem/ShovelItem/HoeItem. */
final class BlockTransformerHelper {
	private BlockTransformerHelper() {
	}

	static InteractionResult transform(UseOnContext context, ResourceKey<BlockTransformer> key) {
		return context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(key)
			.map(transformer -> transformer.value().transformBlock(context))
			.orElse(InteractionResult.PASS);
	}

	static boolean wouldTransform(Level level, BlockHitResult hit, ResourceKey<BlockTransformer> key) {
		return level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(key)
			.map(transformer -> transformer.value().transforms().stream().anyMatch(data ->
				!data.disallowedFaces().contains(hit.getDirection())
					&& data.blockStateProvider().value().getOptionalState(level, level.getRandom(), hit.getBlockPos()) != null))
			.orElse(false);
	}
}
