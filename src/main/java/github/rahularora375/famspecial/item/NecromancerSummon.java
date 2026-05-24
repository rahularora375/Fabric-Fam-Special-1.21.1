package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.effect.ModStatusEffects;
import github.rahularora375.famspecial.mixin.MobEntityAccessor;
import github.rahularora375.famspecial.sound.ModSounds;
import net.minecraft.sound.SoundCategory;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
import net.minecraft.entity.mob.MobEntity;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    // Hard cap on the attacker-set size per summoner. 16 is generous — more
    // than any realistic fight needs. When adding a new attacker and the set
    // is at cap, drop the oldest (FIFO) — LinkedHashSet preserves insertion
    // order so the eldest entry is the first one returned by the iterator.
    private static final int MAX_ATTACKERS_PER_SUMMONER = 16;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // summoner UUID → last-summon world-tick
    private static final Map<UUID, Long> lastSummonTick = new ConcurrentHashMap<>();
    // zombie UUID → summoner UUID (friendly-fire gate)
    private static final Map<UUID, UUID> ownerByZombie = new ConcurrentHashMap<>();
    // zombie UUID → expire world-tick
    private static final Map<UUID, Long> zombieExpireTick = new ConcurrentHashMap<>();
    // summoner UUID → cumulative set of attacker UUIDs. Read live by each
    // summoned mob's target predicate — any LivingEntity whose UUID is in
    // the set (and is alive) is a valid target. Written in onAfterDamage on
    // every hit the summoner takes, regardless of summon-cooldown state, so
    // a second hostile striking during the 5-min cooldown gets added to the
    // pool instead of replacing the first. LinkedHashSet preserves insertion
    // order for FIFO eviction at MAX_ATTACKERS_PER_SUMMONER. The value-set
    // itself is wrapped synchronizedSet because the outer map is
    // ConcurrentHashMap but the inner sets need their own synchronization
    // for iteration / mutation racing across the AI thread (predicate reads)
    // and the server-tick thread (writes from onAfterDamage / cleanup).
    private static final Map<UUID, Set<UUID>> attackersBySummoner = new ConcurrentHashMap<>();

    private NecromancerSummon() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(NecromancerSummon::onAfterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(NecromancerSummon::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(NecromancerSummon::onAfterDeath);
        ServerTickEvents.END_SERVER_TICK.register(NecromancerSummon::onEndTick);
        AttackEntityCallback.EVENT.register(NecromancerSummon::onPlayerAttack);
        FamSpecial.LOGGER.info("Registering Necromancer summon for {}", FamSpecial.MOD_ID);
    }

    // Offensive analogue to onAfterDamage's attacker-tracking block: when the
    // 4/4 wearer strikes a non-player, non-minion LivingEntity, add the target
    // to the summoner's attacker set and nudge idle zombies onto it. The
    // ActiveTargetGoal predicate reads attackersBySummoner live, so freshly
    // added UUIDs are valid targets within one AI scan even without the
    // setTarget nudge — the nudge just cuts the switch to a single tick.
    // Returns PASS so vanilla damage resolution runs normally — this is
    // observation-only, not a cancellation point. Server-side guarded via
    // ServerWorld instanceof check (callback fires on both sides).
    private static ActionResult onPlayerAttack(PlayerEntity player, net.minecraft.world.World world,
                                               Hand hand, Entity target,
                                               net.minecraft.util.hit.EntityHitResult hitResult) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;
        if (!(target instanceof LivingEntity living)) return ActionResult.PASS;
        if (living == player) return ActionResult.PASS;
        if (!ArmorEffects.hasFullSet(player, SET_ID)) return ActionResult.PASS;
        // Mirror onAfterDamage's exclusion list — never enlist another player
        // and never enlist one of this summoner's own minions.
        UUID targetUuid = living.getUuid();
        if (living instanceof PlayerEntity) {
            FamSpecial.LOGGER.info("[Necromancer-PlayerAttack] skip: target is a player ({})",
                    living.getName().getString());
            return ActionResult.PASS;
        }
        if (ownerByZombie.containsKey(targetUuid)) {
            FamSpecial.LOGGER.info("[Necromancer-PlayerAttack] skip: target is one of this summoner's minions ({})",
                    living.getType().getName().getString());
            return ActionResult.PASS;
        }

        UUID summonerUuid = player.getUuid();
        int beforeSize = attackersBySummoner.getOrDefault(summonerUuid,
                java.util.Collections.emptySet()).size();
        addAttacker(summonerUuid, targetUuid);
        int afterSize = attackersBySummoner.get(summonerUuid).size();
        FamSpecial.LOGGER.info("[Necromancer-PlayerAttack] player={} hit {} — attackers {}→{}, nudging zombies",
                player.getName().getString(), living.getType().getName().getString(),
                beforeSize, afterSize);
        nudgeIdleZombies(serverWorld.getServer(), summonerUuid, living);
        return ActionResult.PASS;
    }

    // Summoner died — discard every zombie they own and prune tracking. We
    // leave lastSummonTick alone (harmless without the set equipped, and the
    // player doesn't have the set on respawn anyway), but we DO drop the
    // attackersBySummoner entry since the next summon cycle starts fresh.
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
        attackersBySummoner.remove(summonerUuid);
    }

    // Append attackerUuid to the summoner's attacker set. Creates the set on
    // first call. FIFO eviction at MAX_ATTACKERS_PER_SUMMONER — LinkedHashSet
    // preserves insertion order, so the iterator's first element is the
    // eldest entry; remove-then-add ensures an already-present UUID gets
    // bumped to "most recent" instead of being treated as stale.
    private static void addAttacker(UUID summonerUuid, UUID attackerUuid) {
        Set<UUID> set = attackersBySummoner.computeIfAbsent(summonerUuid,
                k -> java.util.Collections.synchronizedSet(new LinkedHashSet<>()));
        synchronized (set) {
            // Re-insert to bump recency if already present.
            set.remove(attackerUuid);
            set.add(attackerUuid);
            while (set.size() > MAX_ATTACKERS_PER_SUMMONER) {
                Iterator<UUID> it = set.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                } else {
                    break;
                }
            }
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
                if (victim instanceof MobEntity m) {
                    m.setTarget(null);
                }
            }
        }

        if (!(victim instanceof ServerPlayerEntity player)) return;
        if (!player.isAlive()) return;
        if (!ArmorEffects.hasFullSet(player, SET_ID)) return;
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (attacker == player) return;
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        // Always append the new attacker to the cumulative set + nudge idle
        // minions onto them, regardless of cooldown — so a subsequent hostile
        // striking during the 5-min summon cooldown still pulls existing
        // minions onto them while not yanking mobs already engaging a live,
        // in-set target. Guards: never add the summoner themselves (caught
        // above by `attacker == player`), never add another player, and never
        // add one of the summoner's own minions (would cause them to attack
        // each other if the friendly-fire rules ever break).
        UUID summonerUuid = player.getUuid();
        UUID attackerUuid = attacker.getUuid();
        if (!(attacker instanceof PlayerEntity) && !ownerByZombie.containsKey(attackerUuid)) {
            addAttacker(summonerUuid, attackerUuid);
            nudgeIdleZombies(world.getServer(), summonerUuid, attacker);
        }

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

    // Sweep every tracked mob owned by this summoner and point IDLE ones at
    // the new attacker. "Idle" = current target is null, dead, or not in the
    // summoner's attacker set anymore. Mobs already engaging a live, in-set
    // target are left alone — yanking them off a current valid target every
    // time the summoner takes a new hit would make focus-fire impossible.
    // The ActiveTargetGoal predicate reads attackersBySummoner live so it
    // will eventually re-aim on its own scan cadence, but a direct setTarget
    // here cuts the switch for idle mobs down to one tick.
    private static void nudgeIdleZombies(net.minecraft.server.MinecraftServer server,
                                         UUID summonerUuid, LivingEntity newAttacker) {
        Set<UUID> validAttackers = attackersBySummoner.get(summonerUuid);
        for (Map.Entry<UUID, UUID> entry : ownerByZombie.entrySet()) {
            if (!entry.getValue().equals(summonerUuid)) continue;
            UUID zid = entry.getKey();
            for (ServerWorld world : server.getWorlds()) {
                Entity found = world.getEntity(zid);
                if (found instanceof MobEntity m && m.isAlive()) {
                    LivingEntity current = m.getTarget();
                    boolean idle = current == null || !current.isAlive()
                            || validAttackers == null
                            || !validAttackers.contains(current.getUuid());
                    if (idle) {
                        m.setTarget(newAttacker);
                    }
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
                UUID summonerUuid = ownerByZombie.remove(zid);
                // If that was the last mob owned by this summoner, drop their
                // attacker-set entry too — next summon cycle starts fresh.
                if (summonerUuid != null && !ownerByZombie.containsValue(summonerUuid)) {
                    attackersBySummoner.remove(summonerUuid);
                }
            }
        }
    }

    private static int spawnZombies(ServerWorld world, ServerPlayerEntity summoner, LivingEntity attacker) {
        BlockPos origin = summoner.getBlockPos();
        int total = 0;

        total += spawnType(world, origin, summoner, attacker, EntityType.ZOMBIE, SPAWN_COUNT);
        total += spawnType(world, origin, summoner, attacker, EntityType.WITHER_SKELETON, 1);
        total += spawnType(world, origin, summoner, attacker, EntityType.ZOMBIFIED_PIGLIN, 1);

        return total;
    }

    // Run up to SPAWN_ATTEMPTS positional tries for a single mob type, spawning
    // up to `target` of that type. Each type gets its own attempt budget so a
    // bad-position streak for one type doesn't starve the others.
    private static int spawnType(ServerWorld world, BlockPos origin, ServerPlayerEntity summoner,
                                 LivingEntity attacker, EntityType<? extends MobEntity> type, int target) {
        int spawned = 0;
        int posRejects = 0, createNull = 0, spaceRejects = 0;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS && spawned < target; attempt++) {
            int[] outcome = new int[]{0, 0, 0}; // posRejects, createNull, spaceRejects
            if (trySpawnOne(world, origin, summoner, attacker, type, outcome)) {
                spawned++;
            } else {
                posRejects += outcome[0];
                createNull += outcome[1];
                spaceRejects += outcome[2];
            }
        }

        if (spawned < target) {
            FamSpecial.LOGGER.warn("[Necromancer] spawn shortfall for {} at origin={} — got {}/{}, pos_rejects={}, create_nulls={}, space_rejects={}",
                    type.getName().getString(), origin, spawned, target, posRejects, createNull, spaceRejects);
        }
        return spawned;
    }

    private static boolean trySpawnOne(ServerWorld world, BlockPos origin, ServerPlayerEntity summoner,
                                       LivingEntity attacker, EntityType<? extends MobEntity> type, int[] outcome) {
        Random rng = world.random;
        int dx = MathHelper.nextInt(rng, -HORIZONTAL_RADIUS, HORIZONTAL_RADIUS);
        int dy = MathHelper.nextInt(rng, -VERTICAL_RADIUS, VERTICAL_RADIUS);
        int dz = MathHelper.nextInt(rng, -HORIZONTAL_RADIUS, HORIZONTAL_RADIUS);
        BlockPos bp = origin.add(dx, dy, dz);

        // isSpawnPosAllowed = surface/ground check. We keep it so the mob
        // spawns on a walkable block. The full canSpawn predicate is *skipped*
        // because vanilla's hostile canSpawn runs HostileEntity.canSpawnInDark,
        // which fails any time the sky-light level is > 7 — i.e. daylight.
        // For a 4/4 set bonus the summon must fire regardless of lighting, so
        // we accept the position based on ground + collision only.
        if (!SpawnRestriction.isSpawnPosAllowed(type, world, bp)) { outcome[0]++; return false; }

        MobEntity mob = type.create(world, SpawnReason.REINFORCEMENT);
        if (mob == null) { outcome[1]++; return false; }
        mob.refreshPositionAndAngles(bp, 0.0f, 0.0f);
        if (!world.isSpaceEmpty(mob) || !world.doesNotIntersectEntities(mob)) { outcome[2]++; return false; }

        mob.initialize(world, world.getLocalDifficulty(bp), SpawnReason.REINFORCEMENT, null);
        equipArmor(mob, world);
        lockTargetToAttacker(mob, summoner.getUuid(), attacker);
        // Persistent so the chunk-unload / despawn timer can't cull them
        // before our explicit 5-min expiry fires.
        mob.setPersistent();

        world.spawnEntityAndPassengers(mob);
        ownerByZombie.put(mob.getUuid(), summoner.getUuid());
        zombieExpireTick.put(mob.getUuid(), world.getTime() + ZOMBIE_LIFETIME_TICKS);
        return true;
    }

    // Lock target onto any entity whose UUID is currently in the summoner's
    // attackersBySummoner set. Vanilla zombies carry
    // ActiveTargetGoal<PlayerEntity> + ActiveTargetGoal<VillagerEntity>
    // etc., each of which re-scans every tick and retargets the nearest
    // valid entity — which is the summoner standing next to the newly
    // spawned zombie, so setTarget gets overwritten within a few ticks.
    // Clear every ActiveTargetGoal and install a single predicate-gated
    // goal whose filter reads the summoner's attacker set live each tick;
    // updating attackersBySummoner in onAfterDamage implicitly grows the
    // pool of valid targets for every active mob owned by that summoner.
    // RevengeGoal is intentionally left in place so hitting a summoned mob
    // still triggers retaliation — the friendly-fire ALLOW_DAMAGE gate
    // cancels the damage back to the summoner but doesn't block
    // retargeting, and retaliation against non-summoner attackers is fine.
    private static void lockTargetToAttacker(MobEntity mob, UUID summonerUuid, LivingEntity initialAttacker) {
        var targetSelector = ((MobEntityAccessor) mob).famspecial$getTargetSelector();
        targetSelector.clear(goal -> goal instanceof ActiveTargetGoal<?>);
        targetSelector.add(2, new ActiveTargetGoal<>(mob, LivingEntity.class, 10, false, false,
                (entity, world) -> {
                    if (entity == null || !entity.isAlive()) return false;
                    Set<UUID> attackers = attackersBySummoner.get(summonerUuid);
                    if (attackers == null) return false;
                    synchronized (attackers) {
                        return attackers.contains(entity.getUuid());
                    }
                }));
        mob.setTarget(initialAttacker);
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

    private static void equipArmor(MobEntity mob, ServerWorld world) {
        mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));

        ItemStack chestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
        RegistryEntry<Enchantment> protection = world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.PROTECTION);
        ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        builder.set(protection, 1);
        chestplate.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        mob.equipStack(EquipmentSlot.CHEST, chestplate);

        mob.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        mob.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));

        Item swordTier = SWORD_TIERS[world.random.nextInt(SWORD_TIERS.length)];
        mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(swordTier));
        mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            mob.setEquipmentDropChance(slot, 0.0f);
        }
    }
}
