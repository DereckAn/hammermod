package wallywhip.thehammermod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import wallywhip.thehammermod.items.AoeToolItem;
import wallywhip.thehammermod.items.LumberAxeItem;
import wallywhip.thehammermod.items.PaxelItem;
import wallywhip.thehammermod.items.SickleItem;

public final class ModItems {
	/** 1.19.2 materialCopperTool: iron mining level, 759 durability, speed 7, +2 damage, enchantability 8, repaired with copper ingots. */
	public static final ToolMaterial COPPER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 759, 7.0F, 2.0F, 8, ItemTags.COPPER_TOOL_MATERIALS);
	public static final TagKey<Block> MINEABLE_WITH_PAXEL = TagKey.create(Registries.BLOCK, TheHammerMod.id("mineable/paxel"));

	/** Registration order = creative tab order. */
	public static final List<Item> ALL = new ArrayList<>();

	public static Item COPPER_HAMMER;

	private ModItems() {
	}

	public static void register() {
		String[] names = {"wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"};
		ToolMaterial[] materials = {ToolMaterial.WOOD, ToolMaterial.STONE, ToolMaterial.IRON, ToolMaterial.GOLD, ToolMaterial.DIAMOND, ToolMaterial.NETHERITE, COPPER};
		// Sickle and paxel attack damage/speed per material, as in 1.19.2 (same order as above)
		float[] sickleDamage = {0, -1, -2, 0, -3, -4, 0};
		float[] sickleSpeed = {-3, -2, -1, -3, 0, 0, -3};

		for (int i = 0; i < names.length; i++) {
			COPPER_HAMMER = register( // copper is last, so this ends up holding the copper hammer
"hammer_" + names[i], p -> new AoeToolItem(AoeToolItem.ToolType.HAMMER, p), new Item.Properties().pickaxe(materials[i], 1.0F, -2.8F));
		}
		for (int i = 0; i < names.length; i++) {
			register("excavator_" + names[i], p -> new AoeToolItem(AoeToolItem.ToolType.EXCAVATOR, p), new Item.Properties().shovel(materials[i], 1.5F, -3.0F));
		}
		for (int i = 0; i < names.length; i++) {
			register("lumber_" + names[i], LumberAxeItem::new, new Item.Properties().axe(materials[i], 1.0F, -2.8F));
		}
		for (String name : names) {
			register("handle_" + name, Item::new, new Item.Properties());
		}
		for (int i = 0; i < names.length; i++) {
			register("sickle_" + names[i], SickleItem::new, new Item.Properties().hoe(materials[i], sickleDamage[i], sickleSpeed[i]));
		}
		for (int i = 0; i < names.length; i++) {
			register("paxel_" + names[i], PaxelItem::new, new Item.Properties().tool(materials[i], MINEABLE_WITH_PAXEL, sickleDamage[i], sickleSpeed[i], 0.0F));
		}
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TheHammerMod.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ALL.add(item);
		return item;
	}
}
