package github.rahularora375.famspecial.item.entries;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.item.FortuneGloryItem;
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

public final class RaidersLegacyItems {
    private RaidersLegacyItems() {}

    public static FortuneGloryItem FORTUNE_AND_GLORY;

    public static void register() {
        FortuneGloryItem.register();
        FORTUNE_AND_GLORY = FortuneGloryItem.FORTUNE_AND_GLORY;
    }

    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xC19A6B)).withItalic(true);
    private static final Style NAME_PIECE  = Style.EMPTY.withColor(TextColor.fromRgb(0xE8C591)).withBold(true).withItalic(false);
    private static final Style LORE_BASE   = Style.EMPTY.withColor(TextColor.fromRgb(0xA47449)).withItalic(true);
    @SuppressWarnings("unused")
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0xF4C430)).withItalic(true);

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }

    public static final AttributeModifiersComponent HEAD_MODIFIERS  = buildRaiderArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "raider_set_head");
    public static final AttributeModifiersComponent CHEST_MODIFIERS = buildRaiderArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "raider_set_chest");
    public static final AttributeModifiersComponent LEGS_MODIFIERS  = buildRaiderArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "raider_set_legs");
    public static final AttributeModifiersComponent FEET_MODIFIERS  = buildRaiderArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "raider_set_feet");

    private static AttributeModifiersComponent buildRaiderArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildFortuneAndGlory(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildHatOfTheRaider(enchants));
        entries.add(buildExplorersJacket(enchants));
        entries.add(buildTrousersOfTheTrail(enchants));
        entries.add(buildTheCloseCall(enchants));
    }

    public static ItemStack buildFortuneAndGlory(RegistryWrapper<Enchantment> enchants) {
        ItemStack fortuneAndGlory = ModItemGroups.buildItem(FORTUNE_AND_GLORY,
                Text.literal("Fortune & Glory").setStyle(NAME_PIECE),
                List.of(Text.literal("It belongs in a museum").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.QUICK_CHARGE, 4));
        fortuneAndGlory.set(ModComponents.CRUSADERS_VOLLEY, true);
        return fortuneAndGlory;
    }

    public static ItemStack buildHatOfTheRaider(RegistryWrapper<Enchantment> enchants) {
        ItemStack hat = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Hat of the", "Raider"),
                List.of(Text.literal("He lost the jeep. He lost the whip.").setStyle(LORE_BASE),
                        Text.literal("He lost the girl, twice. But never the hat").setStyle(LORE_BASE)),
                enchants);
        hat.set(ModComponents.SET_ID, "raider");
        hat.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HEAD_MODIFIERS);
        return hat;
    }

    public static ItemStack buildExplorersJacket(RegistryWrapper<Enchantment> enchants) {
        ItemStack jacket = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("The Explorer's", "Jacket"),
                List.of(Text.literal("It's not armor. It's a map of every").setStyle(LORE_BASE),
                        Text.literal("close call he ever walked away from").setStyle(LORE_BASE)),
                enchants);
        jacket.set(ModComponents.SET_ID, "raider");
        jacket.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CHEST_MODIFIERS);
        return jacket;
    }

    public static ItemStack buildTrousersOfTheTrail(RegistryWrapper<Enchantment> enchants) {
        ItemStack trousers = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Trousers of", "the Trail"),
                List.of(Text.literal("Snakes. Why'd it have to be snakes?").setStyle(LORE_BASE)),
                enchants);
        trousers.set(ModComponents.SET_ID, "raider");
        trousers.set(ModComponents.BOUNTY_HUNTER, true);
        trousers.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGS_MODIFIERS);
        return trousers;
    }

    public static ItemStack buildTheCloseCall(RegistryWrapper<Enchantment> enchants) {
        ItemStack boots = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("The", "Close Call"),
                List.of(Text.literal("We are going to die").setStyle(LORE_BASE)),
                enchants);
        boots.set(ModComponents.SET_ID, "raider");
        boots.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FEET_MODIFIERS);
        return boots;
    }
}
