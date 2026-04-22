package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.sound.ModSounds;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

public class BlockBreakHandler {
    // 1 / N per block broken. Rolled once per break regardless of how many
    // drops the underlying block itself yields.
    private static final int BONUS_DIAMOND_ROLL_DENOMINATOR = 800;

    public static void register() {
        // AFTER fires post-break, so the block has already been consumed and
        // its own drops have spawned. We add the phantom diamond_ore drops on
        // top — routed through Block.dropStacks so Fortune and Silk Touch on
        // the pickaxe apply the same way they would against a real diamond
        // ore (Silk Touch drops a diamond_ore BLOCK, Fortune rolls the raw-
        // diamond multiplier, etc.).
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            if (player.isCreative() || player.isSpectator()) return;

            ItemStack tool = player.getMainHandStack();
            if (!Boolean.TRUE.equals(tool.get(ModComponents.BONUS_DIAMOND_CHANCE))) return;

            if (serverWorld.random.nextInt(BONUS_DIAMOND_ROLL_DENOMINATOR) != 0) return;

            // Phantom-drop a diamond ore block at the position. Passing the
            // player as the entity + the pickaxe as the tool means the vanilla
            // diamond_ore loot table sees the same LootContext it would see on
            // a real diamond-ore break — Fortune/Silk Touch feed in via TOOL,
            // achievement-style drop attribution via THIS_ENTITY.
            Block.dropStacks(
                    Blocks.DIAMOND_ORE.getDefaultState(),
                    serverWorld,
                    pos,
                    null,
                    serverPlayer,
                    tool
            );

            // Null-source play so nearby players also hear the proc. PLAYERS
            // category so it rides the Players volume slider rather than
            // Blocks — this is a player-gear event, not a world sound.
            serverWorld.playSound(null, pos,
                    ModSounds.BONUS_DIAMOND, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });

        FamSpecial.LOGGER.info("Registering block-break handlers for {}", FamSpecial.MOD_ID);
    }
}
