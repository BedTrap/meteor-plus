package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiVoid extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode to use to attempt saving you from the void.")
            .defaultValue(Mode.Packet)
            .build()
    );

    private final Setting<Double> packetHeight = sgDefault.add(new DoubleSetting.Builder()
            .name("packet-height")
            .description("How high to get teleported up when using packet mode.")
            .defaultValue(2)
            .min(0.01)
            .max(10)
            .visible(() -> mode.get() == Mode.Packet)
            .build()
    );

    public AntiVoid() {
        super(MeteorPlus.CATEGORY, "anti-void", "Attempts to save you from falling into the void.");
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (mc.player.getY() > 0) return;

        switch (mode.get()) {
            case Packet -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + packetHeight.get(), mc.player.getZ(), false));
            case Flight -> {
                Flight flight = Modules.get().get(Flight.class);
                if (!flight.isActive()) {
                    flight.toggle();
                }
            }
            case Elytra -> {
                ChestSwap chestSwap = Modules.get().get(ChestSwap.class);
                ElytraFly elytraFly = Modules.get().get(ElytraFly.class);

                if (!elytraFly.isActive()) elytraFly.toggle();
                if (!chestSwap.isActive()) chestSwap.toggle();
            }
            case Center -> PlayerUtils.centerPlayer();
            case Jump -> mc.player.jump();
        }
    }

    public enum Mode {
        Packet,
        Flight,
        Elytra,
        Center,
        Jump
    }
}
