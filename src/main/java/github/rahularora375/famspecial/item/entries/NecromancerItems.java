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

public final class NecromancerItems {
    private NecromancerItems() {}

    // Two-tone name palette: muted-green prefix (italic), lighter-green piece (bold).
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0x6B8E5A)).withItalic(true);
    private static final Style NAME_PIECE = Style.EMPTY.withColor(TextColor.fromRgb(0xA8C49A)).withBold(true).withItalic(false);
    // Lore palette: dark-green base, crimson accent for the closing clause
    // of each lore line — the "graves open" / "others fall" punch.
    private static final Style LORE_BASE = Style.EMPTY.withColor(TextColor.fromRgb(0x5A6B4F)).withItalic(true);
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000)).withItalic(true);

    // Per-piece modifiers: each Necromancer armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) on its slot. Totals at 4/4 are +2 hearts. Vanilla diamond
    // armor/toughness is preserved by copying the entries from a freshly-built
    // vanilla stack's component — same pattern as Fire Serpent / Poseidon.
    public static final AttributeModifiersComponent HEAD_MODIFIERS  = buildNecromancerArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "necromancer_set_head");
    public static final AttributeModifiersComponent CHEST_MODIFIERS = buildNecromancerArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "necromancer_set_chest");
    public static final AttributeModifiersComponent LEGS_MODIFIERS  = buildNecromancerArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "necromancer_set_legs");
    public static final AttributeModifiersComponent FEET_MODIFIERS  = buildNecromancerArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "necromancer_set_feet");

    // Thriller's Edge: preserve the vanilla netherite-axe baseline
    // (+9 ATTACK_DAMAGE, -3.0 ATTACK_SPEED on MAINHAND) and stack an extra
    // +0.2 ATTACK_SPEED ADD_VALUE on top, bringing the weapon's net modifier
    // to -2.8 and the player's effective attack speed from 1.0 to 1.2.
    public static final AttributeModifiersComponent AXE_MODIFIERS = buildThrillersEdgeModifiers();

    private static AttributeModifiersComponent buildThrillersEdgeModifiers() {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        AttributeModifiersComponent vanilla = new ItemStack(Items.NETHERITE_AXE).get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (vanilla != null) {
            for (AttributeModifiersComponent.Entry entry : vanilla.modifiers()) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        builder.add(EntityAttributes.ATTACK_SPEED,
                new EntityAttributeModifier(
                        Identifier.of(FamSpecial.MOD_ID, "thrillers_edge_speed"),
                        0.2,
                        EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
        return builder.build();
    }

    private static AttributeModifiersComponent buildNecromancerArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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
        // Theme-adjacent — no SET_ID, so it doesn't count toward 4/4.
        // Thriller's Edge: DIAMOND_AXE base with modest enchants. The signature
        // behavior is Wither II on hit (APPLIES_WITHER_ON_HIT) — applied
        // additively by AttackHandlers' ALLOW_DAMAGE handler, covering melee
        // plus any future projectile axe-hit path. Gates on the stack flag,
        // not the tool material, so the flag is the single source of truth.
        ItemStack thrillersEdge = ModItemGroups.buildItem(Items.NETHERITE_AXE,
                twoTone("Thriller's", "Edge"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "No mere mortal can ",
                        Text.literal("resist its swing").setStyle(LORE_ACCENT))),
                enchants,
                ModItemGroups.e(Enchantments.SHARPNESS, 5));
        thrillersEdge.set(ModComponents.APPLIES_WITHER_ON_HIT, true);
        thrillersEdge.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AXE_MODIFIERS);
        entries.add(thrillersEdge);
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        // All four pieces tagged SET_ID="necromancer".
        // 4/4 set bonus is Zombie Reinforcements — on-hit spawn of 2 armored
        // zombies aggro'd on the attacker, 5-min cooldown. Event-driven, lives
        // in NecromancerSummon (AFTER_DAMAGE hook). No BONUSES entry here —
        // the summon fires on damage, not on a tick trigger.
        ItemStack fedora = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Fedora of the", "Damned"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "He tips it once. ",
                        Text.literal("The graves open").setStyle(LORE_ACCENT))),
                enchants);
        fedora.set(ModComponents.SET_ID, "necromancer");
        fedora.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HEAD_MODIFIERS);
        entries.add(fedora);

        ItemStack vestment = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Smooth Criminal's", "Vestment"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "He leans when ",
                        Text.literal("others fall").setStyle(LORE_ACCENT))),
                enchants);
        vestment.set(ModComponents.SET_ID, "necromancer");
        vestment.set(ModComponents.GRANTS_UNDEAD_RESISTANCE, true);
        vestment.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, CHEST_MODIFIERS);
        entries.add(vestment);

        ItemStack billieJeans = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Billie", "Jeans"),
                List.of(ModItemGroups.loreLine(LORE_BASE,
                        "The kid is not my son... ",
                        Text.literal("he's my minion").setStyle(LORE_ACCENT))),
                enchants);
        billieJeans.set(ModComponents.SET_ID, "necromancer");
        billieJeans.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, LEGS_MODIFIERS);
        entries.add(billieJeans);

        ItemStack moonwalker = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Moonwalker's", "Curse"),
                List.of(Text.literal("The foulest stench is in the air —").setStyle(LORE_BASE),
                        ModItemGroups.loreLine(LORE_BASE,
                                Text.literal("it's coming from his shoes").setStyle(LORE_ACCENT))),
                enchants);
        moonwalker.set(ModComponents.SET_ID, "necromancer");
        moonwalker.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, FEET_MODIFIERS);
        entries.add(moonwalker);
    }
}
