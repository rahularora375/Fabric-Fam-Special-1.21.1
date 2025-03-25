package github.rahularora375.famspecial.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents {
    public static final FoodComponent PUNJABI_LASSI = new FoodComponent.Builder()
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 6000, 1), 1.0f)
            .statusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 19), 1.0f)
            .build();

    public static final FoodComponent RADHE_RADHE_GURU_JI = new FoodComponent.Builder()
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 1), 1.0f)
            .statusEffect(new StatusEffectInstance(StatusEffects.LUCK, 100, 19), 1.0f)
            .build();
}