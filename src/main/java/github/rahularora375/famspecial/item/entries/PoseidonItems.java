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

public final class PoseidonItems {
    private PoseidonItems() {}

    // Two-tone name palette: first word deep sky blue (italic), second word pure gold (bold).
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0x00BFFF)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)).withBold(true).withItalic(false);
    // Lore palette: neon cyan base, electric yellow-gold accent for proper-noun / emphasis words.
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0x00E5FF)).withItalic(true);
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xFFEA00)).withItalic(true);

    // Per-piece modifiers: every Poseidon armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) on its slot, totalling +2 hearts across the full set.
    // Vanilla diamond armor/toughness is preserved by copying the entries
    // from a freshly-built vanilla stack's component.
    public static final AttributeModifiersComponent CROWN_MODIFIERS  = buildPoseidonArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "poseidon_set_head");
    public static final AttributeModifiersComponent PLATE_MODIFIERS  = buildPoseidonArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "poseidon_set_chest");
    public static final AttributeModifiersComponent BIND_MODIFIERS   = buildPoseidonArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "poseidon_set_legs");
    public static final AttributeModifiersComponent TRUDGE_MODIFIERS = buildPoseidonArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "poseidon_set_feet");

    private static AttributeModifiersComponent buildPoseidonArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildTridentOfOlympus(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildLorelaisCrown(enchants));
        entries.add(buildIronbornPlate(enchants));
        entries.add(buildHighstormsBind(enchants));
        entries.add(buildPhinnsTrudge(enchants));
    }

    // Riptide V is over vanilla cap (max 3) — matches this mod's convention
    // of exceeding vanilla enchantment ceilings (Sharpness 7, Power 7, etc).
    // No Mending by design — pairs with Unbreaking 4 for a finite-but-long-
    // lived trident.
    public static ItemStack buildTridentOfOlympus(RegistryWrapper<Enchantment> enchants) {
        return ModItemGroups.buildItem(Items.TRIDENT,
                twoTone("Trident of", "Olympus"),
                List.of(Text.literal("I regret many things").setStyle(LORE_BASE),
                        Text.literal("Killing you will not be one of them").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.UNBREAKING, 4),
                ModItemGroups.e(Enchantments.RIPTIDE, 5),
                ModItemGroups.e(Enchantments.IMPALING, 5));
    }

    // All four pieces are tagged SET_ID="poseidon". Full-set bonus is
    // "Riptide works anywhere" (no water/rain requirement), enforced by
    // TridentItemMixin redirecting the vanilla water check when the
    // wearer has all four equipped.
    public static ItemStack buildLorelaisCrown(RegistryWrapper<Enchantment> enchants) {
        ItemStack lorelaiCrown = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Lorelai's", "Crown"),
                List.of(Text.literal("Underwater there were no sweet things").setStyle(LORE_BASE),
                        Text.literal("only salty and bloody").setStyle(LORE_BASE)),
                enchants);
        lorelaiCrown.set(ModComponents.SET_ID, "poseidon");
        // Helmet-alone bonus: Water Breathing (like the potion), always on
        // while the helmet is worn — independent of set completion.
        lorelaiCrown.set(ModComponents.GRANTS_WATER_BREATHING, true);
        lorelaiCrown.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CROWN_MODIFIERS);
        return lorelaiCrown;
    }

    public static ItemStack buildIronbornPlate(RegistryWrapper<Enchantment> enchants) {
        ItemStack ironbornPlate = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Ironborn", "Plate"),
                List.of(Text.literal("What is dead may never die").setStyle(LORE_BASE)),
                enchants);
        ironbornPlate.set(ModComponents.SET_ID, "poseidon");
        ironbornPlate.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, PLATE_MODIFIERS);
        return ironbornPlate;
    }

    public static ItemStack buildHighstormsBind(RegistryWrapper<Enchantment> enchants) {
        ItemStack highstormBind = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Highstorm's", "Bind"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                                "When ",
                                Text.literal("Honor").setStyle(LORE_ACCENT),
                                " was shattered,"),
                        Text.literal("his power did not vanish").setStyle(LORE_BASE),
                        Text.literal("It began to ride the storms").setStyle(LORE_BASE)),
                enchants);
        highstormBind.set(ModComponents.SET_ID, "poseidon");
        highstormBind.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, BIND_MODIFIERS);
        return highstormBind;
    }

    public static ItemStack buildPhinnsTrudge(RegistryWrapper<Enchantment> enchants) {
        ItemStack phinnTrudge = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Phinn's", "Trudge"),
                List.of(Text.literal("Pardon me, excuse me, pardon").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.FEATHER_FALLING, 4));
        phinnTrudge.set(ModComponents.SET_ID, "poseidon");
        phinnTrudge.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, TRUDGE_MODIFIERS);
        return phinnTrudge;
    }
}
