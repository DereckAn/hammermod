package wallywhip.thehammermod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wallywhip.thehammermod.TheHammerMod;

/** Client -> server: sneak + mouse wheel changed the held tool's mode by delta. The server works out AOE vs lumber mode from the item. */
public record ScrollModePayload(int delta) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScrollModePayload> TYPE = new CustomPacketPayload.Type<>(TheHammerMod.id("scroll_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ScrollModePayload> CODEC =
		ByteBufCodecs.VAR_INT.map(ScrollModePayload::new, ScrollModePayload::delta).cast();

	@Override
	public Type<ScrollModePayload> type() {
		return TYPE;
	}
}
