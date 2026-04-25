package github.rahularora375.famspecial;

import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.entity.ModEntities;
import github.rahularora375.famspecial.item.ArmorEffects;
import github.rahularora375.famspecial.item.AttackHandlers;
import github.rahularora375.famspecial.item.BlockBreakHandler;
import github.rahularora375.famspecial.item.BountyHunterKills;
import github.rahularora375.famspecial.item.FortuneGloryItem;
import github.rahularora375.famspecial.item.ModItemGroups;
import github.rahularora375.famspecial.item.NecromancerSummon;
import github.rahularora375.famspecial.item.ThorEffects;
import github.rahularora375.famspecial.item.entries.RaidersLegacyItems;
import github.rahularora375.famspecial.item.entries.ThorItems;
import github.rahularora375.famspecial.loot.ModLootTableModifier;
import github.rahularora375.famspecial.loot.PrebuiltStackEntry;
import github.rahularora375.famspecial.net.VersionHandshake;
import github.rahularora375.famspecial.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FamSpecial implements ModInitializer {
	public static final String MOD_ID = "famspecial";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	// Read once at load from the fabric metadata. Fed to the login handshake
	// so client and server can kick each other on version drift.
	public static final String MOD_VERSION = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");

	@Override
	public void onInitialize() {
		ModComponents.register();
		ModStatusEffects.register();
		ModSounds.register();
		ModEntities.register();
		// Must run before ModItemGroups so MJOLNIR is a live, registered Item
		// by the time the Gear tab's entries lambda enumerates it. Registering
		// an Item subclass at class-load (static-final init) crashes in 1.21+
		// because SimpleRegistry rejects intrusive holders — see ThorItems.
		ThorItems.register();
		RaidersLegacyItems.register();
		ModItemGroups.registerItemGroups();
		PrebuiltStackEntry.register();
		ModLootTableModifier.register();
		ArmorEffects.register();
		AttackHandlers.register();
		BlockBreakHandler.register();
		BountyHunterKills.register();
		FortuneGloryItem.registerTickHandler();
		NecromancerSummon.register();
		ThorEffects.register();
		VersionHandshake.registerServer();
		LOGGER.info("Initialized {} v{}", MOD_ID, MOD_VERSION);
	}
}
