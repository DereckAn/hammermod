package wallywhip.thehammermod.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wallywhip.thehammermod.DropEnchantments;

@Mixin(Block.class)
public class BlockMixin {
	@Inject(
		method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
		at = @At("RETURN"),
		cancellable = true
	)
	private static void thehammermod$applyDropEnchantments(
		BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemInstance tool,
		CallbackInfoReturnable<List<ItemStack>> cir
	) {
		if (breaker instanceof ServerPlayer player) {
			cir.setReturnValue(DropEnchantments.apply(cir.getReturnValue(), state, level, player, tool));
		}
	}
}
