package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

// NotGhostTypes

public class AutoAnvil extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Placement");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgToggles = settings.createGroup("Checks");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgChat = settings.createGroup("Chat");

    // General Settings
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate-on-place").description("Rotate on placements.").defaultValue(true).build());
    private final Setting<Boolean> autoTrap = sgGeneral.add(new BoolSetting.Builder().name("auto-trap").description("Automatically trap the target").defaultValue(true).build());
    private final Setting<Mode> anvilMode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("trap-mode").description("Which anvil trap mode to use").defaultValue(Mode.Balanced).build());
    private final Setting<Boolean> allowSelfTrap = sgGeneral.add(new BoolSetting.Builder().name("allow-targeting-self").description("Allow targeting players that are in your hole.").defaultValue(false).build());

    // Placement Settings
    private final Setting<Double> range = sgPlace.add(new DoubleSetting.Builder().name("target-range").description("Maximum targeting range.").defaultValue(4).min(0).build());
    private final Setting<Integer> delay = sgPlace.add(new IntSetting.Builder().name("anvil-delay").description("Delay between anvil placements.").min(0).defaultValue(4).sliderMax(50).build());
    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Maximum blocks per tick.").defaultValue(4).min(2).max(8).sliderMin(2).sliderMax(8).build());
    private final Setting<Boolean> placeButton = sgPlace.add(new BoolSetting.Builder().name("place-at-feet").description("Places buttons or pressure plates below the target.").defaultValue(true).build());
    private final Setting<ButtonLogic> buttonMode = sgGeneral.add(new EnumSetting.Builder<ButtonLogic>().name("foot-place-logic").description("When to place the button or presure plate below the target.").defaultValue(ButtonLogic.Before).visible(placeButton::get).build());

    // Safety Settings
    private final Setting<Boolean> safety = sgSafety.add(new BoolSetting.Builder().name("safety").description("Automatic protection at X health.").defaultValue(true).build());
    private final Setting<Integer> safetyHP = sgSafety.add(new IntSetting.Builder().name("health").description("What health to start safety at.").min(0).defaultValue(10).sliderMax(36).build());
    private final Setting<Boolean> safetyGapSwap = sgSafety.add(new BoolSetting.Builder().name("swap-to-gap").description("Swap to egaps.").defaultValue(false).build());

    // Checks Settings
    private final Setting<Boolean> toggleOnBreak = sgToggles.add(new BoolSetting.Builder().name("toggle-on-no-helm").description("Disable if the target's helmet gets broken.").defaultValue(false).build()
    );
    private final Setting<Integer> helmDelay = sgToggles.add(new IntSetting.Builder().name("helm-wait-delay").description("How many ticks to wait before disabling after breaking the targets helmet").min(0).defaultValue(40).sliderMax(80).build()
    );
    private final Setting<Boolean> toggleOnAnvilFail = sgToggles.add(new BoolSetting.Builder().name("toggle-on-placement-fail").description("Disable if you fail to place anvils.").defaultValue(true).build());
    private final Setting<Integer> minAnvilFails = sgToggles.add(new IntSetting.Builder().name("min-anvil-fails").description("How many times you fail to place anvils before disabling.").min(0).defaultValue(4).sliderMax(10).build());
    private final Setting<Boolean> protectOwnHelm = sgToggles.add(new BoolSetting.Builder().name("toggle-on-low-helm").description("Disable if your own helmet has low durability.").defaultValue(false).build());
    private final Setting<Integer> minHelmDura = sgToggles.add(new IntSetting.Builder().name("minimum-helm-durability").description("").defaultValue(20).min(1).sliderMax(100).max(100).visible(protectOwnHelm::get).build());

    // Automation Settings
    private final Setting<Boolean> antiWeb = sgAutomation.add(new BoolSetting.Builder().name("anti-self-web").description("Mine webs the target places on themselves.").defaultValue(false).build());
    private final Setting<Boolean> antiAntiAnvil = sgAutomation.add(new BoolSetting.Builder().name("anti-anti-anvil").description("Mine blocks the target places above themselves.").defaultValue(false).build());
    private final Setting<Boolean> disableCAonStart = sgAutomation.add(new BoolSetting.Builder().name("disable-ca-on-enable").description("Disable CrystalAura while AutoAnvil is active.").defaultValue(false).build());
    private final Setting<Boolean> enableCAonEscape = sgAutomation.add(new BoolSetting.Builder().name("enable-ca-on-escape").description("Enable CrystalAura if the target escapes.").defaultValue(false).build());
    private final Setting<CaMode> caMode = sgGeneral.add(new EnumSetting.Builder<CaMode>().name("ca-mode").description("Which crystal aura to use").defaultValue(CaMode.Venom).build());
    private final Setting<Boolean> swapToCrystals = sgAutomation.add(new BoolSetting.Builder().name("swap-to-crystals").description("Swap to crystals after enabling CrystalAura.").defaultValue(false).build());
    private final Setting<Boolean> cityOnAnvilFail = sgAutomation.add(new BoolSetting.Builder().name("auto-city-on-anvil-fail").description("Enable AutoCity if anvil placement fails (requires disableOnPlacementFail).").defaultValue(false).build());
    private final Setting<Boolean> toggleOnAntiAnvil = sgAutomation.add(new BoolSetting.Builder().name("disable-on-anti-anvil").description("Disable if the target has blocks above themselves.").defaultValue(true).build());

    // Chat Settings
    private final Setting<Boolean> announceHelmBreaks = sgChat.add(new BoolSetting.Builder().name("announce-helmet-breaks").description("Annpunce when you break the target's helmet.").defaultValue(true).build());
    private final Setting<Boolean> announceRunners = sgChat.add(new BoolSetting.Builder().name("announce-escapees").description("Announce when the target escapes/runs away.").defaultValue(true).build());
    private final Setting<Boolean> announceWebs = sgChat.add(new BoolSetting.Builder().name("announce-webs").description("Announce when the target webs themselves.").defaultValue(true).build());
    private final Setting<Boolean> announceAntiAnvil = sgChat.add(new BoolSetting.Builder().name("announce-anti-anvil").description("Announce when the target uses AntiAnvil.").defaultValue(true).build());
    private final Setting<Boolean> announceOutOfHelms = sgChat.add(new BoolSetting.Builder().name("announce-out-of-helms").description("Announce when the target runs out of helmets.").defaultValue(true).build());

    private final ArrayList<Vec3d> anvilTrapSpeed = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));
        add(new Vec3d(0, 3, -1));
        add(new Vec3d(0, 3, 1));
    }};

    private final ArrayList<Vec3d> anvilTrapBalanced = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));
        add(new Vec3d(0, 3, -1));
        add(new Vec3d(0, 3, 1));
        add(new Vec3d(1, 3, 0));
        add(new Vec3d(-1, 3, 0));
        add(new Vec3d(0, 4, -1));
        add(new Vec3d(0, 4, 1));
        add(new Vec3d(1, 4, 0));
        add(new Vec3d(-1, 4, 0));
    }};

    private final ArrayList<Vec3d> anvilTrapDamage = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));
        add(new Vec3d(0, 3, -1));
        add(new Vec3d(0, 3, 1));
        add(new Vec3d(1, 3, 0));
        add(new Vec3d(-1, 3, 0));
        add(new Vec3d(0, 4, -1));
        add(new Vec3d(0, 4, 1));
        add(new Vec3d(1, 4, 0));
        add(new Vec3d(-1, 4, 0));
        add(new Vec3d(0, 5, -1));
        add(new Vec3d(0, 5, 1));
        add(new Vec3d(1, 5, 0));
        add(new Vec3d(-1, 5, 0));
    }};

    private PlayerEntity target;
    private int timer;
    private int anvilsFailed;
    private int helmBreakWait;
    private boolean alertedTarget;
    private boolean alertedTarget2;
    private boolean alertedPressure;
    private boolean alertedTrap;
    private boolean anvilFailure;
    private boolean alertedWeb;
    private boolean alertedTtrap;
    private boolean announcedHelmBreak;
    private boolean announcedWebs;
    private boolean announcedAntiAnvil;

    public AutoAnvil() {
        super(MeteorPlus.CATEGORY, "auto-anvil", "Helm go brr");
    }

    @Override
    public void onActivate() {
        if (disableCAonStart.get()) {
            if (caMode.get() == CaMode.Venom) {
                if (Modules.get().get(VenomCrystal.class).isActive()) {
                    Modules.get().get(VenomCrystal.class).toggle();
                }
            }
            else if (caMode.get() == CaMode.Meteor) {
                if (Modules.get().get(CrystalAura.class).isActive()) {
                    Modules.get().get(CrystalAura.class).toggle();
                }
            }
        }

        timer = 0;
        anvilsFailed = 0;
        helmBreakWait = 0;
        target = null;
        alertedTarget = false;
        alertedTarget2 = false;
        alertedPressure = false;
        alertedTrap = false;
        anvilFailure = false;
        alertedWeb = false;
        alertedTtrap = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AnvilScreen) mc.player.closeScreen();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult anvilSlot = InvUtils.findInHotbar(itemStack -> {
                Item item = itemStack.getItem();
                Block block = Block.getBlockFromItem(item);
                return (block instanceof AnvilBlock);
            }
        );
        if (!anvilSlot.found()) {
            ChatUtils.error("No anvils in hotbar, disabling.");
            toggle();
            return;
        }
        //Safety Checks
        if (safety.get() && mc.player.getHealth() + mc.player.getAbsorptionAmount() < safetyHP.get()) {
            ChatUtils.error("Your health is low! Disabling.");
            if (safetyGapSwap.get()) {
                int gapSlot = getGapSlot();
                if (gapSlot == -1) {
                    ChatUtils.error("No gaps in hotbar, unable to safety swap!");
                }
                mc.player.getInventory().selectedSlot = gapSlot;
            }
            toggle();
            return;
        }
        if (protectOwnHelm.get()) {
            ItemStack stack = mc.player.getInventory().getArmorStack(3);
            if (Math.round(((stack.getMaxDamage() - stack.getDamage()) * 100f) / (float) stack.getMaxDamage()) <= minHelmDura.get()) {
                ChatUtils.error("Your helmet is low! Disabling.");
                toggle();
                return;
            }
        }
        if (target != null && (mc.player.distanceTo(target) > range.get() || !target.isAlive())) {
            if (enableCAonEscape.get()) {
                if (swapToCrystals.get()) {
                    FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                    if (crystalSlot.found()) {
                        mc.player.getInventory().selectedSlot = crystalSlot.getSlot();
                    }
                }
                if (caMode.get() == CaMode.Meteor) {
                    Modules.get().get(CrystalAura.class).toggle();
                }
                if (caMode.get() == CaMode.Venom) {
                    Modules.get().get(VenomCrystal.class).toggle();
                }
                ChatUtils.error(target.getGameProfile().getName() + " has moved out of range, swapping to CA.");
                if (announceRunners.get()) {
                    mc.player.sendChatMessage("I just made " + target.getGameProfile().getName() + " run thanks to Meteor+!");
                }
                toggle();
                return;
            }
            ChatUtils.error(target.getGameProfile().getName() + " has moved out of range, locating new target.");
            target = null;
        }
        if (target != null && isTrapFinished(target)) {
            if (toggleOnAntiAnvil.get() && isTargetAntiAnvilled(target)) {
                ChatUtils.error(target.getGameProfile().getName() + " has blocks above them, disabling.");
                toggle();
                return;
            }
            if (antiWeb.get() && mc.world.getBlockState(target.getBlockPos()).getBlock() == Blocks.COBWEB) {
                if (!alertedWeb) {
                    ChatUtils.error(target.getGameProfile().getName() + " is webbed, breaking...");
                    alertedWeb = true;
                }
                if (announceWebs.get() && !announcedWebs) {
                    mc.player.sendChatMessage(target.getGameProfile().getName() + " got ez'd by anvils and had to web themselves thanks to Meteor+!");
                    announcedWebs = true;
                }
                FindItemResult swordSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof SwordItem);
                if (!swordSlot.found()) {
                    ChatUtils.error("No sword found in hotbar, disabling.");
                    toggle();
                    return;
                }

                mc.player.getInventory().selectedSlot = swordSlot.getSlot();
                mc.interactionManager.updateBlockBreakingProgress(target.getBlockPos(), Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            } else {
                if (alertedWeb) {
                    ChatUtils.info("Finished mining webs.");
                }
                alertedWeb = false;
                announcedWebs = false;
            }
            if (antiAntiAnvil.get() && isTargetAntiAnvilled(target)) {
                if (!alertedTtrap) {
                    ChatUtils.error(target.getGameProfile().getName() + " has a block above themselves, mining...");
                    alertedTtrap = true;
                }
                if (announceAntiAnvil.get() && !announcedAntiAnvil) {
                    mc.player.sendChatMessage(target.getGameProfile().getName() + " got ez'd by anvils and had to trap themselves thanks to Meteor+!");
                    announcedAntiAnvil = true;
                }
                FindItemResult pickSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
                if (!pickSlot.found()) {
                    ChatUtils.error("No pickaxe found in hotbar, disabling.");
                    toggle();
                    return;
                }
                mc.player.getInventory().selectedSlot = pickSlot.getSlot();
                mc.interactionManager.updateBlockBreakingProgress(target.getBlockPos().add(0, 2, 0), Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            } else {
                if (alertedTtrap) {
                    ChatUtils.info("Finished mining block.");
                }
                alertedTtrap = false;
                announcedAntiAnvil = false;
            }
            //Place the anvil above the target
            mc.player.getInventory().selectedSlot = anvilSlot.getSlot();
            if (anvilMode.get() == Mode.Speed) {
                BlockPos blockPos = target.getBlockPos().add(0, 3, 0);
                if (mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) {
                    BlockUtils.place(blockPos, anvilSlot, rotate.get(), 50, true);
                } else {
                    anvilsFailed++;
                    if (toggleOnAnvilFail.get() && anvilsFailed >= minAnvilFails.get()) {
                        anvilFailure = true;
                    }
                }
            }
            if (anvilMode.get() == Mode.Balanced) {
                BlockPos blockPos = target.getBlockPos().add(0, 4, 0);
                if (mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) {
                    BlockUtils.place(blockPos, anvilSlot, rotate.get(), 50, true);
                } else {
                    anvilsFailed++;
                    if (toggleOnAnvilFail.get() && anvilsFailed >= minAnvilFails.get()) {
                        anvilFailure = true;
                    }
                }
            }
            if (anvilMode.get() == Mode.Damage) {
                BlockPos blockPos = target.getBlockPos().add(0, 5, 0);
                if (mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) {
                    BlockUtils.place(blockPos, anvilSlot, rotate.get(), 50, true);
                } else {
                    anvilsFailed++;
                    if (toggleOnAnvilFail.get() && anvilsFailed >= minAnvilFails.get()) {
                        anvilFailure = true;
                    }
                }
            }
            if (anvilFailure) {
                if (toggleOnAnvilFail.get()) {
                    if (cityOnAnvilFail.get()) {
                        if (!Modules.get().get(AutoCityPlus.class).isActive()) {
                            Modules.get().get(AutoCityPlus.class).toggle();
                        }
                        ChatUtils.error("Failed to place anvils, trying to city target");
                    } else {
                        ChatUtils.error("Failed to place anvils, disabling");
                    }
                }
                toggle();
                return;
            }
        }

        //Targeting
        if (isActive() && toggleOnBreak.get() && target != null && target.getInventory().getArmorStack(3).isEmpty()) {
            if (announceHelmBreaks.get() && !announcedHelmBreak) {
                mc.player.sendChatMessage("I just broke " + target.getGameProfile().getName() + "'s helmet thanks to Meteor+!");
                announcedHelmBreak = true;
            }
            if (helmBreakWait > helmDelay.get()) {
                if (announceOutOfHelms.get()) {
                    mc.player.sendChatMessage("I just broke all of " + target.getGameProfile().getName() + "'s helmets thanks to Meteor+!");
                }
                ChatUtils.error(target.getGameProfile().getName() + "is out of helmets, disabling.");
                toggle();
                return;
            } else {
                helmBreakWait++;
            }
        } else {
            announcedHelmBreak = false;
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !Friends.get().shouldAttack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get() || isHole(player.getBlockPos())) {
                continue;
            }
            if (target == null) {
                target = player;
            } else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) {
                target = player;
            }
        }
        if (target == null) {
            ChatUtils.error("No targets in range, disabling.");
            toggle();
            return;
        }
        if (!allowSelfTrap.get() && mc.player.distanceTo(target) <= 1) {
            ChatUtils.error(target.getGameProfile().getName() + " moved into your hole, disabling.");
            toggle();
            return;
        }
        if (!alertedTarget) {
            ChatUtils.info("Found new target " + target.getGameProfile().getName());
            alertedTarget = true;
        }

        //lastTarget = target;

        int blocksPlaced = 0;
        if (timer >= delay.get() && target != null) {
            timer = 0;
            if (placeButton.get() && buttonMode.get() == ButtonLogic.Before) {
                FindItemResult floorSlot = InvUtils.findInHotbar(itemStack -> {
                        Item item = itemStack.getItem();
                        Block block = Block.getBlockFromItem(item);
                        return (block instanceof PressurePlateBlock || block instanceof AbstractButtonBlock);
                    }
                );
                if (!floorSlot.found()) {
                    ChatUtils.error("No buttons/plates in hotbar, disabling.");
                    toggle();
                    return;
                }
                mc.player.getInventory().selectedSlot = floorSlot.getSlot();
                BlockPos blockPos = target.getBlockPos();
                if (BlockUtils.place(blockPos, floorSlot, rotate.get(), 0, false, true, true)) {
                    if (!alertedPressure) {
                        ChatUtils.info("Pressure plate ready.");
                        alertedPressure = true;
                    }
                    blocksPlaced++;
                }
            }
            if (autoTrap.get()) {
                FindItemResult slotObby = InvUtils.findInHotbar(Blocks.OBSIDIAN.asItem());
                if (!slotObby.found()) {
                    ChatUtils.error("No obsidian in hotbar, disabling.");
                    toggle();
                    return;
                }

                mc.player.getInventory().selectedSlot = slotObby.getSlot();

                if (!alertedTrap) {
                    ChatUtils.info("Trapping " + target.getGameProfile().getName());
                    alertedTrap = true;
                }

                //Build the anvil trap around the target easily
                if (anvilMode.get() == Mode.Speed) {
                    for (Vec3d blockSpecific : anvilTrapSpeed) {
                        BlockPos posBlock = target.getBlockPos().add(blockSpecific.x, blockSpecific.y, blockSpecific.z);
                        if (mc.world.getBlockState(posBlock).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), posBlock, ShapeContext.absent())) {
                            if (BlockUtils.place(posBlock, slotObby, rotate.get(), 50, true)) {
                                if (++blocksPlaced == blocksPerTick.get()) {
                                    return;
                                }
                            }
                        }
                    }
                }
                if (anvilMode.get() == Mode.Balanced) {
                    for (Vec3d blockSpecific : anvilTrapBalanced) {
                        BlockPos posBlock = target.getBlockPos().add(blockSpecific.x, blockSpecific.y, blockSpecific.z);
                        if (mc.world.getBlockState(posBlock).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), posBlock, ShapeContext.absent())) {
                            if (BlockUtils.place(posBlock, slotObby, rotate.get(), 50, true)) {
                                if (++blocksPlaced == blocksPerTick.get()) {
                                    return;
                                }
                            }
                        }
                    }
                }
                if (anvilMode.get() == Mode.Damage) {
                    for (Vec3d blockSpecific : anvilTrapDamage) {
                        BlockPos posBlock = target.getBlockPos().add(blockSpecific.x, blockSpecific.y, blockSpecific.z);
                        if (mc.world.getBlockState(posBlock).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), posBlock, ShapeContext.absent())) {
                            if (BlockUtils.place(posBlock, slotObby, rotate.get(), 50, true)) {
                                if (++blocksPlaced == blocksPerTick.get()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            if (isTrapFinished(target)) {
                if (!alertedTarget2) {
                    ChatUtils.info("Finished Trapping " + target.getGameProfile().getName());
                    alertedTarget2 = true;
                }
            } else {
                return;
            }

            if (placeButton.get() && buttonMode.get() == ButtonLogic.After) {
                FindItemResult floorSlot = InvUtils.findInHotbar(itemStack -> {
                        Item item = itemStack.getItem();
                        Block block = Block.getBlockFromItem(item);
                        return (block instanceof PressurePlateBlock || block instanceof AbstractButtonBlock);
                    }
                );
                if (!floorSlot.found()) {
                    ChatUtils.error("No buttons/plates in hotbar, disabling.");
                    toggle();
                    return;
                }
                mc.player.getInventory().selectedSlot = floorSlot.getSlot();
                BlockPos blockPos = target.getBlockPos();
                if (BlockUtils.place(blockPos, floorSlot, rotate.get(), 0, true, false, true)) {
                    if (!alertedPressure) {
                        ChatUtils.info("Pressure plate ready.");
                        alertedPressure = true;
                    }
                    blocksPlaced++;
                }
            }
        } else {
            timer++;
        }
    }

    public boolean isHole(BlockPos pos) {
        BlockPos.Mutable posStart = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
        return mc.world.getBlockState(posStart.add(1, 0, 0)).getBlock() == Blocks.AIR ||
            mc.world.getBlockState(posStart.add(-1, 0, 0)).getBlock() == Blocks.AIR ||
            mc.world.getBlockState(posStart.add(0, 0, 1)).getBlock() == Blocks.AIR ||
            mc.world.getBlockState(posStart.add(0, 0, -1)).getBlock() == Blocks.AIR;
    }

    public int getFloorSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AbstractPressurePlateBlock || block instanceof AbstractButtonBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getAnvilSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AnvilBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getGapSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item == Items.ENCHANTED_GOLDEN_APPLE) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private boolean isTrapFinished(PlayerEntity target) {
        for (Vec3d blockSpecific : anvilTrapBalanced) {
            BlockPos posBlock = target.getBlockPos().add(blockSpecific.x, blockSpecific.y, blockSpecific.z);
            if (mc.world.getBlockState(posBlock).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), posBlock, ShapeContext.absent())) {
                return false;
            }
        }
        return true;
    }

    private boolean isTargetAntiAnvilled(PlayerEntity target) {
        return mc.world.getBlockState(target.getBlockPos().add(0, 2, 0)).getBlock() == Blocks.OBSIDIAN;
    }

    public enum Mode {
        Speed,
        Balanced,
        Damage,
    }

    public enum ButtonLogic {
        Before,
        After
    }

    public enum CaMode {
        Meteor,
        Venom
    }

}
