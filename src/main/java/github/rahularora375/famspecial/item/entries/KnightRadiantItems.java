package github.rahularora375.famspecial.item.entries;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.item.ModItemGroups;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
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

public final class KnightRadiantItems {
    private KnightRadiantItems() {}

    // Two-tone name palette: first word pale sky blue (italic), second word near-white-blue (bold).
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xA0D2FF)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xE0F0FF)).withBold(true).withItalic(false);
    // Lore palette: soft sky-blue base, pure white accent for proper-noun / emphasis words.
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0xB8DDFF)).withItalic(true);
    @SuppressWarnings("unused")
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withItalic(true);

    // Per-piece flat modifiers: every Shard armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) on its slot, totalling +2 hearts across the full set.
    // Vanilla diamond armor/toughness is preserved by copying the entries from
    // a freshly-built vanilla stack's component.
    public static final AttributeModifiersComponent HELMET_MODIFIERS   = buildShardArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "knight_radiant_helmet");
    public static final AttributeModifiersComponent PLATE_MODIFIERS    = buildShardArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "knight_radiant_chest");
    public static final AttributeModifiersComponent LEGGINGS_MODIFIERS = buildShardArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "knight_radiant_legs");
    public static final AttributeModifiersComponent BOOTS_MODIFIERS    = buildShardArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "knight_radiant_feet");

    private static AttributeModifiersComponent buildShardArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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
        // Oathbringer is theme-adjacent — no SET_ID, no enchants, no attribute
        // overrides in this pass. Single-word name rendered in NAME_PIECE only.
        // Carries GRANTS_SHARDBEARING so the wearer's melee hits chip an extra
        // 5% of the target's current HP past all mitigation (LivingEntityMixin)
        // and the cosmetic Shardbearing HUD badge shows up top-right
        // (ArmorEffects' shardbearing_mainhand).
        ItemStack oathbringer = ModItemGroups.buildItem(Items.DIAMOND_SWORD,
                Text.literal("Oathbringer").setStyle(NAME_PIECE),
                List.of(Text.literal("What is a man's life worth?..").setStyle(LORE_BASE),
                        Text.literal("Coincidentally, that is the exact value").setStyle(LORE_BASE),
                        Text.literal("of a Shard blade").setStyle(LORE_BASE)),
                enchants);
        oathbringer.set(ModComponents.GRANTS_SHARDBEARING, true);
        entries.add(oathbringer);
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        // All four pieces tagged SET_ID="knight_radiant". No set bonus wired
        // in this pass — SET_ID is forward-compatible scaffolding.
        ItemStack shardHelmet = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Shard", "Helmet"),
                List.of(Text.literal("A man's emotions are what define him").setStyle(LORE_BASE),
                        Text.literal("control is the hallmark of true strength").setStyle(LORE_BASE)),
                enchants);
        shardHelmet.set(ModComponents.SET_ID, "knight_radiant");
        shardHelmet.set(ModComponents.INDESTRUCTIBLE, true);
        shardHelmet.set(ModComponents.REGENS_DURABILITY, true);
        shardHelmet.set(ModComponents.BLOCKS_MENDING, true);
        shardHelmet.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HELMET_MODIFIERS);
        entries.add(shardHelmet);

        ItemStack shardPlate = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Shard", "Plate"),
                List.of(Text.literal("If I must fall, I'll rise each time a better man").setStyle(LORE_BASE)),
                enchants);
        shardPlate.set(ModComponents.SET_ID, "knight_radiant");
        shardPlate.set(ModComponents.INDESTRUCTIBLE, true);
        shardPlate.set(ModComponents.REGENS_DURABILITY, true);
        shardPlate.set(ModComponents.BLOCKS_MENDING, true);
        shardPlate.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, PLATE_MODIFIERS);
        entries.add(shardPlate);

        ItemStack shardLeggings = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Shard", "Leggings"),
                List.of(Text.literal("Sometimes a hypocrite is just a man").setStyle(LORE_BASE),
                        Text.literal("in the process of changing").setStyle(LORE_BASE)),
                enchants);
        shardLeggings.set(ModComponents.SET_ID, "knight_radiant");
        shardLeggings.set(ModComponents.INDESTRUCTIBLE, true);
        shardLeggings.set(ModComponents.REGENS_DURABILITY, true);
        shardLeggings.set(ModComponents.BLOCKS_MENDING, true);
        shardLeggings.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGGINGS_MODIFIERS);
        entries.add(shardLeggings);

        ItemStack shardBoots = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Shard", "Boots"),
                List.of(Text.literal("A journey will have pain and failure").setStyle(LORE_BASE),
                        Text.literal("It is not only the steps forward").setStyle(LORE_BASE),
                        Text.literal("that we must accept").setStyle(LORE_BASE),
                        Text.literal("It is the stumbles").setStyle(LORE_BASE)),
                enchants);
        shardBoots.set(ModComponents.SET_ID, "knight_radiant");
        shardBoots.set(ModComponents.INDESTRUCTIBLE, true);
        shardBoots.set(ModComponents.REGENS_DURABILITY, true);
        shardBoots.set(ModComponents.BLOCKS_MENDING, true);
        shardBoots.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, BOOTS_MODIFIERS);
        entries.add(shardBoots);
    }
}
