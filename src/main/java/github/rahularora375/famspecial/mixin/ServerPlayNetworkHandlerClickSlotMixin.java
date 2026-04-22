package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.item.ArmorEffects;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Instant re-dispatch of the full ArmorEffects bonus pass on any inventory
// mutation packet the player can send while a screen handler is open.
// Complements:
//   - ServerPlayNetworkHandlerMixin (hotbar slot-select)
//   - LivingEntityEquipMixin (polling-based fallback via onEquipStack)
//
// Why this exists: vanilla only calls LivingEntity#onEquipStack during the
// per-tick equipment-diff poll, so inventory paths that mutate slots via
// ScreenHandler leave a 1-tick gap between the mutation and our bonus
// refresh. That gap is invisible on the tooltip (client reads the ItemStack
// component directly) and invisible on gameplay (server status-effect runs
// on first damage tick after the poll), but it is perceptible on the HUD
// icon because packet + render buffering add up. Hooking the packet handler
// TAIL closes the gap — same-tick detection, matching the already-instant
// onUpdateSelectedSlot path.
//
// Two packet handlers cover the inventory paths:
//   - onClickSlot: survival / standard inventory — drag, shift-click,
//     armor-swap-key, creative-hotbar shuffle. Routes through
//     screenHandler.onSlotClick, which for ArmorSlot triggers
//     onEquipStack synchronously inside the slot's setStack override.
//   - onCreativeInventoryAction: creative tab drag-into-armor-slot.
//     Writes to playerScreenHandler.getSlot(...).setStack(...) directly
//     via a separate packet type that does NOT route through
//     onClickSlot. Without this hook, creative-mode armor equip leaves
//     the HUD icon up to ~4 seconds late (only the 80-tick slow path
//     catches it).
//
// LivingEntityEquipMixin stays registered as the fallback for paths that
// don't route through either packet (dispenser equip, /item replace,
// ItemStackMixin broken-piece re-equip, etc.).
//
// TAIL ensures the slot mutation inside the screen handler has already
// applied and the packet has been validated.
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerClickSlotMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", at = @At("TAIL"))
    private void famspecial$refreshBonusesOnSlotClick(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (this.player == null) return;
        ArmorEffects.refreshBonusesFor(this.player);
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("TAIL"))
    private void famspecial$refreshBonusesOnCreativeInventoryAction(
            CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        if (this.player == null) return;
        ArmorEffects.refreshBonusesFor(this.player);
    }
}
