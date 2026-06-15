package net.umf.simpledigitalstorage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;
import net.umf.simpledigitalstorage.gui.StorageHubScreen;

import java.util.LinkedHashMap;
import java.util.Map;

public record SyncNetworkItemsPacket(Map<ItemResource, Long> items) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncNetworkItemsPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleDigitalStorage.MODID, "sync_network_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNetworkItemsPacket> STREAM_CODEC = StreamCodec.ofMember(
            SyncNetworkItemsPacket::write,
            SyncNetworkItemsPacket::new
    );

    public SyncNetworkItemsPacket(RegistryFriendlyByteBuf buf) {
        this(readItems(buf));
    }

    private static Map<ItemResource, Long> readItems(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ItemResource, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            ItemResource res = ItemResource.STREAM_CODEC.decode(buf);
            long amount = buf.readVarLong();
            map.put(res, amount);
        }
        return map;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        for (Map.Entry<ItemResource, Long> entry : items.entrySet()) {
            ItemResource.STREAM_CODEC.encode(buf, entry.getKey());
            buf.writeVarLong(entry.getValue());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNetworkItemsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof StorageHubScreen screen) {
                screen.updateNetworkItems(packet.items());
            }
        });
    }
}
