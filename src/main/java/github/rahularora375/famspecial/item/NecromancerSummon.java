package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.mixin.MobEntityAccessor;
import github.rahularora375.famspecial.sound.ModSounds;
import net.minecraft.sound.SoundCategory;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Zombie Reinforcements — the 4/4 Necromancer set bonus. When the full-set
// wearer takes damage from a living attacker, spawn 2 zombies equipped with
// a fixed turtle-helmet + leather loadout, aggro'd on the attacker. 5-min
// cooldown.
// Spawn loop mirrors vanilla ZombieEntity.damage's reinforcement mechanic —
// a 50-attempt random-position loop gated on SpawnRestriction.isSpawnPosAllowed
// + canSpawn + world.isSpaceEmpty — but scoped to a tight 3-block horizontal
// radius (vanilla's 7-40 is for village-scale reinforcements; here we want
// minions beside the wearer).
//
// Three hooks:
//   1. AFTER_DAMAGE — the summon trigger. Fires after mitigation, only when
//      the victim survived, and carries the post-mitigation DamageSource.
//   2. ALLOW_DAMAGE — friendly-fire gate. Cancels damage from a tracked
//      summoned zombie back onto its own summoner.
//   3. END_SERVER_TICK — cleanup pass (every 20 ticks). Despawns zombies
//      after a 30s lifetime and prunes map entries for zombies killed by
//      other means.
//
// Equipment drop chance is forced to 0.0 on every slot so killing a summoned
// zombie never yields duplicate Necromancer gear.
public final class NecromancerSummon {
    private static final long COOLDOWN_TICKS = 20L * 60L * 5L;      // 5 minutes
    private static final long ZOMBIE_LIFETIME_TICKS = 20L * 60L * 5L; // 5 minutes
    private static final int SPAWN_ATTEMPTS = 50;
    private static final int SPAWN_COUNT = 2;
    private static final int HORIZONTAL_RADIUS = 3;
    private static final int VERTICAL_RADIUS = 1;
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private static final String SET_ID = "necromancer";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // summoner UUID → last-summon world-tick
    private static final Map<UUID, Long> lastSummonTick = new ConcurrentHashMap<>();
    // zombie UUID → summoner UUID (friendly-fire gate)
    private static final Map<UUID, UUID> ownerByZombie = new ConcurrentHashMap<>();
    // zombie UUID → expire world-tick
    private static final Map<UUID, Long> zombieExpireTick = new ConcurrentHashMap<>();
    // summoner UUID → most-recent attacker UUID. Read live by each summoned
    // zombie's target predicate, so updating this retargets every one of that
    // summoner's active zombies on the next AI tick. Written in onAfterDamage
    // on every hit the summoner takes, regardless of summon-cooldown state —
    // so a second hostile striking during the 5-min cooldown still pulls the
    // existing minions onto it instead of leaving them locked on the first.
    private static final Map<UUID, UUID> latestAttackerBySummoner = new ConcurrentHashMap<>();

    private NecromancerSummon() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(NecromancerSummon::onAfterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(NecromancerSummon::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(NecromancerSummon::onAfterDeath);
        ServerTickEvents.END_SERVER_TICK.register(NecromancerSummon::onEndTick);
        FamSpecial.LOGGER.info("Registering Necromancer summon for {}", FamSpecial.MOD_ID);
    }

    // Summoner died — discard every zombie they own and prune tracking. We
    // leave lastSummonTick / latestAttackerBySummoner alone; they're harmless
    // without the set equipped, and the player doesn't have the set on
    // respawn anyway.
    private static void onAfterDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource source) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        net.minecraft.server.MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;
        UUID summonerUuid = player.getUuid();

        Iterator<Map.Entry<UUID, UUID>> iter = ownerByZombie.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, UUID> entry = iter.next();
            if (!entry.getValue().equals(summonerUuid)) continue;
            UUID zid = entry.getKey();
            for (ServerWorld world : server.getWorlds()) {
                Entity found = world.getEntity(zid);
                if (found != null) {
                    found.discard();
                    break;
                }
            }
            iter.remove();
            zombieExpireTick.remove(zid);
        }
    }

    private static void onAfterDamage(LivingEntity victim, net.minecraft.entity.damage.DamageSource source,
                                      float baseDamage, float damageTaken, boolean blocked) {
        // Summoner-hits-own-zombie path: clear the zombie's attacker + target
        // fields so RevengeGoal doesn't retarget the summoner. RevengeGoal's
        // shouldBegin/shouldContinue short-circuits once attacker is null, and
        // setTarget(null) forces an immediate drop; the predicate-gated
        // ActiveTargetGoal we installed at spawn then re-acquires the original
        // hostile on the next scan. Damage itself is NOT cancelled here — the
        // summoner can still kill their own minions if they choose.
        Entity attackerEntity = source.getAttacker();
        if (attackerEntity != null) {
            UUID ownerOfVictim = ownerByZombie.get(victim.getUuid());
            if (ownerOfVictim != null && attackerEntity.getUuid().equals(ownerOfVictim)) {
                victim.setAttacker(null);
                if (victim instanceof ZombieEntity z) {
                    z.setTarget(null);
                }
            }
        }

        if (!(victim instanceof ServerPlayerEntity player)) return;
        if (!player.isAlive()) return;
        if (!ArmorEffects.hasFullSet(player, SET_ID)) return;
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (attacker == player) return;
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        // Always refresh the latest-attacker pointer + retarget active zombies,
        // regardless of cooldown — so a subsequent hostile striking during the
        // 5-min summon cooldown still pulls existing minions onto them.
        UUID summonerUuid = player.getUuid();
        latestAttackerBySummoner.put(summonerUuid, attacker.getUuid());
        retargetActiveZombies(world.getServer(), summonerUuid, attacker);

        long now = world.getTime();
        Long last = lastSummonTick.get(summonerUuid);
        if (last != null && now - last < COOLDOWN_TICKS) {
            FamSpecial.LOGGER.info("[Necromancer] hit by {} — on cooldown, {} ticks remaining",
                    attacker.getType().getName().getString(), COOLDOWN_TICKS - (now - last));
            return;
        }

        FamSpecial.LOGGER.info("[Necromancer] hit by {} — summon charged, attempting spawn (lastSummon={}, now={})",
                attacker.getType().getName().getString(), last, now);
        int spawned = spawnZombies(world, player, attacker);
        FamSpecial.LOGGER.info("[Necromancer] spawnZombies returned {}", spawned);
        if (spawned > 0) {
            lastSummonTick.put(summonerUuid, now);
            stampCooldownOnWornSet(player, now + COOLDOWN_TICKS);
            // Null-source play so everyone in range hears it, including the
            // summoner. HOSTILE category sits on the Hostile Creatures volume
            // slider — matches the zombie-reinforcement theme.
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.NECROMANCER_SUMMON, SoundCategory.HOSTILE, 1.0f, 1.0f);
            // Rotten Muscle is re-applied with 20s duration every 4s from
            // ArmorEffects while the 4/4 set is worn AND the cooldown is
            // clear — once the cooldown kicks in here the refresh stops,
            // but the last 20s duration still has to tick out before the
            // badge disappears. Strip it immediately so the HUD state
            // flips the instant the summon fires.
            player.removeStatusEffect(ModStatusEffects.ROTTEN_MUSCLE);
        }
    }

    // Stamp the absolute cooldown-end tick onto every worn Necromancer piece.
    // Client tooltips read this component to render the MM:SS countdown; the
    // server keeps using its own per-UUID map (stamping is a display-sync
    // mechanism, not the cooldown source of truth). The stamp stays after
    // expiry — the tooltip just hides once currentTime >= cooldownEnd.
    private static void stampCooldownOnWornSet(ServerPlayerEntity player, long cooldownEndTick) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (SET_ID.equals(stack.get(ModComponents.SET_ID))) {
                stack.set(ModComponents.NECROMANCER_COOLDOWN_END, cooldownEndTick);
            }
        }
    }

    // Sweep every tracked zombie owned by this summoner and point it at the
    // new attacker. The ActiveTargetGoal predicate reads latestAttackerBySummoner
    // live so the predicate itself already matches the new UUID — but the
    // zombie's currently-held target field still points at the previous one,
    // and the goal's shouldContinue won't release it until the next scan. A
    // direct setTarget call cuts the switch down to one tick.
    private static void retargetActiveZombies(net.minecraft.server.MinecraftServer server,
                                              UUID summonerUuid, LivingEntity newAttacker) {
        for (Map.Entry<UUID, UUID> entry : ownerByZombie.entrySet()) {
            if (!entry.getValue().equals(summonerUuid)) continue;
            UUID zid = entry.getKey();
            for (ServerWorld world : server.getWorlds()) {
                Entity found = world.getEntity(zid);
                if (found instanceof ZombieEntity z && z.isAlive()) {
                    z.setTarget(newAttacker);
                    break;
                }
            }
        }
    }

    private static boolean onAllowDamage(LivingEntity victim, net.minecraft.entity.damage.DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker == null) return true;
        // Zombie hitting its summoner — cancel. Summoner hitting their own
        // zombie is NOT cancelled (the summoner can kill minions if they
        // want); the aggro-back is suppressed separately in onAfterDamage
        // by clearing the zombie's attacker + target fields.
        UUID ownerOfAttacker = ownerByZombie.get(attacker.getUuid());
        if (ownerOfAttacker != null && victim.getUuid().equals(ownerOfAttacker)) return false;
        return true;
    }

    // Public helper consumed by ArmorEffects' rotten_muscle_ready bonus
    // trigger and by tooltips — returns whether the summon mechanic is
    // currently on cooldown for the given player (i.e. the 4/4 set was worn,
    // took a hit, and the 5-minute timer hasn't elapsed). Used by the Rotten
    // Muscle status effect gate so the badge disappears during the cooldown
    // window and re-appears when the summon is charged again.
    public static boolean isOnCooldown(PlayerEntity player, long worldTime) {
        Long last = lastSummonTick.get(player.getUuid());
        return last != null && worldTime - last < COOLDOWN_TICKS;
    }

    private static void onEndTick(net.minecraft.server.MinecraftServer server) {
        if (server.getTicks() % CLEANUP_INTERVAL_TICKS != 0) return;
        long now = server.getOverworld().getTime();

        Iterator<Map.Entry<UUID, Long>> iter = zombieExpireTick.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Long> entry = iter.next();
            UUID zid = entry.getKey();
            long expireAt = entry.getValue();

            Entity found = null;
            for (ServerWorld world : server.getWorlds()) {
                found = world.getEntity(zid);
                if (found != null) break;
            }

            // If we can't locate the entity, it's either gone (killed off-tick
            // or discarded externally) or in an unloaded chunk. Pruning tracking
            // right away would orphan a setPersistent zombie whose chunk later
            // reloads — the player could ferry it into an unloaded area to
            // permanently escape cleanup. Keep the tracking entry until its
            // expire tick passes; only prune once we're past lifetime (at which
            // point any rehydrated zombie is beyond the mechanic's window and
            // we accept the rare ghost).
            boolean cleanup;
            if (found == null) {
                cleanup = now >= expireAt;
            } else if (!found.isAlive()) {
                cleanup = true;
            } else if (now >= expireAt) {
                found.discard();
                cleanup = true;
            } else {
                cleanup = false;
            }

            if (cleanup) {
                iter.remove();
                ownerByZombie.remove(zid);
            }
        }
    }

    private static int spawnZombies(ServerWorld world, ServerPlayerEntity summoner, LivingEntity attacker) {
        BlockPos origin = summoner.getBlockPos();
        Random rng = world.random;
        int spawned = 0;
        int posRejects = 0, createNull = 0, spaceRejects = 0;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS && spawned < SPAWN_COUNT; attempt++) {
            int dx = MathHelper.nextInt(rng, -HORIZONTAL_RADIUS, HORIZONTAL_RADIUS);
            int dy = MathHelper.nextInt(rng, -VERTICAL_RADIUS, VERTICAL_RADIUS);
            int dz = MathHelper.nextInt(rng, -HORIZONTAL_RADIUS, HORIZONTAL_RADIUS);
            BlockPos bp = origin.add(dx, dy, dz);

            // isSpawnPosAllowed = surface/ground check. We keep it so the zombie
            // spawns on a walkable block. The full canSpawn predicate is *skipped*
            // because vanilla's ZombieEntity.canSpawn runs HostileEntity.canSpawnInDark,
            // which fails any time the sky-light level is > 7 — i.e. daylight.
            // For a 4/4 set bonus the summon must fire regardless of lighting, so
            // we accept the position based on ground + collision only.
            if (!SpawnRestriction.isSpawnPosAllowed(EntityType.ZOMBIE, world, bp)) { posRejects++; continue; }

            ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.REINFORCEMENT);
            if (zombie == null) { createNull++; continue; }
            zombie.refreshPositionAndAngles(bp, 0.0f, 0.0f);
            if (!world.isSpaceEmpty(zombie) || !world.doesNotIntersectEntities(zombie)) { spaceRejects++; continue; }

            zombie.initialize(world, world.getLocalDifficulty(bp), SpawnReason.REINFORCEMENT, null);
            equipArmor(zombie, world);
            lockTargetToAttacker(zombie, summoner.getUuid(), attacker);
            // Persistent so the chunk-unload / despawn timer can't cull them
            // before our explicit 30s expiry fires.
            zombie.setPersistent();

            world.spawnEntityAndPassengers(zombie);
            ownerByZombie.put(zombie.getUuid(), summoner.getUuid());
            zombieExpireTick.put(zombie.getUuid(), world.getTime() + ZOMBIE_LIFETIME_TICKS);
            spawned++;
        }

        if (spawned == 0) {
            FamSpecial.LOGGER.warn("[Necromancer] spawn failed at origin={} — pos_rejects={}, create_nulls={}, space_rejects={}",
                    origin, posRejects, createNull, spaceRejects);
        }
        return spawned;
    }

    // Lock target onto whichever entity is currently in
    // latestAttackerBySummoner for this summoner. Vanilla zombies carry
    // ActiveTargetGoal<PlayerEntity> + ActiveTargetGoal<VillagerEntity>
    // etc., each of which re-scans every tick and retargets the nearest
    // valid entity — which is the summoner standing next to the newly
    // spawned zombie, so setTarget gets overwritten within a few ticks.
    // Clear every ActiveTargetGoal and install a single predicate-gated
    // goal whose filter reads the summoner's latest-attacker UUID live
    // each tick; updating latestAttackerBySummoner in onAfterDamage
    // implicitly re-aims every active zombie for that summoner at the
    // new hostile. RevengeGoal is intentionally left in place so hitting
    // a summoned zombie still triggers retaliation — the friendly-fire
    // ALLOW_DAMAGE gate cancels the damage back to the summoner but
    // doesn't block retargeting, and retaliation against non-summoner
    // attackers is fine.
    private static void lockTargetToAttacker(ZombieEntity zombie, UUID summonerUuid, LivingEntity initialAttacker) {
        var targetSelector = ((MobEntityAccessor) zombie).famspecial$getTargetSelector();
        targetSelector.clear(goal -> goal instanceof ActiveTargetGoal<?>);
        targetSelector.add(2, new ActiveTargetGoal<>(zombie, LivingEntity.class, 10, false, false,
                (entity, world) -> {
                    if (entity == null) return false;
                    UUID latest = latestAttackerBySummoner.get(summonerUuid);
                    return latest != null && entity.getUuid().equals(latest);
                }));
        zombie.setTarget(initialAttacker);
    }

    // Fixed minion loadout: turtle helmet + leather chestplate (Protection I)
    // + leather leggings + leather boots, plus a per-zombie RNG sword chosen
    // uniformly from {wood, stone, iron, gold, diamond, netherite} with no
    // enchants. Drop chance is zeroed on every slot — armor + mainhand — so
    // kills never yield duplicate gear and the netherite-sword path can't be
    // farmed.
    private static final Item[] SWORD_TIERS = {
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
            Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
    };

    private static void equipArmor(ZombieEntity zombie, ServerWorld world) {
        zombie.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));

        ItemStack chestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
        RegistryEntry<Enchantment> protection = world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.PROTECTION);
        ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        builder.set(protection, 1);
        chestplate.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        zombie.equipStack(EquipmentSlot.CHEST, chestplate);

        zombie.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        zombie.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));

        Item swordTier = SWORD_TIERS[world.random.nextInt(SWORD_TIERS.length)];
        zombie.equipStack(EquipmentSlot.MAINHAND, new ItemStack(swordTier));
        zombie.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            zombie.setEquipmentDropChance(slot, 0.0f);
        }
    }
}
