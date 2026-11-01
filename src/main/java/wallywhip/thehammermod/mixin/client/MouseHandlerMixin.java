package wallywhip.thehammermod.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wallywhip.thehammermod.client.AoeHighlight;
import wallywhip.thehammermod.items.AoeToolItem;
import wallywhip.thehammermod.network.ScrollModePayload;

/** Sneak + mouse wheel while holding a Hammer Mod tool changes its AOE size / lumber mode instead of the hotbar slot. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private double thehammermod$accumulatedScroll;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void thehammermod$scrollMode(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;
		if (handle != this.minecraft.getWindow().handle() || this.minecraft.gui.overlay() != null || this.minecraft.gui.screen() != null
			|| player == null || !player.isShiftKeyDown() || !(player.getMainHandItem().getItem() instanceof AoeToolItem)) {
			return;
		}
		ci.cancel();

		double scroll = (this.minecraft.options.discreteMouseScroll().get() ? Math.signum(yoffset) : yoffset) * this.minecraft.options.mouseWheelSensitivity().get();
		if (this.thehammermod$accumulatedScroll != 0.0 && Math.signum(scroll) != Math.signum(this.thehammermod$accumulatedScroll)) {
			this.thehammermod$accumulatedScroll = 0.0;
		}
		this.thehammermod$accumulatedScroll += scroll;
		int steps = (int) this.thehammermod$accumulatedScroll;
		if (steps == 0) {
			return;
		}
		this.thehammermod$accumulatedScroll -= steps;

		AoeHighlight.isScrolling = true;
		player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
		ClientPlayNetworking.send(new ScrollModePayload(steps));
	}
}
