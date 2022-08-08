package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class EChestPhase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance per clip")
        .defaultValue(0.01)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> vertical = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical")
        .description("Will try to phase you vertically as well as horizontally.")
        .defaultValue(false)
        .build()
    );

    public EChestPhase() {
        super(MeteorPlus.CATEGORY, "echest-phase", "Allows you to phase through some blocks (only 1 block, not a reliable method of phasing).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isOnGround()) return;

        if (mc.options.keyForward.isPressed()) updateHorizontal(0.0F);
        if (mc.options.keyBack.isPressed()) updateHorizontal(180.0F);
        if (mc.options.keyLeft.isPressed()) updateHorizontal(90.0F);
        if (mc.options.keyRight.isPressed()) updateHorizontal(270.0F);

        if (vertical.get()) {
            if (mc.options.keyJump.isPressed()) updateVertical(mc.player.getY() + distance.get());
            if (mc.options.keySneak.isPressed()) updateVertical(mc.player.getY() - distance.get());
        }
    }

    public void updateHorizontal(float angle) {
        Vec3d direction = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw() - angle);
        mc.player.updatePosition(mc.player.getX() + direction.x * distance.get(), mc.player.getY(), mc.player.getZ() + direction.z * distance.get());
    }

    public void updateVertical(double y) {
        mc.player.updatePosition(mc.player.getX(), y, mc.player.getZ());
    }
}


