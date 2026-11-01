package wallywhip.thehammermod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wallywhip.thehammermod.TheHammerMod;
import wallywhip.thehammermod.config.HammerConfig;
import wallywhip.thehammermod.items.AoeToolItem;
import wallywhip.thehammermod.items.LumberAxeItem;
import wallywhip.thehammermod.items.SickleItem;

/**
 * Coloured outline around every block the held tool would mine, plus the crack overlay on the extra blocks. Replaces the 1.19.2
 * WorldRenderer.drawBlockOutline mixin: the area is computed while the frame is extracted and drawn in Fabric's BEFORE_BLOCK_OUTLINE event.
 */
public final class AoeHighlight {
	/** Set while sneak-scrolling, so the full area stays visible although sneaking normally means "single block". */
	public static boolean isScrolling = false;

	private record Highlight(BlockPos origin, List<BlockPos> positions, List<VoxelShape> shapes, int argb) {
	}

	private static Highlight current;

	private AoeHighlight() {
	}

	/** LevelRenderEvents.END_EXTRACTION: runs after vanilla extracted the outline and the crack overlays. */
	public static void extract(LevelExtractionContext context) {
		current = null;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		BlockOutlineRenderState outline = context.levelState().blockOutlineRenderState;
		if (player == null || outline == null || !(minecraft.hitResult instanceof BlockHitResult hit)) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		if (!stack.is(ItemTags.MINING_ENCHANTABLE)) { // was: any MiningToolItem or ShearsItem
			return;
		}
		if (isScrolling) {
			isScrolling = player.isShiftKeyDown();
		}

		ClientLevel level = context.level();
		BlockPos origin = outline.pos();
		List<BlockPos> positions = new ArrayList<>();
		positions.add(origin);

		HammerConfig config = HammerConfig.get();
		int colour;
		if (new AreaCollector(level, player, stack, origin, positions).collect(hit)) {
			colour = config.primaryColour;
		} else if (stack.getItem() instanceof AoeToolItem tool && tool.canTransformBlock(level, hit, player)) {
			colour = config.secondaryColour;
		} else {
			colour = config.nonColour;
		}

		CollisionContext collision = CollisionContext.of(player);
		List<VoxelShape> shapes = new ArrayList<>(positions.size());
		for (BlockPos pos : positions) {
			shapes.add(level.getBlockState(pos).getShape(level, pos, collision));
		}
		current = new Highlight(origin, positions, shapes, 0xFF000000 | colour);

		SortedSet<BlockDestructionProgress> progress = level.destructionProgress().get(origin.asLong());
		if (progress != null && !progress.isEmpty()) {
			int stage = progress.last().getProgress();
			for (BlockPos pos : positions.subList(1, positions.size())) {
				context.levelState().blockBreakingRenderStates.add(new BlockBreakingRenderState(pos, level.getBlockState(pos), stage));
			}
		}
	}

	/** LevelRenderEvents.BEFORE_BLOCK_OUTLINE: returns false to replace the vanilla outline. */
	public static boolean render(LevelRenderContext context, BlockOutlineRenderState outline) {
		Highlight highlight = current;
		if (highlight == null || !highlight.origin().equals(outline.pos())) {
			return true;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		float lineWidth = context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth;
		RenderType renderType = context.gameRenderer().useImprovedTransparency() ? RenderTypes.linesTranslucentNoDepthWrite() : RenderTypes.linesTranslucent();
		for (int i = 0; i < highlight.positions().size(); i++) {
			BlockPos pos = highlight.positions().get(i);
			poseStack.pushPose();
			poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			context.submitNodeCollector().submitShapeOutline(poseStack, highlight.shapes().get(i), renderType, highlight.argb(), lineWidth, outline.isTranslucent());
			poseStack.popPose();
		}
		return false;
	}

	/** Client-side mirror of what the tool will break, including running out of durability part way. Port of 1.19.2 renderBlockHighlight. */
	private static final class AreaCollector {
		private final ClientLevel level;
		private final LocalPlayer player;
		private final ItemStack stack;
		private final BlockPos origin;
		private final BlockState originState;
		private final List<BlockPos> positions;
		private final AoeToolItem.ToolType toolType;
		private int remainingDurability;
		private int plantKind;

		AreaCollector(ClientLevel level, LocalPlayer player, ItemStack stack, BlockPos origin, List<BlockPos> positions) {
			this.level = level;
			this.player = player;
			this.stack = stack;
			this.origin = origin;
			this.originState = level.getBlockState(origin);
			this.positions = positions;
			this.toolType = stack.getItem() instanceof AoeToolItem tool ? tool.getToolType() : null;
			this.remainingDurability = player.isCreative() ? stack.getMaxDamage() : stack.getMaxDamage() - stack.getDamageValue();
		}

		/** True if the clicked block can be mined; adds the other blocks that would go with it. */
		boolean collect(BlockHitResult hit) {
			float originalHardness = this.originState.getDestroySpeed(this.level, this.origin);
			if (originalHardness < 0.0F) {
				return false;
			}

			if (this.toolType == AoeToolItem.ToolType.SICKLE) {
				this.plantKind = SickleItem.plantKind(this.originState);
				if (this.plantKind < 0) {
					return false;
				}
			}

			if (this.toolType == AoeToolItem.ToolType.LUMBER) {
				return this.collectTreeColumn(originalHardness);
			}

			int radius = !this.player.isShiftKeyDown() || isScrolling ? TheHammerMod.getMode(this.stack) : 0;
			List<BlockPos> area = AoeToolItem.areaPositions(this.origin, hit.getDirection(), radius, this.player, this.plantKind > 0);
			int originIndex = area.indexOf(this.origin);
			if (!this.tryAdd(this.origin, originalHardness, false)) {
				return false;
			}
			for (int i = 1; i < area.size(); i++) {
				this.tryAdd(area.get((originIndex + i) % area.size()), originalHardness, true);
			}
			return true;
		}

		private boolean collectTreeColumn(float originalHardness) {
			int cost = this.costOf(originalHardness);
			if (this.remainingDurability - cost < 0) {
				return false;
			}
			this.remainingDurability -= cost;

			if (!this.player.isShiftKeyDown() && TheHammerMod.getMode(this.stack) != 1 && LumberAxeItem.isTreeTrunk(this.originState)) {
				for (BlockPos pos = this.origin.above(); this.level.getBlockState(pos).is(this.originState.getBlock()) && this.remainingDurability - cost >= 0; pos = pos.above()) {
					this.remainingDurability -= cost;
					this.positions.add(pos);
				}
			}
			return this.stack.isCorrectToolForDrops(this.originState);
		}

		private boolean tryAdd(BlockPos pos, float originalHardness, boolean add) {
			if (this.remainingDurability == 0 && originalHardness != 0.0F) {
				return false;
			}

			BlockState state = this.level.getBlockState(pos);
			boolean breakable = true;
			if (this.plantKind > 0) {
				breakable = this.plantKind == SickleItem.plantKind(state);
			} else if (this.stack.getItem() instanceof AoeToolItem tool) {
				breakable = tool.canBreakBlock(this.stack, this.level, pos, state, this.originState, originalHardness);
			}

			if (breakable) {
				int cost = this.costOf(state.getDestroySpeed(this.level, pos));
				breakable = this.remainingDurability - cost >= 0;
				if (breakable) {
					this.remainingDurability -= cost;
					if (add) {
						this.positions.add(pos);
					}
				}
			}
			return breakable;
		}

		private int costOf(float hardness) {
			return !this.player.isCreative() && hardness > 0.0F ? 1 : 0;
		}
	}
}
