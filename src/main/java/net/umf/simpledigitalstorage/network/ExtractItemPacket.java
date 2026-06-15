package net.umf.simpledigitalstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;
import net.umf.simpledigitalstorage.block.entity.StorageHubBlockEntity;

public record ExtractItemPacket(BlockPos hubPos, ItemResource resource, int amount, boolean shiftClick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExtractItemPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleDigitalStorage.MODID, "extract_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemPacket> STREAM_CODEC = StreamCodec.ofMember(
            ExtractItemPacket::write,
            ExtractItemPacket::new
    );

    public ExtractItemPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos(), ItemResource.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readBoolean());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(hubPos);
        ItemResource.STREAM_CODEC.encode(buf, resource);
        buf.writeVarInt(amount);
        buf.writeBoolean(shiftClick);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExtractItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null && !player.level().isClientSide()) {
                BlockEntity be = player.level().getBlockEntity(packet.hubPos());
                if (be instanceof StorageHubBlockEntity hub) {
                    ItemStack extracted = hub.extractItem(packet.resource(), packet.amount());
                    if (!extracted.isEmpty()) {
                        // Always insert directly into the player's inventory for simplicity right now.
                        if (!player.getInventory().add(extracted)) {
                            player.drop(extracted, false);
                        }
                    }
                }
            }
        });
    }
}
