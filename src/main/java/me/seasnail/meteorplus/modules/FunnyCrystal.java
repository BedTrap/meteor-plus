/*
    Created by Supakeks 04/02/20
 */

package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class FunnyCrystal extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> targetRange = sgDefault.add(new IntSetting.Builder()
        .name("target-range")
        .description("Maximum range allowed from a player to make it a target.")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The delay in ticks in between actions.")
        .defaultValue(2)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> antiStuck = sgDefault.add(new BoolSetting.Builder()
        .name("anti-stuck")
        .description("Replaces a crystal above the obsidian block if it's broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instant = sgDefault.add(new BoolSetting.Builder()
        .name("instant")
        .description("Attempts to re-break the obsidian instantly.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Faces the blocks you interact with server-side.")
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

    private int tickDelayLeft, stage;
    private boolean shouldAttack;
    private PlayerEntity target;
    private BlockPos blockPos;

    public FunnyCrystal() {
        super(MeteorPlus.CATEGORY, "funny-crystal", "Automatically funny crystals the closest target. Doesn't work on strict servers.");
    }

    // Stages
    // 1: place first obby
    // 2: place the crystal above the obby
    // 3: send the stop mine packet for the obby we placed in the first stage
    // 4: wait until the first obby is gone, then explode crystal, repeat

    @Override
    public void onActivate() {
        stage = 1;
        tickDelayLeft = 0;
        shouldAttack = true;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (target != null && tickDelayLeft <= 0) {
            funnyCrystal();
            tickDelayLeft = tickDelay.get();
        }
        tickDelayLeft--;
    }

    private void funnyCrystal() {
        blockPos = target.getBlockPos().add(0, 2, 0);

        switch (stage) {
            case 1 -> {
                // First stage, place the first obsidian above the target.
                if (tickDelayLeft <= 0) {
                    FindItemResult obbySlot = InvUtils.findInHotbar(Items.OBSIDIAN);
                    if (!obbySlot.found()) {
                        ChatUtils.error("Could not find obsidian. Disabling!");
                        toggle();
                        return;
                    }
                    BlockUtils.place(blockPos, obbySlot, rotate.get(), 50, true);
                    stage = 2;
                }
            }
            case 2 -> {
                // Second stage, place the crystal above the obsidian.
                if (tickDelayLeft <= 0) {
                    FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                    int prevSlot = mc.player.getInventory().selectedSlot;
                    if (!crystalSlot.found() && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                        ChatUtils.error("Could not find end crystals. Disabling!");
                        toggle();
                        return;
                    }
                    if (mc.world.getBlockState(blockPos).isAir()) stage = 1;
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> placeCrystal(crystalSlot, prevSlot));
                    else placeCrystal(crystalSlot, prevSlot);
                    stage = 3;
                }
            }
            case 3 -> {
                // Third stage, send the stop mining packet, which is what causes the instant mining.
                if (tickDelayLeft <= 0) {
                    FindItemResult pickSlot = InvUtils.findInHotbar(itemStack -> {
                            Item item = itemStack.getItem();
                            return (item instanceof PickaxeItem);
                        }
                    );
                    if (!pickSlot.found()) {
                        toggle();
                        ChatUtils.error("Could not find a pickaxe. Disabling!");
                        return;
                    }
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> attackBlock(pickSlot));
                    else attackBlock(pickSlot);
                    stage = 4;
                }
            }
            case 4 -> {
                // 4th and last stage, hit the crystal
                if (tickDelayLeft <= 0) {
                    if (mc.world.getBlockState(blockPos).isAir()) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (mc.player.distanceTo(entity) <= targetRange.get() && entity instanceof EndCrystalEntity) {
                                if (rotate.get()) {
                                    Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Feet), 50, () -> hitCrystal(entity));
                                } else hitCrystal(entity);
                                stage = 1;
                            }
                        }
                    }
                    if (BlockUtils.canPlace(blockPos.add(0, 1, 0), true) && antiStuck.get()) {
                        FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                        int prevSlot = mc.player.getInventory().selectedSlot;
                        if (crystalSlot.found()) {
                            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> placeCrystal(crystalSlot, prevSlot));
                            else placeCrystal(crystalSlot, prevSlot);
                        }
                    }
                }
            }
        }
    }

    private void placeCrystal(FindItemResult crystalSlot, int prevSlot) {
        if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            mc.player.getInventory().selectedSlot = crystalSlot.getSlot();
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        }
        Hand hand = crystalSlot.getHand();
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(mc.player.getPos(), getDirection(blockPos), blockPos, false)));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (hand == Hand.MAIN_HAND) mc.player.getInventory().selectedSlot = prevSlot;
    }

    private void attackBlock(FindItemResult pickSlot) {
        InvUtils.swap(pickSlot.getSlot());
        if (shouldAttack) { // Only call the attackBlock method once, since it would make instant mine not work if you called it all the time (Because it sends the start mining packet)
            mc.interactionManager.attackBlock(blockPos, Direction.DOWN);
            if (instant.get()) shouldAttack = false; // Sets shouldAttack to false if instant is on, if it's off, it'll never set it so it'll make it packet mine
        }
        if (!mc.world.getBlockState(blockPos).isAir()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));
        }
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void hitCrystal(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // Stolen from CA, thanks GL
    private Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }
        if ((double) pos.getY() > eyesPos.y) {
            return Direction.DOWN; // The player can never see the top of a block if they are under it
        }
        return Direction.UP;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get() || blockPos == null || blockPos.getY() == -1 || mc.world.getBlockState(blockPos).isAir()) return;
        event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
