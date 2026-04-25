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

// Electric-cyan "beyond the sea" theme. Holds the Sage's Grace sword (tagged
// HEALS_TARGET) and a 4-piece diamond armor set tagged SET_ID="pacifist".
// Note: the sword is NOT part of the set — it shares the theme/palette but
// carries no SET_ID, so the "Part of the Pacifist Set" tooltip line appears
// only on the four armor pieces. Set membership is read off the SET_ID
// component, not off this file's grouping.
public final class PacifistItems {
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xFFE0E0)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(true).withItalic(false);
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFCCD5)).withItalic(true);
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withItalic(true);

    // Per-piece flat modifiers: every Pacifist armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) plus +0.15 KNOCKBACK_RESISTANCE on its slot (totals: +2
    // hearts and +0.6 KB resistance at 4/4, i.e. 60% stiffness to knockback).
    // Vanilla diamond armor/toughness is preserved by copying the entries from
    // a freshly-built vanilla stack's component.
    public static final AttributeModifiersComponent CROWN_MODIFIERS    = buildPacifistArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "pacifist_set_head",  1.0);
    public static final AttributeModifiersComponent THORS_MODIFIERS = buildPacifistArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "pacifist_set_chest", 1.0);
    public static final AttributeModifiersComponent WRAP_MODIFIERS     = buildPacifistArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "pacifist_set_legs",  1.0);
    public static final AttributeModifiersComponent FAREWELL_MODIFIERS    = buildPacifistArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "pacifist_set_feet",  1.0);

    private static AttributeModifiersComponent buildPacifistArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix, double healthAmount) {
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
                        healthAmount,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
        builder.add(EntityAttributes.KNOCKBACK_RESISTANCE,
                new EntityAttributeModifier(
                        Identifier.of(FamSpecial.MOD_ID, idSuffix + "_kbres"),
                        0.15,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                slot);
        return builder.build();
    }

    private PacifistItems() {}

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildSagesGrace(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildSagesCrown(enchants));
        entries.add(buildSonOfThors(enchants));
        entries.add(buildSagesWrap(enchants));
        entries.add(buildVikingsFarewell(enchants));
    }

    // Sage's Grace — diamond sword flagged HEALS_TARGET. AttackHandlers
    // reads that flag and swaps vanilla damage for a heal on the target
    // (4 HP normal, 8 HP crit) plus heart particles and a levelup chime.
    // Fixed loadout (Unbreaking 5 + Mending); grindstone/anvil blocked via
    // mixins so it stays pristine. Shares the electric-cyan palette with
    // the armor set but is NOT a set member (no SET_ID).
    public static ItemStack buildSagesGrace(RegistryWrapper<Enchantment> enchants) {
        ItemStack sagesGrace = ModItemGroups.buildItem(Items.DIAMOND_SWORD,
                twoTone("Sage's", "Grace"),
                List.of(Text.literal("Your duty is not over").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.UNBREAKING, 5),
                ModItemGroups.e(Enchantments.MENDING, 1));
        sagesGrace.set(ModComponents.HEALS_TARGET, true);
        return sagesGrace;
    }

    // Pacifist Set — 4-piece diamond armor with a shared SET_ID. Full-set
    // bonus (Regen I + Armor +3 + Knockback Resistance +3) is applied in
    // ArmorEffects whenever all four pieces with SET_ID="pacifist" are
    // equipped together. No time gate; the bonus is always on while suited.
    // Each piece uses a distinct "first/last" name split rather than a
    // common prefix — first word is the electric-cyan italic, last word is
    // the bright-blue bold piece accent.
    public static ItemStack buildSagesCrown(RegistryWrapper<Enchantment> enchants) {
        ItemStack sagesCrown = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Sage's", "Crown"),
                List.of(Text.literal("I will defend you all with my power").setStyle(LORE_BASE),
                        Text.literal("and with my life").setStyle(LORE_BASE)),
                enchants);
        sagesCrown.set(ModComponents.SET_ID, "pacifist");
        sagesCrown.set(ModComponents.SHOWS_ENTITY_HP, true);
        sagesCrown.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CROWN_MODIFIERS);
        return sagesCrown;
    }

    public static ItemStack buildSonOfThors(RegistryWrapper<Enchantment> enchants) {
        ItemStack pacifistChest = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Son of", "Thors"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                                "I have ",
                                Text.literal("no enemies").setStyle(LORE_ACCENT)),
                        Text.literal("None at all").setStyle(LORE_BASE)),
                enchants);
        pacifistChest.set(ModComponents.SET_ID, "pacifist");
        pacifistChest.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, THORS_MODIFIERS);
        return pacifistChest;
    }

    public static ItemStack buildSagesWrap(RegistryWrapper<Enchantment> enchants) {
        ItemStack sagesWrap = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Sage's", "Wrap"),
                List.of(Text.literal("I wasn't strong enough before").setStyle(LORE_BASE),
                        Text.literal("But now, now I am strong enough for us all").setStyle(LORE_BASE)),
                enchants);
        sagesWrap.set(ModComponents.SET_ID, "pacifist");
        sagesWrap.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, WRAP_MODIFIERS);
        return sagesWrap;
    }

    public static ItemStack buildVikingsFarewell(RegistryWrapper<Enchantment> enchants) {
        ItemStack pacifistBoots = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Viking's", "Farewell"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "Beyond the ",
                        Text.literal("sea").setStyle(LORE_ACCENT),
                        ", there is a land ",
                        Text.literal("without war").setStyle(LORE_ACCENT))),
                enchants);
        pacifistBoots.set(ModComponents.SET_ID, "pacifist");
        pacifistBoots.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FAREWELL_MODIFIERS);
        return pacifistBoots;
    }

    // Split each item name into a prefix (possessive / connector, electric-cyan
    // italic) and a piece word (bright-blue bold). Keeps the palette visible
    // on every entry without forcing a shared "Pacifist's " prefix — names
    // can be anything ("Sage's Crown", "Viking's Farewell", "Son of Thors").
    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }
}
