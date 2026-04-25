package github.rahularora375.famspecial.item;

import github.rahularora375.famspecial.FamSpecial;
import github.rahularora375.famspecial.component.ModComponents;
import github.rahularora375.famspecial.entity.VolleyArrowEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FortuneGloryItem extends CrossbowItem {
    private static final int SHOT_2_DELAY_TICKS = 2;
    private static final int SHOT_3_DELAY_TICKS = 4;
    private static final float CROSSBOW_PROJECTILE_SPEED = 3.15F;
    private static final float FIREWORK_ROCKET_SPEED = 1.6F;

    public static FortuneGloryItem FORTUNE_AND_GLORY;

    private static final ConcurrentLinkedQueue<ScheduledShot> PENDING_SHOTS = new ConcurrentLinkedQueue<>();

    public FortuneGloryItem(Settings settings) { super(settings); }

    public static void register() {
        if (FORTUNE_AND_GLORY != null) return;
        Identifier id = Identifier.of(FamSpecial.MOD_ID, "fortune_and_glory");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(1)
                .maxDamage(465)
                .rarity(Rarity.EPIC)
                .component(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT)
                .enchantable(1);
        FORTUNE_AND_GLORY = Registry.register(Registries.ITEM, id, new FortuneGloryItem(settings));
    }

    public static void registerTickHandler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING_SHOTS.isEmpty()) return;
            long now = server.getOverworld().getTime();
            Iterator<ScheduledShot> iter = PENDING_SHOTS.iterator();
            while (iter.hasNext()) {
                ScheduledShot scheduled = iter.next();
                if (scheduled.targetTick() > now) continue;
                iter.remove();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(scheduled.playerUuid());
                if (player == null || !player.isAlive()) continue;
                if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) continue;
                fireScheduledShot(serverWorld, player, scheduled.crossbowStack(), scheduled.projectileStack());
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PENDING_SHOTS.removeIf(s -> s.playerUuid().equals(handler.getPlayer().getUuid())));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                PENDING_SHOTS.removeIf(s -> s.playerUuid().equals(p.getUuid()));
            }
        });

        FamSpecial.LOGGER.info("Registering Fortune & Glory tick handler for {}", FamSpecial.MOD_ID);
    }

    private static void fireScheduledShot(ServerWorld world, ServerPlayerEntity player, ItemStack crossbowStack, ItemStack projectileStack) {
        if (projectileStack.isEmpty()) return;

        if (projectileStack.getItem() instanceof FireworkRocketItem) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    world, projectileStack, player,
                    player.getX(), player.getEyeY() - 0.15, player.getZ(), true);
            Vec3d rotationVec = player.getRotationVec(1.0F);
            rocket.setVelocity(rotationVec.x, rotationVec.y, rotationVec.z, FIREWORK_ROCKET_SPEED, 1.0F);
            world.spawnEntity(rocket);
        } else {
            int projectileCount = EnchantmentHelper.getProjectileCount(world, crossbowStack, player, 1);
            if (projectileCount <= 0) return;
            float spreadBase = EnchantmentHelper.getProjectileSpread(world, crossbowStack, player, 0.0F);
            float perShot = projectileCount == 1 ? 0.0F : 2.0F * spreadBase / (projectileCount - 1);
            float center = (projectileCount - 1) % 2 * perShot / 2.0F;
            float sign = 1.0F;

            for (int i = 0; i < projectileCount; i++) {
                float yawOffset = center + sign * ((i + 1) / 2) * perShot;
                sign = -sign;

                ItemStack arrowItemStack = new ItemStack(Items.ARROW);
                VolleyArrowEntity arrow = new VolleyArrowEntity(world, player, arrowItemStack, crossbowStack);
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                arrow.setSound(SoundEvents.ITEM_CROSSBOW_HIT);

                Vec3d oppositeRotation = player.getOppositeRotationVector(1.0F);
                Quaternionf quaternion = new Quaternionf().setAngleAxis(
                        yawOffset * (float) (Math.PI / 180.0),
                        oppositeRotation.x, oppositeRotation.y, oppositeRotation.z);
                Vec3d rotationVec = player.getRotationVec(1.0F);
                Vector3f velocity = rotationVec.toVector3f().rotate(quaternion);
                arrow.setVelocity(velocity.x(), velocity.y(), velocity.z(), CROSSBOW_PROJECTILE_SPEED, 1.0F);

                ProjectileEntity.spawn(arrow, world, arrowItemStack);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.incrementStat(Stats.USED.getOrCreateStat(Items.CROSSBOW));
    }

    static ConcurrentLinkedQueue<ScheduledShot> getPendingShots() {
        return PENDING_SHOTS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean wasChargedBefore = CrossbowItem.isCharged(stack);
        ChargedProjectilesComponent loaded = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        ItemStack capturedProjectile = (loaded != null && !loaded.isEmpty())
                ? loaded.getProjectiles().get(0).copy()
                : ItemStack.EMPTY;
        ActionResult result = super.use(world, user, hand);
        if (!wasChargedBefore) return result;
        if (CrossbowItem.isCharged(stack)) return result;
        if (world.isClient()) return result;
        if (!Boolean.TRUE.equals(stack.get(ModComponents.CRUSADERS_VOLLEY))) return result;
        if (capturedProjectile.isEmpty()) return result;

        long now = world.getTime();
        UUID uuid = user.getUuid();
        PENDING_SHOTS.add(new ScheduledShot(now + SHOT_2_DELAY_TICKS, stack, uuid, capturedProjectile));
        PENDING_SHOTS.add(new ScheduledShot(now + SHOT_3_DELAY_TICKS, stack, uuid, capturedProjectile));
        if (user instanceof ServerPlayerEntity sp) {
            sp.incrementStat(Stats.USED.getOrCreateStat(Items.CROSSBOW));
        }
        return result;
    }

    public record ScheduledShot(long targetTick, ItemStack crossbowStack, UUID playerUuid, ItemStack projectileStack) {}
}
