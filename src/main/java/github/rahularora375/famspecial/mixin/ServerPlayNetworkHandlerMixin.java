package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.item.ArmorEffects;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Instant re-dispatch of the full ArmorEffects bonus pass on hotbar slot
// select. Counterpart to LivingEntityEquipMixin (which handles armor slot
// changes). Scrolling the hotbar or pressing a number key is the common
// case for mainhand-driven effects and must flip both HUD icons and
// gameplay gates without waiting for the 80-tick refresh.
//
// Design note: rather than hand-rolling per-effect strip checks, we call
// ArmorEffects.refreshBonusesFor(player) — the same method the 80-tick
// tick loop runs. This handles BOTH directions in one call:
//   - APPLY: if the new mainhand satisfies a bonus trigger that previously
//     failed, the effect is added immediately (no 4-second swap-in delay
//     for effect-gated gameplay — Emperor's Divide, Asgardian's Flight
//     mainhand half).
//   - STRIP: if the new mainhand no longer satisfies a previously-active
//     bonus, the MOD_MANAGED diff in refreshBonusesFor removes the effect
//     on the spot.
//
// The 80-tick tick loop remains as the eventual-consistency safety net.
// TAIL-inject ensures the new selectedSlot has been assigned and any
// invalid-slot rejection has run, so getMainHandStack() returns the stack
// the player is now actually holding.
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("TAIL"))
    private void famspecial$refreshBonusesOnSlotSwap(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        if (this.player == null) return;
        ArmorEffects.refreshBonusesFor(this.player);
    }
}
