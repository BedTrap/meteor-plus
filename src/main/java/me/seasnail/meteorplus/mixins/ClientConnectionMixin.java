package me.seasnail.meteorplus.mixins;

import io.netty.util.concurrent.GenericFutureListener;
import me.seasnail.meteorplus.modules.PacketLogger;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void logReceivedPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        PacketLogger module = Modules.get().get(PacketLogger.class);
        if (module.isActive() && module.s2cPackets.get().contains(packet.getClass())) module.logReceivedPacket(packet);
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"))
    private void logSentPacket(Packet<?> packet, GenericFutureListener<?> callback, CallbackInfo ci) {
        PacketLogger module = Modules.get().get(PacketLogger.class);
        if (module.isActive() && module.c2sPackets.get().contains(packet.getClass())) module.logSentPacket(packet);
    }
}
