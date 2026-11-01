package wallywhip.thehammermod;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallywhip.thehammermod.config.HammerConfig;
import wallywhip.thehammermod.items.AoeToolItem;
import wallywhip.thehammermod.network.ScrollModePayload;

public class TheHammerMod implements ModInitializer {
	public static final String MOD_ID = "thehammermod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<Enchantment> DIGGERS_AOE = enchantment("diggers_aoe");
	public static final ResourceKey<Enchantment> DIGGERS_MAGNET = enchantment("diggers_magnet");
	public static final ResourceKey<Enchantment> DIGGERS_MELTER = enchantment("diggers_melter");
	public static final ResourceKey<Enchantment> DIGGERS_VOID = enchantment("diggers_void");
	public static final ResourceKey<Enchantment> LUMBERS_PLANKER = enchantment("lumbers_planker");
	public static final ResourceKey<Enchantment> LUMBERS_STRIPPER = enchantment("lumbers_stripper");
	public static final ResourceKey<Enchantment> MINERS_DELIGHT = enchantment("miners_delight");

	public static final TagKey<Block> MUSHROOM_STEMS_TAG = TagKey.create(Registries.BLOCK, id("mushroom_stems"));
	public static final TagKey<Item> OVERWORLD_ORES_TAG = TagKey.create(Registries.ITEM, id("overworld_ores"));
	public static final TagKey<Item> DEEPSLATE_ORES_TAG = TagKey.create(Registries.ITEM, id("deepslate_ores"));
	public static final TagKey<Item> NETHER_ORES_TAG = TagKey.create(Registries.ITEM, id("nether_ores"));
	public static final TagKey<Item> END_ORES_TAG = TagKey.create(Registries.ITEM, id("end_ores"));

	/** AOE radius (0 = 1x1, 1 = 3x3, ...) for hammers/excavators/sickles/paxels; 0 = fell tree, 1 = single block for lumber axes. Was the "Mode" NBT int. */
	public static final DataComponentType<Integer> MODE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		id("mode"),
		DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()
	);

	@Override
	public void onInitialize() {
		ModItems.register();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("tab"), FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.thehammermod.tab"))
			.icon(() -> new ItemStack(ModItems.COPPER_HAMMER))
			.displayItems((parameters, output) -> {
				ModItems.ALL.forEach(output::accept);
				parameters.holders().lookup(Registries.ENCHANTMENT).ifPresent(enchantments -> enchantments.listElements()
					.filter(enchantment -> enchantment.key().identifier().getNamespace().equals(MOD_ID))
					.forEach(enchantment -> output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, enchantment.value().getMaxLevel())))));
			})
			.build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> ModItems.ALL.forEach(output::accept));

		PayloadTypeRegistry.serverboundPlay().register(ScrollModePayload.TYPE, ScrollModePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ScrollModePayload.TYPE, (payload, context) -> onScrollMode(context.player(), payload.delta()));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static ResourceKey<Enchantment> enchantment(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, id(name));
	}

	/** Reads an enchantment level straight from the item's components, so it works on both sides without a registry lookup. */
	public static int enchantmentLevel(DataComponentGetter item, ResourceKey<Enchantment> key) {
		ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (var entry : enchantments.entrySet()) {
			if (entry.getKey().is(key)) {
				return entry.getIntValue();
			}
		}
		return 0;
	}

	public static int getAoeMaxSize(ItemStack stack) {
		return enchantmentLevel(stack, DIGGERS_AOE) + 1;
	}

	public static int getMode(ItemStack stack) {
		return stack.getOrDefault(MODE, 0);
	}

	private static void onScrollMode(ServerPlayer player, int delta) {
		ItemStack handStack = player.getMainHandItem();
		if (!(handStack.getItem() instanceof AoeToolItem tool)) {
			return;
		}

		boolean lumber = tool.getToolType() == AoeToolItem.ToolType.LUMBER;
		int max = lumber ? 1 : getAoeMaxSize(handStack);
		int mode = getMode(handStack) + delta;
		if (mode < 0) {
			mode = max;
		}
		if (mode > max) {
			mode = 0;
		}
		handStack.set(MODE, mode);

		Component message;
		if (lumber) {
			message = Component.translatable(mode == 1 ? "tip.thehammermod.single" : "tip.thehammermod.fella");
		} else {
			int aoeSize = 1 + mode * 2;
			message = Component.translatable("tip.thehammermod.aoe", aoeSize + "x" + aoeSize);
		}
		player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.GREEN));
	}

	/** Breaks one extra block of an AOE as if the player mined it, without damaging the tool (callers do that). Mirrors ServerPlayerGameMode.destroyBlock. */
	public static boolean breakExtraBlock(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState state) {
		if (!player.getMainHandItem().canDestroyBlock(state, level, pos, player)) {
			return false;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		Block block = state.getBlock();
		if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) {
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
			return false;
		}

		if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
			return false;
		}

		BlockState adjustedState = block.playerWillDestroy(level, pos, state, player);
		boolean removed = level.removeBlock(pos, false);
		if (removed) {
			block.destroy(level, pos, adjustedState);
		}

		if (player.preventsBlockDrops()) {
			return true;
		}

		ItemStack destroyedWith = player.getMainHandItem().copy();
		if (removed && player.hasCorrectToolForDrops(adjustedState)) {
			block.playerDestroy(level, player, pos, adjustedState, blockEntity, destroyedWith);
		}
		return true;
	}

	/** Miners' Delight: a configurable chance that mining stone/deepslate/netherrack/end stone also yields a random ore from the matching tag. */
	public static ItemStack rollMinersDelight(DataComponentGetter tool, BlockState state) {
		if (enchantmentLevel(tool, MINERS_DELIGHT) == 0) {
			return ItemStack.EMPTY;
		}

		RandomSource random = RandomSource.create();
		if (random.nextInt(100) >= HammerConfig.get().chanceMinersDelight) {
			return ItemStack.EMPTY;
		}

		TagKey<Item> oreTag;
		if (state.is(Blocks.STONE)) {
			oreTag = OVERWORLD_ORES_TAG;
		} else if (state.is(Blocks.DEEPSLATE)) {
			oreTag = DEEPSLATE_ORES_TAG;
		} else if (state.is(Blocks.NETHERRACK)) {
			oreTag = NETHER_ORES_TAG;
		} else if (state.is(Blocks.END_STONE)) {
			oreTag = END_ORES_TAG;
		} else {
			return ItemStack.EMPTY;
		}

		Optional<HolderSet.Named<Item>> ores = BuiltInRegistries.ITEM.get(oreTag);
		if (ores.isEmpty() || ores.get().size() == 0) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(ores.get().get(random.nextInt(ores.get().size())));
	}

	/** Hammers and paxels roll Miners' Delight once per swing (not once per AOE block); other tools roll it per block in BlockMixin. */
	public static void doMinersDelight(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, ServerPlayer player) {
		if (enchantmentLevel(tool, DIGGERS_VOID) != 0) {
			return;
		}

		ItemStack ore = rollMinersDelight(tool, state);
		if (ore.isEmpty()) {
			return;
		}

		// ponytail: inverted on purpose, 1.19.2 did the same (no Melter -> smelted bonus ore)
		if (enchantmentLevel(tool, DIGGERS_MELTER) == 0) {
			ore = smelt(level, ore);
		}

		if (enchantmentLevel(tool, DIGGERS_MAGNET) > 0 && player.getInventory().add(ore)) {
			playPickupSound(level, player);
		}

		if (!ore.isEmpty()) {
			level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F,
				(level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F);
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), ore);
		}
	}

	public static void playPickupSound(ServerLevel level, ServerPlayer player) {
		level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
			((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
	}

	/** Result of putting the stack alone in a 1x1 crafting grid (log -> planks), scaled by stack size, or the stack itself if no recipe. */
	public static ItemStack craftOneByOne(ServerLevel level, ItemStack stack) {
		CraftingInput input = CraftingInput.of(1, 1, List.of(stack.copyWithCount(1)));
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(recipe -> scaled(recipe.value().assemble(input), stack.getCount()))
			.orElse(stack);
	}

	public static ItemStack smelt(ServerLevel level, ItemStack stack) {
		SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
		return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
			.map(recipe -> scaled(recipe.value().assemble(input), stack.getCount()))
			.orElse(stack);
	}

	private static ItemStack scaled(ItemStack result, int times) {
		return result.copyWithCount(result.getCount() * times);
	}
}
