package wallywhip.thehammermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import wallywhip.thehammermod.TheHammerMod;

/** config/thehammermod.json, same keys as 1.19.2 so an old file still loads. Editable in-game through ModMenu + Cloth Config. */
public final class HammerConfig {
	public static final int DEFAULT_PRIMARY = 0x00FF00;
	public static final int DEFAULT_SECONDARY = 0xFF8B3D;
	public static final int DEFAULT_NON = 0x990501;
	public static final int DEFAULT_MINERS_CHANCE = 5;

	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("thehammermod.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HammerConfig instance;

	/** Outline colour (RGB) when the block(s) can be mined. */
	public int primaryColour = DEFAULT_PRIMARY;
	/** Outline colour when the tool can't mine it but can right-click transform it (strip, till). */
	public int secondaryColour = DEFAULT_SECONDARY;
	/** Outline colour when neither. */
	public int nonColour = DEFAULT_NON;
	/** Percent chance per mined stone-like block for Miners' Delight. */
	public int chanceMinersDelight = DEFAULT_MINERS_CHANCE;
	/** Sickle replanting consumes a seed. */
	public boolean requiresSeeds = false;

	public static synchronized HammerConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static HammerConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				HammerConfig config = GSON.fromJson(reader, HammerConfig.class);
				if (config != null) {
					return config;
				}
			} catch (IOException | JsonParseException e) {
				TheHammerMod.LOGGER.error("Unable to read config file, restoring defaults!", e);
			}
		}
		instance = new HammerConfig();
		save();
		return instance;
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(PATH)) {
			GSON.toJson(get(), writer);
		} catch (IOException e) {
			TheHammerMod.LOGGER.error("Unable to write config file", e);
		}
	}
}
