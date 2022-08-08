package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AntiClip extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgDefault.add(new IntSetting.Builder()
        .name("range")
        .description("How far the target has to be to anti clip.")
        .defaultValue(5)
        .min(1)
        .max(7)
        .build()
    );

    private final Setting<Integer> height = sgDefault.add(new IntSetting.Builder()
        .name("height")
        .description("How high to cover the target's top part.")
        .defaultValue(4)
        .min(1)
        .max(5)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgDefault.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Blocks per tick.")
        .defaultValue(2)
        .min(1)
        .max(8)
        .build()
    );

    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Delay in ticks.")
        .defaultValue(2)
        .min(1)
        .max(10)
        .build()
    );

    private final Setting<Boolean> toggle = sgDefault.add(new BoolSetting.Builder()
        .name("toggle")
        .description("Toggles off after placing all blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> trap = sgDefault.add(new BoolSetting.Builder()
        .name("trap")
        .description("Traps the top side parts of the target.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bottom = sgDefault.add(new BoolSetting.Builder()
        .name("bottom")
        .description("Prevents the target from vcliping down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> bottomHeight = sgDefault.add(new IntSetting.Builder()
        .name("bottom-height")
        .description("How high to cover the target's bottom part.")
        .defaultValue(4)
        .min(1)
        .max(5)
        .visible(bottom::get)
        .build()
    );

    private final Setting<Boolean> funnyCrystal = sgDefault.add(new BoolSetting.Builder()
        .name("funny-crystal")
        .description("Doesn't place a block above the first block to make the Funny Crystal module work.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the block you place server-side.")
        .defaultValue(true)
        .build()
    );

    // Render

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
        .defaultValue(new SettingColor(204, 0, 0, 45))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final List<BlockPos> placePositions = new ArrayList<>();
    private PlayerEntity target;
    private int tickDelayLeft;
    private BlockPos pos;

    public AntiClip() {
        super(MeteorPlus.CATEGORY, "anti-clip", "Fuck netheranarchy anticheat");
    }

    @Override
    public void onActivate() {
        target = null;
        if (!placePositions.isEmpty()) placePositions.clear();
        tickDelayLeft = 0;
    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        FindItemResult obbySlot = InvUtils.findInHotbar(Items.OBSIDIAN);

        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target != null && tickDelayLeft <= 0) {
            tickDelayLeft = tickDelay.get();
            for (int i = 0; i <= blocksPerTick.get(); i++) {
                findPlacePos(target);
                if (placePositions.isEmpty()) {
                    if (toggle.get()) {
                        this.toggle();
                        sendToggledMsg();
                    }
                    return;
                }
                pos = placePositions.get(placePositions.size() - 1);
                BlockUtils.place(pos, obbySlot, rotate.get(), 10, true);
            }
        }
        tickDelayLeft--;
    }

    private void findPlacePos(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();


        if (bottomHeight.get() > 4 && bottom.get()) add(targetPos.add(0, -5, 0));
        if (bottomHeight.get() > 3 && bottom.get()) add(targetPos.add(0, -4, 0));
        if (bottomHeight.get() > 2 && bottom.get()) add(targetPos.add(0, -3, 0));
        if (bottomHeight.get() > 1 && bottom.get()) add(targetPos.add(0, -2, 0));
        if (height.get() > 5) add(targetPos.add(0, 6, 0));
        if (height.get() > 4) add(targetPos.add(0, 5, 0));
        if (height.get() > 3) add(targetPos.add(0, 4, 0));
        if (height.get() > 2 && !funnyCrystal.get()) add(targetPos.add(0, 3, 0));
        add(targetPos.add(0, 2, 0));
        if (trap.get()) add(targetPos.add(1, 1, 0));
        if (trap.get()) add(targetPos.add(0, 1, 1));
        if (trap.get()) add(targetPos.add(-1, 1, 0));
        if (trap.get()) add(targetPos.add(0, 1, -1));
    }

    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent())) placePositions.add(blockPos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get() || target == null || pos == null || pos.getY() == -1 || mc.world.getBlockState(pos).isAir()) return;
        event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getGameProfile().getName();
        return null;
    }
}
