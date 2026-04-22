package github.rahularora375.famspecial.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.registry.RegistryKeys;

public class ModLootTableModifier {
    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;
            var enchants = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
            LegendaryPool.buildFor(key, enchants).ifPresent(pool -> tableBuilder.pool(pool));
            MapsPool.buildFor(key).ifPresent(pool -> tableBuilder.pool(pool));
        });
    }
}
