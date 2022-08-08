/* Supercakes' first ever module omg */

package me.seasnail.meteorplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import me.seasnail.meteorplus.MeteorPlus;

import java.util.Collections;
import java.util.List;

public class AntiCity extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Boolean> doubleBlock = sgDefault.add(new BoolSetting.Builder()
        .name("double")
        .description("Will also place right in front of you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggle = sgDefault.add(new BoolSetting.Builder()
        .name("toggle")
        .description("Toggles off after anti citying once.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> blocks = sgDefault.add(new BlockListSetting.Builder()
        .name("block")
        .description("What blocks to use for anti citying.")
        .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks you place server-side.")
        .defaultValue(true)
        .build()
    );

    public AntiCity() {
        super(MeteorPlus.CATEGORY, "anti-city", "Places obsidian 2 blocks in front of you to avoid getting stuck in a 3x1 hole.");
    }

    @EventHandler
    public void onPostTick(TickEvent.Post event) {
        FindItemResult findItemResult = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        Direction direction = mc.player.getMovementDirection();
        BlockUtils.place(mc.player.getBlockPos().offset(direction, 2), findItemResult, rotate.get(), 50, true);
        BlockUtils.place(mc.player.getBlockPos().add(0, 1, 0).offset(direction, 2), findItemResult, rotate.get(), 50, true);
        if (doubleBlock.get()) BlockUtils.place(mc.player.getBlockPos().offset(direction, 1), findItemResult, rotate.get(), 50, true);
        if (toggle.get()) {
            sendToggledMsg();
            toggle();
        }
    }

    private boolean blockFilter(Block block) {
        return block.getBlastResistance() > 600;
    }
}
