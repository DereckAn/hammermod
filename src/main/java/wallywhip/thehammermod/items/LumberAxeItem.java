package wallywhip.thehammermod.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import wallywhip.thehammermod.TheHammerMod;

/** Fells the whole log column above the mined log (mode 0), or a single block (mode 1 or sneaking). */
public class LumberAxeItem extends AoeToolItem {
	public LumberAxeItem(Properties properties) {
		super(ToolType.LUMBER, properties);
	}

	public static boolean isTreeTrunk(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(TheHammerMod.MUSHROOM_STEMS_TAG);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState originalState, BlockPos originalPos, LivingEntity owner) {
		if (!(level instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer player)) {
			return true;
		}

		if (player.isShiftKeyDown() || TheHammerMod.getMode(stack) == 1 || !isTreeTrunk(originalState)) {
			if (originalState.getDestroySpeed(level, originalPos) > 0.0F) {
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}
			return true;
		}

		Block log = originalState.getBlock();
		BlockPos pos = originalPos;
		BlockState state = originalState;
		while (state.is(log)) {
			float hardness = state.getDestroySpeed(level, pos);
			if (pos != originalPos) { // vanilla already broke the clicked log
				TheHammerMod.breakExtraBlock(serverLevel, pos, player, state);
			}
			if (hardness > 0.0F) {
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				if (stack.isEmpty()) {
					return true;
				}
			}
			pos = pos.above();
			state = level.getBlockState(pos);
		}
		return true;
	}

	@Override
	public boolean canTransformBlock(Level level, BlockHitResult hit, Player player) {
		return BlockTransformerHelper.wouldTransform(level, hit, BlockTransformers.AXE);
	}
}
