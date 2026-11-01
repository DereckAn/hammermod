package wallywhip.thehammermod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pickaxe + axe + shovel in one. Right-click strips/scrapes/waxes off like an axe, otherwise tills like a hoe, or makes a path like a
 * shovel while sneaking. Dousing campfires comes from the minecraft:douses_campfires item tag.
 */
public class PaxelItem extends AoeToolItem {
	public PaxelItem(Properties properties) {
		super(ToolType.PAXEL, properties);
	}

	/** Paxels mine anything of similar hardness, no "same kind of block" check. */
	@Override
	public boolean canBreakBlock(ItemStack tool, BlockGetter level, BlockPos targetPos, BlockState targetState, BlockState originalState, float originalHardness) {
		return hasSimilarHardness(level, targetPos, targetState, originalHardness);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult result = BlockTransformerHelper.transform(context, BlockTransformers.AXE);
		if (result != InteractionResult.PASS) {
			return result;
		}
		boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
		return BlockTransformerHelper.transform(context, sneaking ? BlockTransformers.SHOVEL : BlockTransformers.HOE);
	}
}
