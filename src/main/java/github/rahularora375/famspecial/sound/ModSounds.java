package github.rahularora375.famspecial.sound;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent ITEM_RADHE_RADHE_GURU_JI_DRINK = registerSoundEvent("radhe_radhe_guru_ji_drink");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(FamSpecial.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        FamSpecial.LOGGER.info("Registering Mod Sounds for " + FamSpecial.MOD_ID);
    }
}