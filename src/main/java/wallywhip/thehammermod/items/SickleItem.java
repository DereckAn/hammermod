package wallywhip.thehammermod.items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import wallywhip.thehammermod.TheHammerMod;
import wallywhip.thehammermod.config.HammerConfig;

/** Harvests (and replants) plants of the same kind in an area; on anything else it behaves like a hammer/excavator. */
public class SickleItem extends AoeToolItem {
	public SickleItem(Properties properties) {
		super(ToolType.SICKLE, properties);
	}

	/**
	 * Plant kind: blocks only get harvested together with the same kind. 0 = not a plant (normal AOE mining),
	 * negative = a plant that isn't ready (immature crop/nether wart). 3 crop, 4 flower/grass, 5 sugar cane/vines, 6 chorus, 7 nether wart.
	 */
	public static int plantKind(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMaxAge(state) ? 3 : -3;
		}
		if (block instanceof FlowerBlock) {
			return 4;
		}
		if (block instanceof StemBlock || block instanceof AttachedStemBlock) {
			return 0;
		}
		if (block instanceof SugarCaneBlock) {
			return 5;
		}
		if (block == Blocks.CHORUS_PLANT) {
			return 6;
		}
		if (block instanceof VegetationBlock) {
			if (block instanceof NetherWartBlock) {
				return state.isRandomlyTicking() ? -7 : 7;
			}
			return 4;
		}
		return block instanceof GrowingPlantHeadBlock ? 5 : 0;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState originalState, BlockPos originalPos, LivingEntity owner) {
		if (!(level instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer player)) {
			return true;
		}

		int kind = plantKind(originalState);
		if (kind == 0) {
			return this.breakArea(stack, serverLevel, originalState, originalPos, player);
		}

		BlockHitResult hit = clipOriginalBlock(level, player, originalState, originalPos);
		if (hit == null) {
			return false;
		}

		int radius = player.isShiftKeyDown() ? 0 : TheHammerMod.getMode(stack);
		List<BlockPos> area = areaPositions(originalPos, hit.getDirection(), radius, player, true);
		int originIndex = area.indexOf(originalPos);
		if (!harvestIfSameKind(originalPos, serverLevel, player, stack, kind, originalState)) {
			return false;
		}

		spawnSweepParticles(serverLevel, player);
		for (int i = 1; i < area.size() && !stack.isEmpty(); i++) {
			BlockPos pos = area.get((originIndex + i) % area.size());
			harvestIfSameKind(pos, serverLevel, player, stack, kind, level.getBlockState(pos));
		}
		return true;
	}

	private static boolean harvestIfSameKind(BlockPos pos, ServerLevel level, ServerPlayer player, ItemStack stack, int kind, BlockState state) {
		int targetKind = plantKind(state);
		boolean sameKind = kind > 0 && targetKind == kind && player.hasCorrectToolForDrops(state);
		if (sameKind) {
			harvest(level, player, pos, state, stack, targetKind);
			stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
		return sameKind;
	}

	/** Breaks the plant and everything of the same block stacked on it, then replants the bottom one (flowers/grass are not replanted). */
	private static void harvest(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool, int kind) {
		List<ItemStack> drops = new ArrayList<>();
		Block block = state.getBlock();
		for (BlockPos above = pos; level.getBlockState(above).is(block); above = above.above()) {
			BlockState aboveState = level.getBlockState(above);
			drops.addAll(Block.getDrops(aboveState, level, above, null, player, tool));
			level.removeBlock(above, false);
			block.playerWillDestroy(level, above, aboveState, player);
			block.destroy(level, above, aboveState);
		}

		if (kind != 4) {
			replant(level, player, pos, state, block, drops);
		}

		if (TheHammerMod.enchantmentLevel(tool, TheHammerMod.DIGGERS_MAGNET) > 0) {
			drops.forEach(drop -> player.getInventory().add(drop));
		}
		drops.forEach(drop -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop));
	}

	private static void replant(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, Block block, List<ItemStack> drops) {
		Item seed;
		if (block instanceof GrowingPlantBodyBlock) {
			seed = state.getCloneItemStack(level, pos, false).getItem();
		} else if (block == Blocks.CHORUS_PLANT) {
			seed = Items.CHORUS_FLOWER;
		} else {
			seed = block.asItem();
		}

		boolean hasSeed = false;
		if (!level.getBlockState(pos.below()).is(block)) {
			if (player.isCreative()) {
				hasSeed = true;
			} else {
				player.awardStat(Stats.BLOCK_MINED.get(block));
				player.causeFoodExhaustion(0.005F);
				hasSeed = !HammerConfig.get().requiresSeeds || takeSeed(player, seed, drops);
			}
		}

		if (hasSeed) {
			level.setBlockAndUpdate(pos, Block.byItem(seed).defaultBlockState());
			player.awardStat(Stats.ITEM_USED.get(seed));
		}
	}

	/** With "requires seeds" on, replanting uses a seed from the drops first, then from the inventory. */
	private static boolean takeSeed(Player player, Item seed, List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			if (drop.is(seed)) {
				drop.shrink(1);
				return true;
			}
		}
		int slot = player.getInventory().findSlotMatchingItem(new ItemStack(seed));
		if (slot != -1) {
			player.getInventory().getItem(slot).shrink(1);
			return true;
		}
		return false;
	}

	private static void spawnSweepParticles(ServerLevel level, Player player) {
		double dx = -Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
		double dz = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dx, player.getY(0.5), player.getZ() + dz, 0, dx, 0.0, dz, 0.0);
	}

	@Override
	public boolean canTransformBlock(Level level, BlockHitResult hit, Player player) {
		return BlockTransformerHelper.wouldTransform(level, hit, BlockTransformers.HOE);
	}
}
