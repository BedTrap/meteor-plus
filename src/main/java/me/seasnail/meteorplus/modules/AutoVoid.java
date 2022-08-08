package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoVoid extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Integer> range = sgDefault.add(new IntSetting.Builder()
        .name("range")
        .description("The maximum range from the target.")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block being mined server-side")
        .defaultValue(true)
        .build()
    );

    private PlayerEntity target;
    private BlockPos blockPos;
    private FindItemResult pickSlot;

    public AutoVoid() {
        super(MeteorPlus.CATEGORY, "auto-void", "Automatically tries to void people. Made for EC.ME.");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        pickSlot = InvUtils.findInHotbar(itemStack -> {
                Item item = itemStack.getItem();
                return (item instanceof PickaxeItem);
            }
        );

        if (pickSlot.found() && target != null) {
            blockPos = target.getBlockPos().add(0, -1, 0);

            if (blockPos.getY() < 1 && mc.world.getBlockState(blockPos.add(0, -1, 0)).getHardness(mc.world, blockPos) >= 0) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 20, this::mineVoidBlock);
                 else mineVoidBlock();
                toggle();
            }
        }
    }


    private void mineVoidBlock() {
        InvUtils.swap(pickSlot.getSlot());
        mc.interactionManager.attackBlock(blockPos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
