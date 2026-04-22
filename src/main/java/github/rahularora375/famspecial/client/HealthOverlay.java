package github.rahularora375.famspecial.client;

import github.rahularora375.famspecial.component.ModComponents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// Client-side HP bars above every remote player in view. Gated on the local
// player's HEAD slot carrying SHOWS_ENTITY_HP (Sage's Crown). No server
// involvement — health is read live off the tracked client entity.
public final class HealthOverlay {
    private HealthOverlay() {}

    // Skip entities beyond this radius to keep the per-frame entity loop cheap
    // and avoid drawing unreadable far-away bars. 48 blocks ≈ 3 chunks, which
    // matches typical render distance for legible text/quads.
    private static final double VIEW_DISTANCE_SQ = 48.0 * 48.0;

    private static final float BAR_WIDTH = 24.0f;   // post-scale pixels
    private static final float BAR_HEIGHT = 3.0f;
    private static final float SCALE = 0.025f;       // vanilla nameplate scale
    private static final float HEAD_CLEARANCE = 0.55f; // blocks above eye line

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity self = mc.player;
            if (self == null) return;
            if (!Boolean.TRUE.equals(self.getEquippedStack(EquipmentSlot.HEAD).get(ModComponents.SHOWS_ENTITY_HP))) return;

            ClientWorld world = mc.world;
            if (world == null) return;

            Camera camera = mc.gameRenderer.getCamera();
            Vec3d camPos = camera.getCameraPos();
            MatrixStack matrices = ctx.matrices();
            VertexConsumerProvider consumers = ctx.consumers();
            float tickDelta = mc.getRenderTickCounter().getTickProgress(false);

            // Only remote players — not self, not mobs. `world.getPlayers()`
            // already includes the local player, so we skip on identity.
            for (PlayerEntity other : world.getPlayers()) {
                if (other == self) continue;
                if (other.isRemoved() || !other.isAlive() || other.isSpectator()) continue;
                if (self.squaredDistanceTo(other) > VIEW_DISTANCE_SQ) continue;

                float max = other.getMaxHealth();
                if (max <= 0.0f) continue;
                float pct = MathHelper.clamp(other.getHealth() / max, 0.0f, 1.0f);

                renderBar(other, pct, tickDelta, camPos, camera, matrices, consumers);
            }
        });
    }

    private static void renderBar(PlayerEntity e, float pct, float tickDelta, Vec3d camPos,
                                  Camera camera, MatrixStack matrices, VertexConsumerProvider consumers) {
        double ex = MathHelper.lerp(tickDelta, e.lastX, e.getX()) - camPos.x;
        double ey = MathHelper.lerp(tickDelta, e.lastY, e.getY()) + e.getHeight() + HEAD_CLEARANCE - camPos.y;
        double ez = MathHelper.lerp(tickDelta, e.lastZ, e.getZ()) - camPos.z;

        matrices.push();
        matrices.translate(ex, ey, ez);
        matrices.multiply(camera.getRotation());
        // Negate X/Y to flip from text-renderer's "screen-up-is-negative-Y"
        // convention into world-space where +Y is up.
        matrices.scale(-SCALE, -SCALE, SCALE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float halfW = BAR_WIDTH / 2.0f;
        float x0 = -halfW;
        float x1 = halfW;
        float y0 = 0.0f;
        float y1 = BAR_HEIGHT;

        // debugQuads is a POSITION_COLOR pipeline — simplest working path for
        // solid world-space quads; text-background layers silently dropped
        // our writes (vertex format mismatch for the text pipeline).
        VertexConsumer vc = consumers.getBuffer(RenderLayers.debugQuads());
        // Background bar: dark, semi-transparent, full width.
        quad(vc, matrix, x0, y0, x1, y1, 0, 0, 0, 160);

        // Foreground bar: colored by HP %, drawn just in front of the bg so
        // depth-sort doesn't flicker against the background quad.
        int rgb = colorForPct(pct);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float fgX1 = x0 + BAR_WIDTH * pct;
        quad(vc, matrix, x0, y0, fgX1, y1, r, g, b, 255);

        matrices.pop();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1,
                             int r, int g, int b, int a) {
        vc.vertex(m, x0, y1, 0).color(r, g, b, a);
        vc.vertex(m, x1, y1, 0).color(r, g, b, a);
        vc.vertex(m, x1, y0, 0).color(r, g, b, a);
        vc.vertex(m, x0, y0, 0).color(r, g, b, a);
    }

    // Three-band color ramp: green above 66%, yellow above 33%, red otherwise.
    private static int colorForPct(float pct) {
        if (pct >= 2.0f / 3.0f) return 0x55DD55;
        if (pct >= 1.0f / 3.0f) return 0xEECC33;
        return 0xDD3333;
    }
}
