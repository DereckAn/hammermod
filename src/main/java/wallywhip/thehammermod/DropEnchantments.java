package wallywhip.thehammermod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import wallywhip.thehammermod.items.AoeToolItem;

import static wallywhip.thehammermod.TheHammerMod.enchantmentLevel;

/** Void, Planker, Stripper, Miners' Delight, Melter and Magnet, applied to every block drop a player causes (see BlockMixin). */
public final class DropEnchantments {
	private DropEnchantments() {
	}

	public static List<ItemStack> apply(List<ItemStack> originalDrops, BlockState state, ServerLevel level, ServerPlayer player, ItemInstance tool) {
		if (enchantmentLevel(tool, TheHammerMod.DIGGERS_VOID) != 0) {
			return new ArrayList<>();
		}

		List<ItemStack> drops = new ArrayList<>(originalDrops);
		if (enchantmentLevel(tool, TheHammerMod.LUMBERS_PLANKER) > 0) {
			drops.replaceAll(stack -> stack.is(ItemTags.LOGS) ? TheHammerMod.craftOneByOne(level, stack) : stack);
		}

		if (enchantmentLevel(tool, TheHammerMod.LUMBERS_STRIPPER) > 0) {
			drops.replaceAll(stack -> stack.is(ItemTags.LOGS) ? stripped(stack) : stack);
		}

		// Hammers and paxels roll Miners' Delight once per swing instead (AoeToolItem.breakArea)
		AoeToolItem.ToolType toolType = tool.typeHolder().value() instanceof AoeToolItem aoe ? aoe.getToolType() : null;
		if (toolType != AoeToolItem.ToolType.HAMMER && toolType != AoeToolItem.ToolType.PAXEL) {
			ItemStack ore = TheHammerMod.rollMinersDelight(tool, state);
			if (!ore.isEmpty()) {
				drops.add(ore);
				level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F,
					(level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F);
			}
		}

		if (enchantmentLevel(tool, TheHammerMod.DIGGERS_MELTER) > 0) {
			drops.replaceAll(stack -> TheHammerMod.smelt(level, stack));
		}

		// Sickles handle Magnet themselves after replanting
		if (toolType != AoeToolItem.ToolType.SICKLE && enchantmentLevel(tool, TheHammerMod.DIGGERS_MAGNET) > 0) {
			boolean pickedUp = false;
			for (ItemStack stack : drops) {
				pickedUp |= player.getInventory().add(stack);
			}
			if (pickedUp) {
				TheHammerMod.playPickupSound(level, player);
			}
		}
		return drops;
	}

	/** oak_log -> stripped_oak_log etc. by name, which covers every vanilla log/wood/stem/hyphae. 1.19.2 asked AxeItem, which no longer exists. */
	private static ItemStack stripped(ItemStack log) {
		Identifier id = BuiltInRegistries.ITEM.getKey(log.getItem());
		if (id.getPath().startsWith("stripped_")) {
			return log;
		}
		return BuiltInRegistries.ITEM.getOptional(id.withPrefix("stripped_"))
			.map(strippedItem -> log.transmuteCopy(strippedItem))
			.orElse(log);
	}
}
