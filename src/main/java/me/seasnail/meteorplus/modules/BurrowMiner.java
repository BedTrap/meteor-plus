package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstaMine;
import meteordevelopment.meteorclient.systems.modules.player.PacketMine;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BurrowMiner extends Module {

    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgPause = settings.createGroup("Pause");

    private final Setting<SortPriority> sortPriority = sgDefault.add(new EnumSetting.Builder<SortPriority>()
        .name("sort-priority")
        .description("What target to prioritize.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Double> range = sgDefault.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range allowed from a target.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> toggle = sgDefault.add(new BoolSetting.Builder()
        .name("toggle")
        .description("Toggles off after mining once.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block being mined server-side.")
        .defaultValue(true)
        .build()
    );

    // Pause

    private final Setting<Boolean> eatPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses Burrow Miner when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses Burrow Miner when drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> minePause = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses Burrow Miner when mining.")
        .defaultValue(false)
        .build()
    );

    private PlayerEntity target;
    private BlockPos blockPos;

    public BurrowMiner() {
        super(MeteorPlus.CATEGORY, "burrow-miner", "Mines a target's burrow block.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;
        target = TargetUtils.getPlayerTarget(range.get(), sortPriority.get());
        if (target != null) {
            blockPos = target.getBlockPos();
            if (!mc.world.getBlockState(blockPos).isAir()) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 10, this::mineBurrowBlock);
                else mineBurrowBlock();

                if (toggle.get()) {
                    sendToggledMsg();
                    toggle();
                }
            }
        }
    }

    private void mineBurrowBlock() {
        mc.interactionManager.attackBlock(blockPos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (!Modules.get().get(PacketMine.class).isActive() && !Modules.get().get(InstaMine.class).isActive()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
