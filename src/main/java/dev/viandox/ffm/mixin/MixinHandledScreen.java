package dev.viandox.ffm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.viandox.ffm.Config;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

@Mixin(HandledScreen.class)
public class MixinHandledScreen {
    private static int maxStep = 100;
    private static float stepSize = Config.ToolTipLerpStepSize;
    private static float maxSquaredDistance = (float) Math.pow(Config.ToolTipMaxLerpDistance, 2);
    protected float steps = 0;
    protected Vector3f oldState;
    protected Vector3f newState;
    @Shadow
    int x;
    @Shadow
    int y;
    @Shadow
    Slot focusedSlot;

    @Redirect(
        method = "drawMouseoverTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V"
        )
    )
    void redirectRenderTooltip(HandledScreen<?> hs, MatrixStack matrices, ItemStack stack, int x, int y) {

        try {
            int sx = this.focusedSlot.x + this.x + 10;
            int sy = this.focusedSlot.y + this.y + 17;
            Vector3f requestedState = new Vector3f((float) sx, (float) sy, 0.0f);
            if(this.oldState == null) {
                this.oldState = requestedState;
                this.newState = requestedState;
                this.steps = maxStep;
            } else {
                Vector3f currentState = this.oldState.copy();
                currentState.lerp(this.newState, this.steps / maxStep);
                if(!requestedState.equals(this.newState)) {
                    Vector3f lengthVector = requestedState.copy();
                    lengthVector.subtract(this.oldState);
                    float distance = lengthVector.getX() * lengthVector.getX() + lengthVector.getY() * lengthVector.getY();
                    this.oldState = distance > maxSquaredDistance ? requestedState : currentState;
                    this.newState = requestedState;  
                    this.steps = distance > maxSquaredDistance ? maxStep : 0;
                }
                if(this.steps < maxStep) {
                    this.steps = this.steps > maxStep ? maxStep : this.steps + stepSize;
                    sx = (int) currentState.getX();
                    sy = (int) currentState.getY();
                    if(steps >= maxStep) {
                        this.oldState = requestedState;
                        this.newState = requestedState;
                    }
                }
            }
            // TODO fix opacity to reset when the tooltip is not rendered from here
            //GlobalData.TooltipOpactity = this.steps / maxStep;
            hs.renderTooltip(matrices, hs.getTooltipFromItem(stack), sx + (int) Config.ToolTipMarginSize / 2 -1, sy);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}