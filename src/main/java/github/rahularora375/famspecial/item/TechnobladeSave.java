package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

// Technoblade Never Dies — Raider's Legacy 4/4 totem-of-undying auto-save.
// The TECHNOBLADE_NEVER_DIES status effect is applied + ticked by
// ArmorEffects while the full set is worn; this hook is the totem-save half
// of the 4/4 payoff (the fall-damage immunity half is bound to the sibling
// TECHNOBLADE_FEATHERWEIGHT effect in LivingEntityMixin). Layered on top of
// the effect aura: when a hit would have killed the wearer, consume a
// 5-minute per-set cooldown and revive them with the exact vanilla-totem
// effect bundle + visual + sound, à la ItemStack.UseAction.TOTEM. Mirrors
// the static-utility shape of ThorEffects / NecromancerSummon — no instance,
// no register() (the mixin calls in directly from modifyAppliedDamage's
// RETURN injector).
public final class TechnobladeSave {
    private static final long COOLDOWN_TICKS = 6000L; // 5 minutes at 20 tps
    private static final String SET_ID = "raider";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private TechnobladeSave() {}

    // Attempt to consume the save. Returns true iff the cooldown was clear,
    // the wearer carried TECHNOBLADE_NEVER_DIES, and the save fired — in
    // which case the caller (LivingEntityMixin) is expected to clamp the
    // damage so the wearer survives at 1 HP. Returns false in all other
    // cases (not on a server world, effect missing, cooldown active).
    public static boolean tryConsume(PlayerEntity player, DamageSource source) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        if (!player.hasStatusEffect(ModStatusEffects.TECHNOBLADE_NEVER_DIES)) return false;
        if (isOnCooldown(player, world.getTime())) return false;

        // Vanilla totem-of-undying bundle: Regen II 45s, Fire Resistance 40s,
        // Absorption II 5s. Durations match Items.TOTEM_OF_UNDYING's onConsume
        // (PlayerEntity#useRiptide path) so the save feels indistinguishable
        // from a real totem proc.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));

        // Clamp to 1 HP so post-save state matches a real totem revive (and
        // so the caller's "return totalHP - 1.0f" doesn't underflow against
        // a fractional getHealth from prior damage math).
        player.setHealth(1.0f);

        // Stamp the cooldown on every worn Raider piece. Mirrors
        // NecromancerSummon.stampCooldownOnWornSet — server reads the stamp
        // back via isOnCooldown, and the client tooltip renders MM:SS off
        // the same component.
        long cooldownEndTick = world.getTime() + COOLDOWN_TICKS;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (SET_ID.equals(stack.get(ModComponents.SET_ID))) {
                stack.set(ModComponents.TECHNOBLADE_TOTEM_COOLDOWN_END, cooldownEndTick);
            }
        }

        // Client FX: USE_TOTEM_OF_UNDYING entity status replicates the
        // gold flash + heart particle burst to every client tracking the
        // player. Totem sound plays through the WORLD via a null-source
        // playSound so every nearby player hears it (matches vanilla
        // PlayerEntity#tryUseTotem, which emits the same sound to all
        // tracked clients via the entity-status packet's side effect).
        world.sendEntityStatus(player, EntityStatuses.USE_TOTEM_OF_UNDYING);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return true;
    }

    // Read-only cooldown probe: scan all 4 armor slots for Raider pieces and
    // return the max-stamped cooldown-end tick. Returns false when no piece
    // carries a stamp (e.g. fresh set never triggered) or when every stamp
    // has elapsed. Matches the per-stack pattern in
    // NecromancerSummon.isOnCooldown (which uses a per-UUID map) — here the
    // stamp itself is the source of truth, so we read it back rather than
    // tracking a parallel map.
    public static boolean isOnCooldown(PlayerEntity player, long worldTime) {
        long max = 0L;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!SET_ID.equals(stack.get(ModComponents.SET_ID))) continue;
            Long stamped = stack.get(ModComponents.TECHNOBLADE_TOTEM_COOLDOWN_END);
            if (stamped != null && stamped > max) max = stamped;
        }
        return max > worldTime;
    }
}
