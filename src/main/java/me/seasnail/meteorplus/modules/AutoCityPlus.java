/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AutoCityPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range a city-able block will be found.")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("support")
        .description("If there is no block below a city block it will place one before mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Sends a client-side message if you city a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Insta mines the city block if it's replaced by the target.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates you towards the city block.")
        .defaultValue(true)
        .build()
    );

    private PlayerEntity target;
    private BlockPos mineTarget;

    public AutoCityPlus() {
        super(MeteorPlus.CATEGORY, "auto-city-plus", "Automatically cities a target by mining the nearest obsidian next to them.");
    }

    @Override
    public void onActivate() {
        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target != null) {
            mineTarget = EntityUtils.getCityBlock(target);
        }

        if (target == null || mineTarget == null) {
            if (chatInfo.get()) ChatUtils.error("No target block found... disabling.");
        } else {
            if (chatInfo.get()) ChatUtils.info("Attempting to city " + target.getGameProfile().getName());

            if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > mc.interactionManager.getReachDistance()) {
                if (chatInfo.get()) ChatUtils.error("Target block out of reach... disabling.");
                toggle();
                return;
            }

            FindItemResult slot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE);
            if (!slot.found()) slot = InvUtils.findInHotbar(Items.DIAMOND_PICKAXE);

            if (!slot.found()) {
                if (chatInfo.get()) ChatUtils.error("No pick found... disabling.");
                toggle();
                return;
            }


            if (support.get()) {
                FindItemResult findItemResult = InvUtils.findInHotbar(Items.OBSIDIAN);
                BlockPos blockPos = mineTarget.down(1);

                if (!BlockUtils.canPlace(blockPos)
                    && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN
                    && mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK
                    && chatInfo.get()) {
                    ChatUtils.warning("Couldn't place support block, mining anyway.");
                } else {
                    if (!findItemResult.found()) {
                        if (chatInfo.get()) ChatUtils.warning("No obsidian found for support, mining anyway.");
                    } else BlockUtils.place(blockPos, findItemResult, rotate.get(), 0, true);

                }
            }

            mc.player.getInventory().selectedSlot = slot.getSlot();

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget), () -> mine(mineTarget));
            else mine(mineTarget);
        }

        this.toggle();
    }

    private void mine(BlockPos blockPos) {
        if (!instant.get()) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}

