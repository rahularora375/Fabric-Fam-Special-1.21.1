package github.rahularora375.famspecial.net;

import github.rahularora375.famspecial.FamSpecial;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

// Login-phase version handshake. During QUERY_START the server sends its
// mod version to every connecting client; the client responds with its own.
// The server kicks the connection if (a) the client didn't understand the
// query (vanilla / no-mod client) or (b) the version strings don't match
// exactly. Policy: strict equality — matches how the project ships small
// incremental releases.
public final class VersionHandshake {
    public static final Identifier CHANNEL = Identifier.of(FamSpecial.MOD_ID, "version");

    private VersionHandshake() {}

    public static void registerServer() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(FamSpecial.MOD_VERSION);
            sender.sendPacket(CHANNEL, buf);
        });

        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                handler.disconnect(Text.literal(
                        "Fam Special " + FamSpecial.MOD_VERSION + " is required on your client. Install the matching mod version to join this server."
                ));
                return;
            }
            String clientVersion = buf.readString();
            if (!FamSpecial.MOD_VERSION.equals(clientVersion)) {
                handler.disconnect(Text.literal(
                        "Fam Special version mismatch — server: " + FamSpecial.MOD_VERSION + ", client: " + clientVersion + ". Install the matching version."
                ));
            }
        });

        FamSpecial.LOGGER.info("Registered version handshake (server) for {} v{}", FamSpecial.MOD_ID, FamSpecial.MOD_VERSION);
    }

    public static void registerClient() {
        ClientLoginNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, callbacksConsumer) -> {
            String serverVersion = buf.readString();
            FamSpecial.LOGGER.info("Fam Special handshake: server {}, client {}", serverVersion, FamSpecial.MOD_VERSION);
            PacketByteBuf reply = new PacketByteBuf(Unpooled.buffer());
            reply.writeString(FamSpecial.MOD_VERSION);
            return CompletableFuture.completedFuture(reply);
        });
    }
}
