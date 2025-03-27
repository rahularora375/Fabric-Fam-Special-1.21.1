package github.rahularora375.famspecial.effect;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> GURU_JI_BLESSING = registerStatusEffect("guru_ji_blessing",
            new GuruJiBlessingEffect(StatusEffectCategory.NEUTRAL, 0xfdbf02));


    private static RegistryEntry<StatusEffect> registerStatusEffect(String name, StatusEffect statusEffect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(FamSpecial.MOD_ID, name), statusEffect);
    }

    public static void registerEffects() {
        FamSpecial.LOGGER.info("Registering Mod Effects for " + FamSpecial.MOD_ID);
    }
}