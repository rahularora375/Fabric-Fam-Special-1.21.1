package github.rahularora375.famspecial;

import github.rahularora375.famspecial.effect.ModEffects;
import github.rahularora375.famspecial.item.ModItemGroups;
import github.rahularora375.famspecial.item.ModItems;
import github.rahularora375.famspecial.sound.ModSounds;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FamSpecial implements ModInitializer {
	public static final String MOD_ID = "famspecial";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModEffects.registerEffects();
		ModSounds.registerSounds();
	}
}