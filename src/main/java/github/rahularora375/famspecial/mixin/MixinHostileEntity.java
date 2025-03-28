package github.rahularora375.famspecial.mixin;

import github.rahularora375.famspecial.effect.ModEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(HostileEntity.class)
public abstract class MixinHostileEntity extends PathAwareEntity implements Monster {

    private static final Predicate<LivingEntity> GURU_JI_FILTER = entity -> entity.hasStatusEffect(ModEffects.GURU_JI_BLESSING);

    protected MixinHostileEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(EntityType<? extends HostileEntity> entityType, World world, CallbackInfo ci) {
        this.goalSelector.add(1, new FleeEntityGoal(this, PlayerEntity.class, 8.0F, 1.2, 1.4, GURU_JI_FILTER));
    }
}