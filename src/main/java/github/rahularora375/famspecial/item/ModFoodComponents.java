package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.effect.ModEffects;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents {
    public static final FoodComponent PUNJABI_LASSI = new FoodComponent.Builder()
            .nutrition(12)
            .saturationModifier(1.8F)
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 12000, 1), 1.0f)
            .statusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 19), 1.0f)
            .build();

    public static final FoodComponent RADHE_RADHE_GURU_JI = new FoodComponent.Builder()
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 600, 2), 1.0f)
            .statusEffect(new StatusEffectInstance(ModEffects.GURU_JI_BLESSING, 600, 0), 1.0f)
            .build();
}