package wallywhip.thehammermod.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wallywhip.thehammermod.config.HammerConfig;

/** Config screen in ModMenu. Only loaded when ModMenu is installed; needs Cloth Config too. */
public class ModMenuCompat implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return FabricLoader.getInstance().isModLoaded("cloth-config") ? ModMenuCompat::createConfigScreen : parent -> null;
	}

	private static Screen createConfigScreen(Screen parent) {
		HammerConfig config = HammerConfig.get();
		ConfigEntryBuilder entries = ConfigEntryBuilder.create();
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("itemGroup.thehammermod.tab"));
		builder.getOrCreateCategory(Component.literal("The Hammer Mod"))
			.addEntry(entries.startColorField(Component.literal("colorPrimaryBreakable"), config.primaryColour)
				.setDefaultValue(HammerConfig.DEFAULT_PRIMARY).setSaveConsumer(value -> config.primaryColour = value).build())
			.addEntry(entries.startColorField(Component.literal("colorSecondaryBreakable"), config.secondaryColour)
				.setDefaultValue(HammerConfig.DEFAULT_SECONDARY).setSaveConsumer(value -> config.secondaryColour = value).build())
			.addEntry(entries.startColorField(Component.literal("colorNonBreakable"), config.nonColour)
				.setDefaultValue(HammerConfig.DEFAULT_NON).setSaveConsumer(value -> config.nonColour = value).build())
			.addEntry(entries.startIntField(Component.literal("chanceMinersDelight"), config.chanceMinersDelight)
				.setDefaultValue(HammerConfig.DEFAULT_MINERS_CHANCE).setSaveConsumer(value -> config.chanceMinersDelight = value).build())
			.addEntry(entries.startBooleanToggle(Component.literal("requiresSeeds"), config.requiresSeeds)
				.setDefaultValue(false).setSaveConsumer(value -> config.requiresSeeds = value).build());
		builder.setSavingRunnable(HammerConfig::save);
		return builder.build();
	}
}
