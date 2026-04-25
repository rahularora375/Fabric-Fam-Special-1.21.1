package github.rahularora375.famspecial.entity;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class VolleyArrowEntity extends ArrowEntity {
    @Nullable
    private ItemStack weaponStack;
    private byte cachedPierceLevel;

    public VolleyArrowEntity(EntityType<? extends VolleyArrowEntity> entityType, World world) {
        super(entityType, world);
    }

    public VolleyArrowEntity(World world, LivingEntity owner, ItemStack arrowStack, @Nullable ItemStack crossbowStack) {
        this(ModEntities.VOLLEY_ARROW, world);
        this.setStack(arrowStack.copy());
        this.copyComponentsFrom(arrowStack);
        Unit unit = arrowStack.remove(DataComponentTypes.INTANGIBLE_PROJECTILE);
        if (unit != null) {
            this.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        }
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1F, owner.getZ());
        if (crossbowStack != null && world instanceof ServerWorld serverWorld) {
            if (crossbowStack.isEmpty()) {
                throw new IllegalArgumentException("Invalid weapon firing an arrow");
            }
            this.weaponStack = crossbowStack.copy();
            int i = EnchantmentHelper.getProjectilePiercing(serverWorld, crossbowStack, this.getItemStack());
            if (i > 0) {
                this.cachedPierceLevel = (byte) i;
            }
        }
        this.setOwner(owner);
    }

    @Override
    @Nullable
    public ItemStack getWeaponStack() {
        return this.weaponStack;
    }

    @Override
    public byte getPierceLevel() {
        return this.cachedPierceLevel;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity living) {
            living.timeUntilRegen = 0;
        }
        super.onEntityHit(entityHitResult);
    }
}
