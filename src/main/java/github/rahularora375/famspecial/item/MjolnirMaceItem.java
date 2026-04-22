package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MjolnirMaceItem extends MaceItem {
    private static final int RIPTIDE_SPIN_TICKS = 20;
    private static final int CHARGE_TICKS = 10;

    public MjolnirMaceItem(Settings settings) { super(settings); }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (hand != Hand.MAIN_HAND
                || !Boolean.TRUE.equals(stack.get(ModComponents.THOR_MACE))
                || !player.hasStatusEffect(ModStatusEffects.ASGARDIANS_FLIGHT)) {
            return super.use(world, player, hand);
        }
        player.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModComponents.THOR_MACE))
                ? UseAction.SPEAR
                : super.getUseAction(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Boolean.TRUE.equals(stack.get(ModComponents.THOR_MACE))
                ? 72000
                : super.getMaxUseTime(stack, user);
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)
                || !Boolean.TRUE.equals(stack.get(ModComponents.THOR_MACE))
                || !player.hasStatusEffect(ModStatusEffects.ASGARDIANS_FLIGHT)) {
            return super.onStoppedUsing(stack, world, user, remainingUseTicks);
        }
        int held = getMaxUseTime(stack, user) - remainingUseTicks;
        if (held < CHARGE_TICKS) return super.onStoppedUsing(stack, world, user, remainingUseTicks);

        // Vanilla TridentItem#onStoppedUsing riptide branch — byte-for-byte,
        // runs on BOTH client and server. useRiptide/addVelocity/move all
        // need to run client-side too because the LocalPlayerEntity is
        // authoritative on its own velocity; a server-only addVelocity
        // fires velocityDirty → EntityVelocityUpdateS2CPacket which the
        // local player ignores, producing spin-without-launch.
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float j = -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        float k = -MathHelper.sin(pitch * (float) (Math.PI / 180.0));
        float l = MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
        float m = MathHelper.sqrt(j * j + k * k + l * l);
        float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        j *= f / m;
        k *= f / m;
        l *= f / m;
        player.addVelocity(j, k, l);
        player.useRiptide(RIPTIDE_SPIN_TICKS, 8.0F, stack);
        if (player.isOnGround()) {
            player.move(MovementType.SELF, new Vec3d(0.0, 1.1999999F, 0.0));
        }
        world.playSoundFromEntity(null, player, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        // Server-only: prevent spam-riptide by arming a cooldown equal to the
        // spin duration. Client cooldown mirrors via packet on next tick.
        if (!world.isClient()) {
            player.getItemCooldownManager().set(stack, RIPTIDE_SPIN_TICKS);
        }
        return true;
    }
}
