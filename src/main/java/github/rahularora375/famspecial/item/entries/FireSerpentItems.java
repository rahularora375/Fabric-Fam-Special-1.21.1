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

public final class FireSerpentItems {
    private FireSerpentItems() {}

    // Two-tone name palette: blazing-orange prefix (italic), neon-green piece (bold).
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0xFF6600)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0x39FF14)).withBold(true).withItalic(false);
    // Lore palette: hot-orange base, toxic-green accent for signature / emphasis phrases.
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0xFF8833)).withItalic(true);
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0x50FF30)).withItalic(true);

    // Per-piece modifiers: every Fire Serpent armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) and +0.0025 MOVEMENT_SPEED on its slot. Totals at 4/4 are
    // +2 hearts and +0.01 movement speed (= 10% of the base player speed of
    // 0.1). Vanilla diamond armor/toughness is preserved by copying the entries
    // from a freshly-built vanilla stack's component.
    public static final AttributeModifiersComponent HEAD_MODIFIERS  = buildFireSerpentArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "fire_serpent_set_head");
    public static final AttributeModifiersComponent CHEST_MODIFIERS = buildFireSerpentArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "fire_serpent_set_chest");
    public static final AttributeModifiersComponent LEGS_MODIFIERS  = buildFireSerpentArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "fire_serpent_set_legs");
    public static final AttributeModifiersComponent FEET_MODIFIERS  = buildFireSerpentArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "fire_serpent_set_feet");

    private static AttributeModifiersComponent buildFireSerpentArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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
        return builder.build();
    }

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }

    public static void addWeapons(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildFireSerpentsWrath(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildImpalersCrown(enchants));
        entries.add(buildSerpentsEmbrace(enchants));
        entries.add(buildKindlingsWrap(enchants));
        entries.add(buildKindlingsPath(enchants));
    }

    // Theme-adjacent — no SET_ID, so it doesn't count toward 4/4.
    // Fire Serpent's Wrath: DIAMOND_SPEAR base — vanilla 1.21.11 ships a
    // full spear tier family (Items.DIAMOND_SPEAR, tagged #minecraft:spears)
    // with its own animation scale and the Lunge 3 (vanilla max) enchant
    // gated to that tag. Fire Aspect 2 (vanilla max) applies via the
    // broader #minecraft:enchantable/melee_weapon tree. Mending keeps it
    // repairable. No Sharpness/Looting/Unbreaking — the spear leans on
    // Lunge's dash + Fire Aspect's burn + the set's Messmer's Venom tick
    // for its damage profile, not raw bonus damage. Messmer's Venom is a
    // 4/4 set bonus applied per-hit by AttackHandlers, so the spear
    // poisons only while the full set is worn.
    public static ItemStack buildFireSerpentsWrath(RegistryWrapper<Enchantment> enchants) {
        ItemStack fireSerpentsWrath = ModItemGroups.buildItem(Items.DIAMOND_SPEAR,
                twoTone("Fire Serpent's", "Wrath"),
                List.of(Text.literal("Those stripped of the Grace of Gold").setStyle(LORE_BASE),
                        Text.literal("shall all meet death").setStyle(LORE_BASE),
                        ModItemGroups.loreLine(LORE_BASE,
                                "In the embrace of ",
                                Text.literal("Messmer's flame").setStyle(LORE_ACCENT))),
                enchants,
                ModItemGroups.e(Enchantments.LUNGE, 3),
                ModItemGroups.e(Enchantments.FIRE_ASPECT, 2),
                ModItemGroups.e(Enchantments.MENDING, 1));
        // Suppresses Lunge's post-piercing-attack exhaustion tick so dashing
        // doesn't eat hunger. Enforced by ApplyExhaustionEnchantmentEffectMixin.
        fireSerpentsWrath.set(ModComponents.NO_LUNGE_HUNGER, true);
        return fireSerpentsWrath;
    }

    // All four pieces tagged SET_ID="fire_serpent".
    // 4/4 set bonus is Messmer's Flame Aegis — 80% fire damage reduction (like
    // Fire Protection, not Fire Resistance). Applied by ArmorEffects and
    // enforced by LivingEntityMixin.
    public static ItemStack buildImpalersCrown(RegistryWrapper<Enchantment> enchants) {
        ItemStack impalersCrown = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Impaler's", "Crown"),
                List.of(Text.literal("Mother, wouldst thou truly Lordship sanction").setStyle(LORE_BASE),
                        Text.literal("in one so bereft of light?").setStyle(LORE_BASE)),
                enchants);
        impalersCrown.set(ModComponents.SET_ID, "fire_serpent");
        impalersCrown.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HEAD_MODIFIERS);
        return impalersCrown;
    }

    public static ItemStack buildSerpentsEmbrace(RegistryWrapper<Enchantment> enchants) {
        ItemStack serpentsEmbrace = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Serpent's", "Embrace"),
                List.of(Text.literal("Soon, Tarnished").setStyle(LORE_BASE),
                        Text.literal("Wilt thou be taken in the jaws").setStyle(LORE_BASE),
                        Text.literal("Of the abyssal serpent, shorn of light").setStyle(LORE_BASE)),
                enchants);
        serpentsEmbrace.set(ModComponents.SET_ID, "fire_serpent");
        serpentsEmbrace.set(ModComponents.GRANTS_MESSMERS_FLAME, true);
        serpentsEmbrace.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CHEST_MODIFIERS);
        return serpentsEmbrace;
    }

    public static ItemStack buildKindlingsWrap(RegistryWrapper<Enchantment> enchants) {
        ItemStack kindlingsWrap = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Kindling's", "Wrap"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "Burn away all that is ",
                        Text.literal("impure").setStyle(LORE_ACCENT))),
                enchants);
        kindlingsWrap.set(ModComponents.SET_ID, "fire_serpent");
        kindlingsWrap.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGS_MODIFIERS);
        return kindlingsWrap;
    }

    public static ItemStack buildKindlingsPath(RegistryWrapper<Enchantment> enchants) {
        ItemStack kindlingsPath = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Kindling's", "Path"),
                List.of(Text.literal("Yet my purpose standeth unchanged").setStyle(LORE_BASE)),
                enchants);
        kindlingsPath.set(ModComponents.SET_ID, "fire_serpent");
        kindlingsPath.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FEET_MODIFIERS);
        return kindlingsPath;
    }
}