package dev.xt9y.perf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A request, not an item grant. The server authorizes and resolves everything. */
public record GrantBlockPayload(Identifier blockId, boolean fullStack) implements CustomPacketPayload {
    public static final Type<GrantBlockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("xt9y263", "grant_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrantBlockPayload> CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, GrantBlockPayload::blockId,
                    ByteBufCodecs.BOOL, GrantBlockPayload::fullStack,
                    GrantBlockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
