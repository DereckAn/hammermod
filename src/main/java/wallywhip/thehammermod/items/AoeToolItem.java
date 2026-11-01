package wallywhip.thehammermod.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import wallywhip.thehammermod.TheHammerMod;

/** Hammers and excavators directly; base class of lumber axes, sickles and paxels. Was the aoeBase interface. */
public class AoeToolItem extends Item {
	public enum ToolType {
		HAMMER,
		EXCAVATOR,
		LUMBER,
		SICKLE,
		PAXEL
	}

	private final ToolType toolType;

	public AoeToolItem(ToolType toolType, Item.Properties properties) {
		super(properties);
		this.toolType = toolType;
	}

	public ToolType getToolType() {
		return this.toolType;
	}

	/**
	 * Whether an AOE neighbour is mined along with the clicked block: similar hardness (at most 3x), and the same kind of block.
	 * 1.19.2 compared Block Materials, which no longer exist; "same kind" is now "this tool is effective on both", or the exact same block
	 * when the tool isn't effective on the clicked one.
	 */
	public boolean canBreakBlock(ItemStack tool, BlockGetter level, BlockPos targetPos, BlockState targetState, BlockState originalState, float originalHardness) {
		if (!hasSimilarHardness(level, targetPos, targetState, originalHardness)) {
			return false;
		}
		return tool.getDestroySpeed(originalState) > 1.0F ? tool.getDestroySpeed(targetState) > 1.0F : targetState.is(originalState.getBlock());
	}

	protected static boolean hasSimilarHardness(BlockGetter level, BlockPos targetPos, BlockState targetState, float originalHardness) {
		if (targetState.isAir()) {
			return false;
		}
		float targetHardness = targetState.getDestroySpeed(level, targetPos);
		return targetHardness >= 0.0F && (targetHardness == originalHardness || targetHardness / originalHardness <= 3.0F);
	}

	/** Right-click action the outline shows in the secondary colour (strip for lumber axes, till for sickles). */
	public boolean canTransformBlock(Level level, BlockHitResult hit, Player player) {
		return false;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
		if (level instanceof ServerLevel serverLevel && owner instanceof ServerPlayer player) {
			breakArea(stack, serverLevel, state, pos, player);
		}
		return true;
	}

	protected boolean breakArea(ItemStack stack, ServerLevel level, BlockState originalState, BlockPos originalPos, ServerPlayer player) {
		BlockHitResult hit = clipOriginalBlock(level, player, originalState, originalPos);
		if (hit == null) {
			return false;
		}

		float originalHardness = originalState.getDestroySpeed(level, originalPos);
		int radius = player.isShiftKeyDown() ? 0 : TheHammerMod.getMode(stack);
		List<BlockPos> area = areaPositions(originalPos, hit.getDirection(), radius, player, false);
		int originIndex = area.indexOf(originalPos);

		if (!breakIfAllowed(originalPos, level, originalState, originalState, originalHardness, player, stack, true)) {
			return false;
		}

		for (int i = 1; i < area.size() && !stack.isEmpty(); i++) {
			BlockPos pos = area.get((originIndex + i) % area.size());
			breakIfAllowed(pos, level, level.getBlockState(pos), originalState, originalHardness, player, stack, false);
		}

		TheHammerMod.doMinersDelight(level, originalPos, originalState, stack, player);
		return true;
	}

	/** isOrigin: the clicked block, which vanilla already broke, so only durability is charged. */
	private boolean breakIfAllowed(BlockPos pos, ServerLevel level, BlockState targetState, BlockState originalState, float originalHardness, ServerPlayer player, ItemStack stack, boolean isOrigin) {
		boolean allowed = this.canBreakBlock(stack, level, pos, targetState, originalState, originalHardness) && player.hasCorrectToolForDrops(targetState);
		if (allowed && (isOrigin || TheHammerMod.breakExtraBlock(level, pos, player, targetState)) && targetState.getDestroySpeed(level, pos) > 0.0F) {
			stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
		return allowed;
	}

	/** Which face of the mined block the player is looking at; null if the ray misses it. */
	public static @Nullable BlockHitResult clipOriginalBlock(Level level, Player player, BlockState state, BlockPos pos) {
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
		return state.getShape(level, pos).clip(eye, end, pos);
	}

	/**
	 * The (2r+1)x(2r+1) square around the origin on the plane of the hit face, in the 1.19.2 spiral order. For radius >= 2 the square is shifted
	 * so its bottom edge sits one block below the origin (side faces), or towards where the player faces when mining far above/below.
	 * Sickles pass horizontalPlane = true: they always cut a flat area, whatever face was hit.
	 */
	public static List<BlockPos> areaPositions(BlockPos origin, Direction face, int radius, Player player, boolean horizontalPlane) {
		int count = (1 + radius * 2) * (1 + radius * 2);
		int shift = radius == 0 ? 0 : radius - 1;
		boolean vertical = horizontalPlane || face.getAxis() == Direction.Axis.Y;

		int shiftZ = 0;
		int shiftX = 0;
		if (face.getAxis() == Direction.Axis.Y && Math.abs(origin.getY() - player.getBlockY()) > 5.5) {
			switch (player.getDirection()) {
				case NORTH -> shiftZ = -shift;
				case SOUTH -> shiftZ = shift;
				case EAST -> shiftX = shift;
				case WEST -> shiftX = -shift;
				default -> {
				}
			}
		}

		List<BlockPos> positions = new ArrayList<>(count);
		int x = 0;
		int y = 0;
		for (int i = 0; i < count; i++) {
			if (vertical) {
				positions.add(origin.offset(x + shiftX, 0, y + shiftZ));
			} else if (face.getAxis() == Direction.Axis.X) {
				positions.add(origin.offset(0, x + shift, y));
			} else {
				positions.add(origin.offset(x, y + shift, 0));
			}

			if (Math.abs(x) <= Math.abs(y) && (x != y || x >= 0)) {
				x += y >= 0 ? 1 : -1;
			} else {
				y += x >= 0 ? -1 : 1;
			}
		}
		return positions;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
		if (this.toolType == ToolType.LUMBER) {
			builder.accept(Component.translatable(TheHammerMod.getMode(stack) == 0 ? "tip.thehammermod.fella" : "tip.thehammermod.single").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			int maxSize = 1 + TheHammerMod.getAoeMaxSize(stack) * 2;
			int currentSize = 1 + TheHammerMod.getMode(stack) * 2;
			builder.accept(Component.translatable("tip.thehammermod.aoe", currentSize + "x" + currentSize).withStyle(ChatFormatting.DARK_GRAY));
			builder.accept(Component.translatable("tip.thehammermod.maxaoe", maxSize + "x" + maxSize).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return false;
	}
}
