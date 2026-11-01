package wallywhip.thehammermod.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block-break particles when stripping, flattening or tilling. Replaces the 1.19.2 AxeItem/ShovelItem/HoeItem mixins (those classes are gone). */
@Mixin(BlockTransformer.class)
public class BlockTransformerMixin {
	@Inject(
		method = "transformBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
	)
	private void thehammermod$breakParticles(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Level level = context.getLevel();
		if (level.isClientSide()) {
			BlockPos pos = context.getClickedPos();
			level.addDestroyBlockEffect(pos, level.getBlockState(pos)); // still the old block here
		}
	}
}
