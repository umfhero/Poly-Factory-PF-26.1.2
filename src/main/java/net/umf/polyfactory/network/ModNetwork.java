package net.umf.polyfactory.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
                ToggleSplitPacket.TYPE,
                ToggleSplitPacket.STREAM_CODEC,
                ToggleSplitPacket::handle
        );

        registrar.playToServer(
                ClearFluidPacket.TYPE,
                ClearFluidPacket.STREAM_CODEC,
                ClearFluidPacket::handle
        );
    }
}
