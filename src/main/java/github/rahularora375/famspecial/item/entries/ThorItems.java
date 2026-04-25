package github.rahularora375.famspecial.item.entries;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.item.MjolnirMaceItem;
import github.rahularora375.famspecial.item.ModItemGroups;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

// Thor / God of Thunder theme. 4-piece diamond armor set + 1 signature mace
// (Mjolnir). No per-piece attribute modifiers — vanilla diamond base is
// preserved unchanged. The 5/5 gate (armor + Mjolnir in main hand) is what
// unlocks the right-click riptide launch; each individual mechanic has its
// own gate:
//   - Mjolnir held in main hand: grants GOD_OF_THUNDER (cosmetic HUD badge).
//     On every successful melee hit, ThorEffects.AFTER_DAMAGE rolls 20%
//     (clear weather) or 100% (thundering) to strike the victim with a
//     vanilla LIGHTNING_BOLT. Gated on the attacker's mainhand carrying
//     LIGHTNING_ON_HIT.
//   - Warrior's Greaves alone: drives STORMS_AWAKENING (cosmetic HUD badge,
//     only visible while off cooldown). On any kill the wearer performs, if
//     the world is not already thundering, roll 8% to start a thunderstorm.
//     Per-player 1-day cooldown only starts on a successful trigger — if
//     already thundering the kill is skipped entirely (no roll, no
//     cooldown burn).
//   - All 4 armor pieces + Mjolnir in main hand (5/5): right-clicking Mjolnir
//     fires a vanilla Riptide launch (Level 3 equivalent) — no water / rain
//     requirement, no enchant needed. Handled in ThorEffects via
//     UseItemCallback.EVENT (MaceItem doesn't declare 'use' — it inherits
//     from Item — so a mixin on MaceItem would mis-target).
public final class ThorItems {
    private ThorItems() {}

    // Mjolnir is a custom MaceItem subclass that owns the right-click riptide
    // behavior (use/getUseAction/getMaxUseTime/onStoppedUsing). Registered as
    // a real mod item during FamSpecial#onInitialize via register() below;
    // constructing Item subclasses at class-load (static-final initializer)
    // crashes in 1.21+ because SimpleRegistry rejects intrusive holders and
    // requires a RegistryKey on the Settings. Non-final: populated by
    // register().
    public static MjolnirMaceItem MJOLNIR;

    // Builds Settings that mirror how vanilla MACE is registered (see
    // net.minecraft.item.Items mace bytecode): EPIC rarity, 500 max damage,
    // MaceItem tool + weapon components, Breeze-rod repairable, level-15
    // enchantable, plus MaceItem attribute modifiers so the base damage
    // scaling and attack speed match vanilla. This is the ONLY reason
    // Mjolnir needs to be a real mod item — subclassing MaceItem with a
    // plain new Item.Settings() would strip all of these.
    public static void register() {
        Identifier id = Identifier.of(FamSpecial.MOD_ID, "mjolnir");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .rarity(Rarity.EPIC)
                .maxDamage(500)
                .component(DataComponentTypes.TOOL, MaceItem.createToolComponent())
                .repairable(Items.BREEZE_ROD)
                .attributeModifiers(MaceItem.createAttributeModifiers())
                .enchantable(15)
                .component(DataComponentTypes.WEAPON, new WeaponComponent(1));
        MJOLNIR = Registry.register(Registries.ITEM, id, new MjolnirMaceItem(settings));
    }

    // Two-tone name palette: storm-purple prefix (italic), pale lightning-
    // white piece (bold). Pure white-blue reads too close to Knight Radiant's
    // icy palette, so we lean on purple/violet for the prefix to anchor the
    // "god of thunder" aesthetic.
    private static final Style NAME_PREFIX = Style.EMPTY.withColor(TextColor.fromRgb(0x8870D8)).withItalic(true);
    private static final Style NAME_PIECE  = Style.EMPTY.withColor(TextColor.fromRgb(0xE0D8FF)).withBold(true).withItalic(false);
    // Lore palette: deep storm-purple base, lightning-cyan accent for
    // emphasis words.
    private static final Style LORE_BASE   = Style.EMPTY.withColor(TextColor.fromRgb(0x6858B8)).withItalic(true);
    @SuppressWarnings("unused")
    private static final Style LORE_ACCENT = Style.EMPTY.withColor(TextColor.fromRgb(0x60C8FF)).withItalic(true);

    private static Text twoTone(String prefix, String piece) {
        return Text.literal(prefix).setStyle(NAME_PREFIX)
                .append(Text.literal(" "))
                .append(Text.literal(piece).setStyle(NAME_PIECE));
    }

    // Per-piece modifiers: every Thor armor piece grants +1 MAX_HEALTH
    // (= 0.5 heart) on its slot. Totals at 4/4 are +2 hearts. Vanilla
    // diamond armor/toughness is preserved by copying the entries from a
    // freshly-built vanilla stack's component. The merged elytra inherits
    // this automatically via AnvilScreenHandlerMixin's attribute-filter
    // path (MAX_HEALTH is not in the filter's skip list).
    private static final AttributeModifiersComponent HELM_MODIFIERS    = buildThorArmor(Items.DIAMOND_HELMET,     AttributeModifierSlot.HEAD,  "thor_set_head");
    private static final AttributeModifiersComponent PLATE_MODIFIERS   = buildThorArmor(Items.DIAMOND_CHESTPLATE, AttributeModifierSlot.CHEST, "thor_set_chest");
    private static final AttributeModifiersComponent GREAVES_MODIFIERS = buildThorArmor(Items.DIAMOND_LEGGINGS,   AttributeModifierSlot.LEGS,  "thor_set_legs");
    private static final AttributeModifiersComponent STRIDER_MODIFIERS = buildThorArmor(Items.DIAMOND_BOOTS,      AttributeModifierSlot.FEET,  "thor_set_feet");

    private static AttributeModifiersComponent buildThorArmor(Item baseItem, AttributeModifierSlot slot, String idSuffix) {
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
        entries.add(buildMjolnir(enchants));
    }

    public static void addTools(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) { }

    public static void addArmor(ItemGroup.Entries entries, RegistryWrapper<Enchantment> enchants) {
        entries.add(buildThunderHelm(enchants));
        entries.add(buildAsgardianPlate(enchants));
        entries.add(buildWarriorsGreaves(enchants));
        entries.add(buildStormStrider(enchants));
    }

    // Mjolnir — signature mace. Theme-adjacent weapon — no SET_ID on the
    // weapon itself (SET_ID lives on armor). Carries LIGHTNING_ON_HIT
    // (drives the cosmetic GOD_OF_THUNDER badge via ArmorEffects' mainhand
    // bonus + the AFTER_DAMAGE lightning roll in ThorEffects) and
    // THOR_MACE (the marker that identifies this specific weapon as
    // Mjolnir for the 5/5 gate — distinct from the hit-trigger so the
    // two flags could be split later onto other Thor-themed weapons
    // without making them all count as Mjolnir). Fixed enchant: Riptide
    // III — vanilla mace does not accept Riptide via the table, so it's
    // pre-stamped via DataComponentTypes.ENCHANTMENTS (what buildItem
    // does for all themed weapons). The right-click launch in
    // MjolnirMaceItem#onStoppedUsing reads this level via
    // EnchantmentHelper.getTridentSpinAttackStrength to scale launch
    // magnitude. Anvil-side Riptide books are also accepted via the
    // famspecial:riptide_eligible tag override (Riptide's own
    // max_level=3 caps the result).
    public static ItemStack buildMjolnir(RegistryWrapper<Enchantment> enchants) {
        ItemStack mjolnir = ModItemGroups.buildItem(MJOLNIR,
                Text.literal("Mjolnir").setStyle(NAME_PIECE),
                List.of(Text.literal("Whosoever holds this hammer,").setStyle(LORE_BASE),
                        Text.literal("if they be worthy,").setStyle(LORE_BASE),
                        Text.literal("shall possess the power of Thor").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.RIPTIDE, 3));
        mjolnir.set(ModComponents.LIGHTNING_ON_HIT, true);
        mjolnir.set(ModComponents.THOR_MACE, true);
        return mjolnir;
    }

    // All four pieces tagged SET_ID="thor". No per-piece attribute
    // modifiers — vanilla diamond armor/toughness is unchanged. The 5/5
    // gate (armor + Mjolnir in main hand) unlocks the right-click
    // riptide launch in ThorEffects.
    //
    // Warrior's Greaves additionally carries TRIGGERS_STORM_AWAKENING —
    // the piece-alone kill-triggered thunderstorm mechanic, gated on the
    // per-player ThorEffects.isStormCooldown helper so the cosmetic
    // STORMS_AWAKENING badge flips off during the 1-day cooldown window.
    public static ItemStack buildThunderHelm(RegistryWrapper<Enchantment> enchants) {
        ItemStack thunderhelm = ModItemGroups.buildItem(Items.DIAMOND_HELMET,
                twoTone("Thunder", "Helm"),
                List.of(Text.literal("He who commands the storm").setStyle(LORE_BASE),
                        Text.literal("needs no crown").setStyle(LORE_BASE)),
                enchants);
        thunderhelm.set(ModComponents.SET_ID, "thor");
        thunderhelm.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, HELM_MODIFIERS);
        return thunderhelm;
    }

    public static ItemStack buildAsgardianPlate(RegistryWrapper<Enchantment> enchants) {
        ItemStack asgardianPlate = ModItemGroups.buildItem(Items.DIAMOND_CHESTPLATE,
                twoTone("Asgardian", "Plate"),
                List.of(Text.literal("Forged in the heart").setStyle(LORE_BASE),
                        Text.literal("of a dying star").setStyle(LORE_BASE)),
                enchants);
        asgardianPlate.set(ModComponents.SET_ID, "thor");
        asgardianPlate.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, PLATE_MODIFIERS);
        return asgardianPlate;
    }

    public static ItemStack buildWarriorsGreaves(RegistryWrapper<Enchantment> enchants) {
        ItemStack warriorsGreaves = ModItemGroups.buildItem(Items.DIAMOND_LEGGINGS,
                twoTone("Warrior's", "Greaves"),
                List.of(Text.literal("A god does not tire").setStyle(LORE_BASE),
                        Text.literal("he endures").setStyle(LORE_BASE)),
                enchants);
        warriorsGreaves.set(ModComponents.SET_ID, "thor");
        warriorsGreaves.set(ModComponents.TRIGGERS_STORM_AWAKENING, true);
        warriorsGreaves.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, GREAVES_MODIFIERS);
        return warriorsGreaves;
    }

    public static ItemStack buildStormStrider(RegistryWrapper<Enchantment> enchants) {
        ItemStack stormstrider = ModItemGroups.buildItem(Items.DIAMOND_BOOTS,
                twoTone("Storm", "Strider"),
                List.of(Text.literal("The bifrost beneath his feet,").setStyle(LORE_BASE),
                        Text.literal("the nine realms at his back").setStyle(LORE_BASE)),
                enchants,
                ModItemGroups.e(Enchantments.FEATHER_FALLING, 4));
        stormstrider.set(ModComponents.SET_ID, "thor");
        stormstrider.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, STRIDER_MODIFIERS);
        return stormstrider;
    }
}
