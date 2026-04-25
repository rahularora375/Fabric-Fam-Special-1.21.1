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

public final class MistbornItems {
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xB0C4DE)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xE0E8F5)).withBold(true).withItalic(false);
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0x9AA5C4)).withItalic(true);
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xD4DCE8)).withItalic(true);

    // Atium Dagger attribute modifiers — swapped in/out by ArmorEffects when
    // the dagger is held in the main hand. Attack speed is always double vanilla
    // netherite (3.2 = 2 × 1.6); only damage changes with the day/night cycle.
    //
    // Vanilla netherite sword baseline: 8 damage, 1.6 speed.
    // Day   → 4 Attack Damage (half vanilla), 3.2 Attack Speed (double vanilla)
    // Night → 8 Attack Damage (full vanilla), 3.2 Attack Speed (double vanilla)
    //
    // Empty-hand bases the attributes stack on top of: 1.0 damage, 4.0 speed.
    // So damage modifier = target − 1.0; speed modifier = target − 4.0.
    public static final AttributeModifiersComponent DAGGER_DAY_MODIFIERS = AttributeModifiersComponent.builder()
            .add(EntityAttributes.ATTACK_DAMAGE,
                    new EntityAttributeModifier(
                            Identifier.ofVanilla("base_attack_damage"),
                            3.0,
                            EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
            .add(EntityAttributes.ATTACK_SPEED,
                    new EntityAttributeModifier(
                            Identifier.ofVanilla("base_attack_speed"),
                            -0.8,
                            EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
            .build();

    public static final AttributeModifiersComponent DAGGER_NIGHT_MODIFIERS = AttributeModifiersComponent.builder()
            .add(EntityAttributes.ATTACK_DAMAGE,
                    new EntityAttributeModifier(
                            Identifier.ofVanilla("base_attack_damage"),
                            7.0,
                            EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
            .add(EntityAttributes.ATTACK_SPEED,
                    new EntityAttributeModifier(
                            Identifier.ofVanilla("base_attack_speed"),
                            -0.8,
                            EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND)
            .build();

    // Per-piece flat modifiers: every Mistborn armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) on its slot, totalling +2 hearts across the full set.
    // Vanilla diamond armor/toughness is preserved by copying the entries from
    // a freshly-built vanilla stack's component.
    public static final AttributeModifiersComponent VEIL_MODIFIERS  = buildMistbornArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "mistborn_set_head");
    public static final AttributeModifiersComponent CLOAK_MODIFIERS = buildMistbornArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "mistborn_set_chest");
    public static final AttributeModifiersComponent WRAP_MODIFIERS  = buildMistbornArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "mistborn_set_legs");
    public static final AttributeModifiersComponent DRIFT_MODIFIERS = buildMistbornArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "mistborn_set_feet");

    private static AttributeModifiersComponent buildMistbornArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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
        return builder.build();
    }

    private MistbornItems() {}

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildAtiumDagger(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildMistbornVeil(enchants));
        entries.add(buildMistbornCloak(enchants));
        entries.add(buildMistbornWrap(enchants));
        entries.add(buildMistbornDrift(enchants));
    }

    // Atium Dagger — Mistborn-adjacent weapon. Marker component NIGHT_STRENGTH
    // makes ArmorEffects swap its AttributeModifiers to the "night" form while
    // held in the main hand at night or in Nether/End, so the damage tooltip
    // shifts (day 4 → night 8). Speed stays constant at double vanilla (3.2).
    public static ItemStack buildAtiumDagger(RegistryWrapper<Enchantment> enchants) {
        ItemStack atiumDagger = ModItemGroups.buildItem(Items.NETHERITE_SWORD,
                twoTone("Atium", "Dagger"),
                List.of(Text.literal("I represent that one thing").setStyle(LORE_BASE),
                        Text.literal("you've never been able to killm").setStyle(LORE_BASE),
                        Text.literal("lord tyrant").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.SHARPNESS, 5), ModItemGroups.e(Enchantments.MENDING, 1));
        atiumDagger.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, DAGGER_DAY_MODIFIERS);
        atiumDagger.set(ModComponents.NIGHT_STRENGTH, true);
        return atiumDagger;
    }

    // Mistborn Set — two-tone names + accented lore. No enchantments.
    // "Part of the Mistborn Set (X/4)" and "Grants Night Vision" tooltip
    // lines are appended client-side via ItemTooltipCallback in
    // FamSpecialClient — the counter switches to gold at 4/4 to signal the
    // set bonus is active. Full-set bonus (Strength I + Speed I) applied
    // in ArmorEffects when all four pieces with SET_ID="mistborn" are
    // equipped together.
    public static ItemStack buildMistbornVeil(RegistryWrapper<Enchantment> enchants) {
        ItemStack mistbornVeil = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Mistborn", "Veil"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                                "The ",
                                Text.literal("mists").setStyle(LORE_ACCENT),
                                " reveal nothing"),
                        ModItemGroups.loreLine(LORE_BASE,
                                "That is their ",
                                Text.literal("gift").setStyle(LORE_ACCENT))),
                enchants);
        mistbornVeil.set(ModComponents.GRANTS_NIGHT_VISION, true);
        mistbornVeil.set(ModComponents.SET_ID, "mistborn");
        mistbornVeil.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, VEIL_MODIFIERS);
        return mistbornVeil;
    }

    public static ItemStack buildMistbornCloak(RegistryWrapper<Enchantment> enchants) {
        ItemStack mistbornCloak = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Mistborn", "Cloak"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "Sewn from a thousand tattered strips of ",
                        Text.literal("night").setStyle(LORE_ACCENT))),
                enchants);
        mistbornCloak.set(ModComponents.SET_ID, "mistborn");
        mistbornCloak.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CLOAK_MODIFIERS);
        return mistbornCloak;
    }

    public static ItemStack buildMistbornWrap(RegistryWrapper<Enchantment> enchants) {
        ItemStack mistbornWrap = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Mistborn", "Wrap"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                                "You don't hide in the ",
                                Text.literal("mist").setStyle(LORE_ACCENT)),
                        ModItemGroups.loreLine(LORE_BASE,
                                "You ",
                                Text.literal("become it").setStyle(LORE_ACCENT))),
                enchants);
        mistbornWrap.set(ModComponents.SET_ID, "mistborn");
        mistbornWrap.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, WRAP_MODIFIERS);
        return mistbornWrap;
    }

    public static ItemStack buildMistbornDrift(RegistryWrapper<Enchantment> enchants) {
        ItemStack mistbornDrift = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Mistborn", "Drift"),
                List.of(Text.literal("There's always another secret").setStyle(LORE_BASE)),
                enchants);
        mistbornDrift.set(ModComponents.SET_ID, "mistborn");
        mistbornDrift.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, DRIFT_MODIFIERS);
        return mistbornDrift;
    }

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }
}
