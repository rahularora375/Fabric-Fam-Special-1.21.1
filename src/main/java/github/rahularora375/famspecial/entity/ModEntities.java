package github.rahularora375.famspecial.entity;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {}

    private static final RegistryKey<EntityType<?>> VOLLEY_ARROW_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(FamSpecial.MOD_ID, "volley_arrow"));

    public static final EntityType<VolleyArrowEntity> VOLLEY_ARROW = EntityType.Builder
            .<VolleyArrowEntity>create(VolleyArrowEntity::new, SpawnGroup.MISC)
            .dropsNothing()
            .dimensions(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .maxTrackingRange(4)
            .trackingTickInterval(20)
            .build(VOLLEY_ARROW_KEY);

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, VOLLEY_ARROW_KEY, VOLLEY_ARROW);
        FamSpecial.LOGGER.info("Registering entities for {}", FamSpecial.MOD_ID);
    }
}
