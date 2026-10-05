/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import reika.voidmonster.VoidClient;
import reika.voidmonster.entity.EntityVoidMonster;

/** Periodic entity verification uses dimension keys and UUIDs, including protection against reused IDs. */
public final class VoidNetwork {
    private VoidNetwork() {}
    public record Verify(Identifier dimension, int id, UUID uuid) implements CustomPacketPayload {
        public static final Type<Verify> TYPE = new Type<>(Identifier.fromNamespaceAndPath("voidmonster", "verify"));
        public static final StreamCodec<ByteBuf, Verify> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Verify::dimension, ByteBufCodecs.VAR_INT, Verify::id,
                UUIDUtil.STREAM_CODEC, Verify::uuid, Verify::new);
        @Override public Type<Verify> type() { return TYPE; }
    }
    public record Missing(Identifier dimension, int id, UUID uuid) implements CustomPacketPayload {
        public static final Type<Missing> TYPE = new Type<>(Identifier.fromNamespaceAndPath("voidmonster", "missing"));
        public static final StreamCodec<ByteBuf, Missing> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Missing::dimension, ByteBufCodecs.VAR_INT, Missing::id,
                UUIDUtil.STREAM_CODEC, Missing::uuid, Missing::new);
        @Override public Type<Missing> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Verify.TYPE, Verify.CODEC, (packet, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var level = player.level();
            var entity = level.getEntity(packet.id());
            if (!level.dimension().identifier().equals(packet.dimension()) || !(entity instanceof EntityVoidMonster)
                    || !entity.getUUID().equals(packet.uuid()))
                PacketDistributor.sendToPlayer(player, new Missing(packet.dimension(), packet.id(), packet.uuid()));
        });
        registrar.playToClient(Missing.TYPE, Missing.CODEC, (packet, context) -> VoidClient.removeMissing(packet));
    }
}
