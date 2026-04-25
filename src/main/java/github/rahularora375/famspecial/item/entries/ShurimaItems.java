package github.rahularora375.famspecial.item.entries;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.item.ModItemGroups;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.List;

// Shurima / Sun Emperor theme. 4-piece diamond armor set + 1 signature spear.
// Set bonuses:
//   - Per-piece: +1 MAX_HEALTH (= 0.5 heart), +0.0025 MOVEMENT_SPEED,
//                +0.075 KNOCKBACK_RESISTANCE on its slot. Totals at 4/4 are
//                +2 hearts, +0.01 move speed (10% of base), +0.3 KB resistance
//                (30% stiffness).
//   - Helmet (Sun Emperor's Crown) alone + DESERT biome: grants SUNS_PROTECTION
//     (20% incoming damage reduction via LivingEntityMixin).
//   - Full 4/4 set + DESERT biome: grants SHURIMAN_ENDURANCE (cancels all
//     saturation loss via PlayerEntityExhaustionMixin). 4-minute apply-once
//     duration — persists anywhere the player goes after it's earned.
//   - Sun Disc Spear held in main hand: grants EMPERORS_DIVIDE
//     (LivingEntityKnockbackMixin bypasses the target's KNOCKBACK_RESISTANCE).
//     Applied/removed on a per-tick fast-path so it's instant on swap.
public final class ShurimaItems {
    private ShurimaItems() {}

    // Two-tone name palette: worn-gold prefix (italic), pale-sand-white piece (bold).
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xC8A84B)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xF0E6C8)).withBold(true).withItalic(false);
    // Lore palette: dusty-parchment base, warm-orange accent for signature / emphasis words.
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0xB8A070)).withItalic(true);
    @SuppressWarnings("unused")
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xE8922A)).withItalic(true);

    // Per-piece flat modifiers: +1 MAX_HEALTH, +0.0025 MOVEMENT_SPEED,
    // +0.075 KNOCKBACK_RESISTANCE on the piece's slot. Vanilla diamond
    // armor/toughness is preserved by copying the entries from a freshly-built
    // vanilla stack's component.
    public static final AttributeModifiersComponent HEAD_MODIFIERS  = buildShurimaArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "shurima_set_head");
    public static final AttributeModifiersComponent CHEST_MODIFIERS = buildShurimaArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "shurima_set_chest");
    public static final AttributeModifiersComponent LEGS_MODIFIERS  = buildShurimaArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "shurima_set_legs");
    public static final AttributeModifiersComponent FEET_MODIFIERS  = buildShurimaArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "shurima_set_feet");

    private static AttributeModifiersComponent buildShurimaArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        AttributeModifiersComponent vanilla = new ItemStack(baseItem).get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (vanilla != null) {
            for (AttributeModifiersComponent.Entry entry : vanilla.modifiers()) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        builder.add(EntityAttributes.MAX_HEALTH,
                new EntityAttributeModifier(
                        Identifier.of(FamSpecial.MOD_ID, idSuffix + "_health"),
                        1.0,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
        builder.add(EntityAttributes.MOVEMENT_SPEED,
                new EntityAttributeModifier(
                        Identifier.of(FamSpecial.MOD_ID, idSuffix + "_speed"),
                        0.0025,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
        builder.add(EntityAttributes.KNOCKBACK_RESISTANCE,
                new EntityAttributeModifier(
                        Identifier.of(FamSpecial.MOD_ID, idSuffix + "_kbres"),
                        0.075,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
        return builder.build();
    }

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildSunDiscSpear(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildSunEmperorsCrown(enchants));
        entries.add(buildMantleOfShurima(enchants));
        entries.add(buildSandstriderWraps(enchants));
        entries.add(buildDriftingSands(enchants));
    }

    // Sun Disc Spear: theme-adjacent — no SET_ID. Built on DIAMOND_SPEAR
    // (matches FireSerpent precedent). Fixed enchants: Knockback 5 (max)
    // + Sharpness 6. Carries IGNORES_KB_RESISTANCE so the per-tick
    // fast-path in ArmorEffects applies EMPERORS_DIVIDE while the spear
    // is in the main hand — LivingEntityKnockbackMixin reads that effect
    // off the attacker to zero out the victim's KB resistance.
    public static ItemStack buildSunDiscSpear(RegistryWrapper<Enchantment> enchants) {
        ItemStack sunDiscSpear = ModItemGroups.buildItem(Items.DIAMOND_SPEAR,
                twoTone("Sun Disc", "Spear"),
                List.of(Text.literal("You dare raise arms").setStyle(LORE_BASE),
                        Text.literal("against the emperor of Shurima?").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.KNOCKBACK, 5),
                ModItemGroups.e(Enchantments.SHARPNESS, 6));
        sunDiscSpear.set(ModComponents.IGNORES_KB_RESISTANCE, true);
        return sunDiscSpear;
    }

    // All four pieces tagged SET_ID="shurima". Full-set bonus is
    // Shuriman Endurance (desert-gated, apply-once 4-minute duration).
    // The boots additionally carry GRANTS_SUNS_PROTECTION — the
    // piece-alone + desert-gated Resistance I.
    public static ItemStack buildSunEmperorsCrown(RegistryWrapper<Enchantment> enchants) {
        ItemStack sunEmperorsCrown = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Sun Emperor's", "Crown"),
                List.of(Text.literal("I am the emperor of Shurima").setStyle(LORE_BASE),
                        Text.literal("I do not kneel").setStyle(LORE_BASE)),
                enchants);
        sunEmperorsCrown.set(ModComponents.SET_ID, "shurima");
        sunEmperorsCrown.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HEAD_MODIFIERS);
        return sunEmperorsCrown;
    }

    public static ItemStack buildMantleOfShurima(RegistryWrapper<Enchantment> enchants) {
        ItemStack mantleOfShurima = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Mantle of", "Shurima"),
                List.of(Text.literal("Shurima will rise again").setStyle(LORE_BASE),
                        Text.literal("as will its emperor").setStyle(LORE_BASE)),
                enchants);
        mantleOfShurima.set(ModComponents.SET_ID, "shurima");
        mantleOfShurima.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CHEST_MODIFIERS);
        return mantleOfShurima;
    }

    public static ItemStack buildSandstriderWraps(RegistryWrapper<Enchantment> enchants) {
        ItemStack sandstriderWraps = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Sandstrider", "Wraps"),
                List.of(Text.literal("I have crossed the sands of eternity").setStyle(LORE_BASE),
                        Text.literal("What is distance to me?").setStyle(LORE_BASE)),
                enchants);
        sandstriderWraps.set(ModComponents.SET_ID, "shurima");
        sandstriderWraps.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGS_MODIFIERS);
        return sandstriderWraps;
    }

    public static ItemStack buildDriftingSands(RegistryWrapper<Enchantment> enchants) {
        ItemStack driftingSands = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Drifting", "Sands"),
                List.of(Text.literal("I walked these roads").setStyle(LORE_BASE),
                        Text.literal("before your fathers' fathers were born").setStyle(LORE_BASE)),
                enchants);
        driftingSands.set(ModComponents.SET_ID, "shurima");
        driftingSands.set(ModComponents.GRANTS_SUNS_PROTECTION, true);
        driftingSands.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FEET_MODIFIERS);
        return driftingSands;
    }
}
