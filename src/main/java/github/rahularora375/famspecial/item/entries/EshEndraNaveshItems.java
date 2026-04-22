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

public final class EshEndraNaveshItems {
    private EshEndraNaveshItems() {}

    private static final Style NAME_PIECE = Style.EMPTY
            .withColor(TextColor.fromRgb(0xE8D171)).withBold(true).withItalic(false);
    private static final Style LORE_BASE = Style.EMPTY
            .withColor(TextColor.fromRgb(0xA88FBA)).withItalic(true);

    // Per-piece modifiers: every Esh-Endra-Navesh armor piece grants +1
    // MAX_HEALTH (= 0.5 heart) on its slot. Totals at 4/4 are +2 hearts. Vanilla
    // diamond armor/toughness is preserved by copying the entries from a freshly-
    // built vanilla stack's component.
    public static final AttributeModifiersComponent HEAD_MODIFIERS  = buildArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "esh_endra_navesh_set_head");
    public static final AttributeModifiersComponent CHEST_MODIFIERS = buildArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "esh_endra_navesh_set_chest");
    public static final AttributeModifiersComponent LEGS_MODIFIERS  = buildArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "esh_endra_navesh_set_legs");
    public static final AttributeModifiersComponent FEET_MODIFIERS  = buildArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "esh_endra_navesh_set_feet");

    private static AttributeModifiersComponent buildArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        // Theme-adjacent tool — no SET_ID, so it doesn't count toward 4/4.
        // Just Hit Bro — diamond pickaxe. Carries BONUS_DIAMOND_CHANCE so
        // BlockBreakHandler rolls the vanilla blocks/diamond_ore loot table on
        // every break at 1/10_000, using the same stack as the tool so Fortune
        // and Silk Touch apply naturally.
        ItemStack pickaxe = ModItemGroups.buildItem(Items.DIAMOND_PICKAXE,
                Text.literal("Just Hit Bro").setStyle(NAME_PIECE),
                List.of(Text.literal("Abe yar lassi bugged hai kya ?").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.FORTUNE, 4));
        pickaxe.set(ModComponents.BONUS_DIAMOND_CHANCE, true);
        entries.add(pickaxe);
    }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        // All four pieces tagged SET_ID="esh_endra_navesh".
        // 4/4 set bonus is Haste I. Helmet additionally grants Bad Omen while
        // worn between 8 PM and 6 AM (or always in Nether/End).
        ItemStack helmet = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                Text.literal("Time Dekhlo Helmet").setStyle(NAME_PIECE),
                List.of(Text.literal("12 baj gaye bhai, brain off karke khel rha hu...").setStyle(LORE_BASE)),
                enchants);
        helmet.set(ModComponents.SET_ID, "esh_endra_navesh");
        helmet.set(ModComponents.GRANTS_OMINOUS, true);
        helmet.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HEAD_MODIFIERS);
        entries.add(helmet);

        ItemStack chestplate = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                Text.literal("Bombardiro Coccodrillo").setStyle(NAME_PIECE),
                List.of(Text.literal("Chestplate Desu").setStyle(LORE_BASE)),
                enchants);
        chestplate.set(ModComponents.SET_ID, "esh_endra_navesh");
        chestplate.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CHEST_MODIFIERS);
        entries.add(chestplate);

        ItemStack leggings = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                Text.literal("Knee-Use Paper").setStyle(NAME_PIECE),
                List.of(Text.literal("Bobby Deol mai sone di").setStyle(LORE_BASE)),
                enchants);
        leggings.set(ModComponents.SET_ID, "esh_endra_navesh");
        leggings.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGS_MODIFIERS);
        entries.add(leggings);

        ItemStack boots = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                Text.literal("Dhoom Machale").setStyle(NAME_PIECE),
                List.of(Text.literal("~ Coop 2025").setStyle(LORE_BASE)),
                enchants);
        boots.set(ModComponents.SET_ID, "esh_endra_navesh");
        boots.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FEET_MODIFIERS);
        entries.add(boots);
    }
}
