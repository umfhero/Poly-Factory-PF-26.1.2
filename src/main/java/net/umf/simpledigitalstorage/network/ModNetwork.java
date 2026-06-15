package net.umf.simpledigitalstorage.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;

public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
                ExtractItemPacket.TYPE,
                ExtractItemPacket.STREAM_CODEC,
                ExtractItemPacket::handle
        );

        registrar.playToClient(
                SyncNetworkItemsPacket.TYPE,
                SyncNetworkItemsPacket.STREAM_CODEC,
                SyncNetworkItemsPacket::handle
        );
    }
}
