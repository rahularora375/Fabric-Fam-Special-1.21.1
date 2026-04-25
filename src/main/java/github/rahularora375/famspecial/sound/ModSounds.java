package github.rahularora375.famspecial.sound;

import github.rahularora375.famspecial.FamSpecial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    private ModSounds() {}

    public static final Identifier NECROMANCER_SUMMON_ID =
            Identifier.of(FamSpecial.MOD_ID, "necromancer_summon");

    public static final SoundEvent NECROMANCER_SUMMON =
            Registry.register(Registries.SOUND_EVENT, NECROMANCER_SUMMON_ID, SoundEvent.of(NECROMANCER_SUMMON_ID));

    public static final Identifier BONUS_DIAMOND_ID =
            Identifier.of(FamSpecial.MOD_ID, "bonus_diamond");

    public static final SoundEvent BONUS_DIAMOND =
            Registry.register(Registries.SOUND_EVENT, BONUS_DIAMOND_ID, SoundEvent.of(BONUS_DIAMOND_ID));

    public static final Identifier INDY_ID =
            Identifier.of(FamSpecial.MOD_ID, "indy");

    public static final SoundEvent INDY =
            Registry.register(Registries.SOUND_EVENT, INDY_ID, SoundEvent.of(INDY_ID));

    public static void register() {
        FamSpecial.LOGGER.info("Registering sounds for {}", FamSpecial.MOD_ID);
    }
}
