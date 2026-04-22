package github.rahularora375.famspecial.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Widens access to MobEntity.targetSelector so NecromancerSummon can swap
// out the vanilla ActiveTargetGoal<PlayerEntity>/<VillagerEntity>/etc.
// entries on a summoned zombie with a single predicate-gated goal bound
// to the original attacker's UUID. Otherwise the summoned zombie's AI
// would re-scan every tick and retarget the summoner (the nearest player).
@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("targetSelector")
    GoalSelector famspecial$getTargetSelector();
}
