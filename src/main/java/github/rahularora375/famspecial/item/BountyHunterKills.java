package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.sound.ModSounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

public class BountyHunterKills {
    private static final float DROP_CHANCE = 0.05f;

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, damageSource) -> {
            if (!(victim.getEntityWorld() instanceof ServerWorld serverWorld)) return;
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity attacker)) return;
            if (!Boolean.TRUE.equals(attacker.getEquippedStack(EquipmentSlot.LEGS).get(ModComponents.BOUNTY_HUNTER))) return;
            if (!(damageSource.getSource() instanceof ArrowEntity)) return;
            if (serverWorld.random.nextFloat() >= DROP_CHANCE) return;

            int count = serverWorld.random.nextInt(3) + 1;
            Block.dropStack(serverWorld, victim.getBlockPos(), new ItemStack(Items.DIAMOND, count));
            serverWorld.playSound(null, attacker.getBlockPos(),
                    ModSounds.INDY, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });

        FamSpecial.LOGGER.info("Registering bounty hunter kills for {}", FamSpecial.MOD_ID);
    }
}
