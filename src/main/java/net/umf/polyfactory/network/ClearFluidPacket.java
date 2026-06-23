package net.umf.polyfactory.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.block.entity.FabricatorBlockEntity;

/** Sent when the player clicks the Fabricator GUI's "Trash" button to void the fluid tank. */
public record ClearFluidPacket(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearFluidPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PolyFactory.MODID, "clear_fluid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearFluidPacket> STREAM_CODEC =
            StreamCodec.ofMember(ClearFluidPacket::write, ClearFluidPacket::new);

    public ClearFluidPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearFluidPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || player.level().isClientSide()) {
                return;
            }
            if (player.position().distanceToSqr(Vec3.atCenterOf(packet.pos())) > 64.0
                    || !(player.level().getBlockEntity(packet.pos()) instanceof FabricatorBlockEntity fabricator)) {
                return;
            }
            fabricator.clearFluid();
        });
    }
}
