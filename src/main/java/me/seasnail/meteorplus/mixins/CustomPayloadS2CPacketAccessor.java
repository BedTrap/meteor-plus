package me.seasnail.meteorplus.mixins;

import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CustomPayloadS2CPacket.class)
public interface CustomPayloadS2CPacketAccessor {
    @Accessor("channel")
    Identifier getChannel();
}
