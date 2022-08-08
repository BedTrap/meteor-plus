package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import me.seasnail.meteorplus.utils.BlockUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Surround extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgDisable = settings.createGroup("Disable");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What mode to use for surrounding.")
        .defaultValue(Mode.Normal)
        .build()
    );

    private final Setting<Boolean> center = sgDefault.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you when surrounding.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> anchor = sgDefault.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Anchors you after centering to prevent you from obstructing placements.")
        .defaultValue(true)
        .visible(center::get)
        .build()
    );

    private final Setting<Boolean> support = sgDefault.add(new BoolSetting.Builder()
        .name("support")
        .description("Places support blocks below your surround blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Tick delay in between block placement.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgDefault.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Amount of blocks to place in 1 tick.")
        .defaultValue(1)
        .min(1)
        .sliderMax(4)
        .build()
    );

    private final Setting<Boolean> ignoreEntities = sgDefault.add(new BoolSetting.Builder()
        .name("ignore-entities")
        .description("Ignores visible entities/crystals and tries to place on top of it. Useful on high ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> blocks = sgDefault.add(new BlockListSetting.Builder()
        .name("block")
        .description("What blocks to use for surrounding.")
        .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the blocks you place server side.")
        .defaultValue(true)
        .build()
    );

    // Disable

    private final Setting<Boolean> jumpDisable = sgDisable.add(new BoolSetting.Builder()
        .name("jump-disable")
        .description("Disables surround when your y coordinate changes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tpDisable = sgDisable.add(new BoolSetting.Builder()
        .name("tp-disable")
        .description("Disables surround when you teleport to another location.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableWhenDone = sgDisable.add(new BoolSetting.Builder()
        .name("disable-when-done")
        .description("Disables surround when you're done placing all blocks.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders client-side swinging when placing the blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 45, true))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255, true))
        .build()
    );

    private final Setting<SettingColor> nextSideColor = sgRender.add(new ColorSetting.Builder()
        .name("next-side-color")
        .description("The side color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245, 10))
        .build()
    );

    private final Setting<SettingColor> nextLineColor = sgRender.add(new ColorSetting.Builder()
        .name("next-line-color")
        .description("The line color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245))
        .build()
    );

    private final List<BlockPos> placePositions = new ArrayList<>();
    private BlockPos blockPos;
    private int timeLeft;

    public Surround() {
        super(MeteorPlus.CATEGORY, "surround", "Surrounds you in blocks to prevent crystal explosions damage.");
    }

    @Override
    public void onActivate() {
        timeLeft = 0;

        if (center.get()) {
            PlayerUtils.centerPlayer();
            if (anchor.get()) ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
        }
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (mc.player.prevY < mc.player.getY() && jumpDisable.get()) {
            toggle();
            return;
        }

        if (center.get() && anchor.get()) {
            ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
        }

        if (!mc.player.isOnGround()) return;

        if (timeLeft <= 0) {
            for (int i = 0; i < blocksPerTick.get(); i++) {
                addPositions();

                if (!placePositions.isEmpty()) {
                    BlockPos pos = placePositions.get(placePositions.size() - 1);
                    FindItemResult findItemResult = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
                    BlockUtils.place(pos, findItemResult, rotate.get(), 80, swing.get(), false, true);
                    placePositions.remove(blockPos);
                }
            }

            timeLeft = tickDelay.get();
        }

        timeLeft--;

        if (placePositions.isEmpty() && disableWhenDone.get()) toggle();
    }

    private void addPositions() {
        blockPos = mc.player.getBlockPos();
        placePositions.clear();

        if (mode.get() == Mode.AntiCity) {
            add(blockPos.add(2, 0, 0));
            add(blockPos.add(0, 0, 2));
            add(blockPos.add(-2, 0, 0));
            add(blockPos.add(0, 0, -2));

            if (support.get()) {
                add(blockPos.add(2, -1, 0));
                add(blockPos.add(0, -1, 2));
                add(blockPos.add(-2, -1, 0));
                add(blockPos.add(0, -1, -2));
            }
        }

        add(blockPos.add(1, 0, 0));
        add(blockPos.add(0, 0, 1));
        add(blockPos.add(-1, 0, 0));
        add(blockPos.add(0, 0, -1));

        if (support.get()) {
            if (!BlockUtils.hasSupport(blockPos.add(1, 0, 0))) add(blockPos.add(1, -1, 0));
            if (!BlockUtils.hasSupport(blockPos.add(0, 0, 1))) add(blockPos.add(0, -1, 1));
            if (!BlockUtils.hasSupport(blockPos.add(-1, 0, 0))) add(blockPos.add(-1, -1, 0));
            if (!BlockUtils.hasSupport(blockPos.add(0, 0, -1))) add(blockPos.add(0, -1, -1));
        }
    }

    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && BlockUtils.canPlace(blockPos, !ignoreEntities.get())) placePositions.add(blockPos);
    }

    private boolean blockFilter(Block block) {
        return block.getBlastResistance() > 600;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && tpDisable.get()) toggle();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;

        for (BlockPos pos : placePositions) {
            if (pos.equals(placePositions.get(placePositions.size() - 1))) {
                event.renderer.box(pos, nextSideColor.get(), nextLineColor.get(), shapeMode.get(), 0);
            } else {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    public enum Mode {
        Normal,
        AntiCity
    }
}
