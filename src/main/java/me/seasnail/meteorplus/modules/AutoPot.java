package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AnchorAura;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoPot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPotions = settings.createGroup("Potions");

    private final Setting<Boolean> Healing = sgPotions.add(new BoolSetting.Builder()
        .name("healing")
        .description("Enables healing potions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> Timed_Potion = sgPotions.add(new EnumSetting.Builder<Mode>()
        .name("timed")
        .description("Which Timed Potion will Autopot consume.")
        .defaultValue(Mode.Strength)
        .build()
    );

    private final Setting<Boolean> useSplashPots = sgGeneral.add(new BoolSetting.Builder()
        .name("splash")
        .description("Allow the use of splash pots")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("If health goes below this point, Healing Pot will trigger.")
        .defaultValue(15)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> pauseAuras = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-auras")
        .description("Pauses all auras when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Pause baritone when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Forces you to rotate downwards when throwing bottles.")
        .defaultValue(true)
        .build()
    );

    private static final Class<? extends Module>[] AURAS = new Class[]{KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class};
    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private int slot, prevSlot;
    private boolean drinking, splashing;

    public AutoPot() {
        super(MeteorPlus.CATEGORY, "auto-pot", "Automatically Drinks Potions");
    }
    //Gilded's first module, lets see how much i'll die making this
    //TODO: Rework everything to accept all pots

    @Override
    public void onDeactivate() {
        if (drinking) stopDrinking();
        if (splashing) stopSplashing();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Healing.get()) {
            if (ShouldDrinkHealth()) {
                //Heal Pot Slot
                int slot = HealingpotionSlot();
                //Slot Not Invalid
                if (slot != -1) {
                    startDrinking();
                } else if (HealingpotionSlot() == -1 && useSplashPots.get()) {
                    slot = HealingSplashpotionSlot();
                    if (slot != -1) {
                        startSplashing();
                    }
                }
            }
            if (drinking) {
                if (ShouldDrinkHealth()) {
                    if (isNotPotion(mc.player.getInventory().getStack(slot))) {
                        slot = HealingpotionSlot();
                        if (slot == -1) {
                            ChatUtils.info("Ran out of Pots while drinking");
                            stopDrinking();
                            return;
                        }
                    } else changeSlot(slot);
                }
                drink();
                if (ShouldNotDrinkHealth()) {
                    ChatUtils.info("Health Full");
                    stopDrinking();
                    return;
                }
            }
            if (splashing) {
                if (ShouldDrinkHealth()) {
                    if (isNotSplashPotion(mc.player.getInventory().getStack(slot))) {
                        slot = HealingSplashpotionSlot();
                        if (slot == -1) {
                            ChatUtils.info("Ran out of Pots while splashing");
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }
                    splash();
                    if (ShouldNotDrinkHealth()) {
                        ChatUtils.info("Health Full");
                        stopSplashing();
                        return;
                    }
                }
            }
        }
        if (Timed_Potion.get() == Mode.Strength) {
            if (ShouldDrinkStrengthPot()) {
                //Strength Pot Slot
                int slot = StrengthpotionSlot();
                //Slot Not Invalid
                if (slot != -1) {
                    startDrinking();
                }
                else if (StrengthpotionSlot() == -1 && useSplashPots.get()) {
                    slot = StrengthSplashpotionSlot();
                    if (slot != -1) {
                        startSplashing();
                    }
                }
            }
            if (drinking) {
                if (ShouldDrinkStrengthPot()) {
                    if (isNotPotion(mc.player.getInventory().getStack(slot))) {
                        slot = StrengthpotionSlot();
                        if (slot == -1) {
                            stopDrinking();
                            ChatUtils.info("Out of Pots");
                            return;
                        } else changeSlot(slot);
                    }
                    drink();
                } else {
                    stopDrinking();
                }
            }
            if (splashing) {
                if (ShouldDrinkStrengthPot()) {
                    if (isNotSplashPotion(mc.player.getInventory().getStack(slot))) {
                        slot = StrengthSplashpotionSlot();
                        if (slot == -1) {
                            ChatUtils.info("Ran out of Pots while splashing");
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }
                    splash();
                } else {
                    stopSplashing();
                }
            }
        }
        if (Timed_Potion.get() == Mode.Regeneration) {
            if (ShouldDrinkRegenPot()) {
                //Regen Pot Slot
                int slot = RegenpotionSlot();
                //Slot Not Invalid
                if (slot != -1) {
                    startDrinking();
                }
                else if (RegenpotionSlot() == -1 && useSplashPots.get()) {
                    slot = RegenSplashpotionSlot();
                    if (slot != -1) {
                        startSplashing();
                    }
                }
            }
            if (drinking) {
                if (ShouldDrinkRegenPot()) {
                    if (isNotPotion(mc.player.getInventory().getStack(slot))) {
                        slot = RegenpotionSlot();
                        if (slot == -1) {
                            stopDrinking();
                            ChatUtils.info("Out of Pots");
                            return;
                        } else changeSlot(slot);
                    }
                    drink();
                } else {
                    stopDrinking();
                }
            }
            if (splashing) {
                if (ShouldDrinkRegenPot()) {
                    if (isNotSplashPotion(mc.player.getInventory().getStack(slot))) {
                        slot = RegenSplashpotionSlot();
                        if (slot == -1) {
                            ChatUtils.info("Ran out of Pots while splashing");
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }
                    splash();
                } else {
                    stopSplashing();
                }
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (drinking) event.target = null;
    }

    private void setPressed(boolean pressed) {
        mc.options.keyUse.setPressed(pressed);
    }

    private void startDrinking() {
        prevSlot = mc.player.getInventory().selectedSlot;
        drink();
        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }
        // Pause baritone
        // TODO: fix
        /*wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }*/
    }

    private void startSplashing() {
        prevSlot = mc.player.getInventory().selectedSlot;
        if (lookDown.get()) {
            Rotations.rotate(mc.player.getYaw(), 90);
            splash();
        }
        splash();
        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }
        // Pause baritone
        // TODO: Fix
        /*wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }*/
    }

    private void drink() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();

        drinking = true;
    }

    private void splash() {
        changeSlot(slot);
        setPressed(true);
        splashing = true;
    }

    private void stopDrinking() {
        changeSlot(prevSlot);
        setPressed(false);
        drinking = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }
        // Resume baritone
        // TODO: Fix
        /*if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }*/
    }

    private void stopSplashing() {
        changeSlot(prevSlot);
        setPressed(false);

        splashing = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }
        // Resume baritone
        // TODO: Fix
        /*if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }*/
    }

    private double truehealth() {
        assert mc.player != null;
        return mc.player.getHealth();
    }

    private void changeSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        this.slot = slot;
    }

    //Sunk 7 hours into these checks, if i die blame checks
    //also blame keks
    //there must be a better way to check

    //Heal pot checks
    //ok and? -supakeks
    //cope -Gilded
    private int HealingpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }
        return slot;
    }

    //Sunk 7 hours into these checks, if i die blame checks
    //also blame keks
    //there must be a better way to check

    private int HealingSplashpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }
        return slot;
    }

    //Strength Pot Checks
    private int StrengthSplashpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                        slot = i;
                        break;
                    }
                }

            }
        }
        return slot;
    }

    private int StrengthpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                        slot = i;
                        break;
                    }
                }

            }
        }
        return slot;
    }

    //Regen Pot check
    private int RegenpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.regeneration")) {
                        slot = i;
                        break;
                    }
                }

            }
        }
        return slot;
    }

    private int RegenSplashpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.getInventory().getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.regeneration")) {
                        slot = i;
                        break;
                    }
                }

            }
        }
        return slot;
    }

    private boolean isNotPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.POTION;
    }

    private boolean isNotSplashPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.SPLASH_POTION;
    }

    private boolean ShouldDrinkHealth() {
        return truehealth() < health.get();
    }

    private boolean ShouldNotDrinkHealth() {
        if (slot == HealingpotionSlot() || slot == HealingSplashpotionSlot()) {
            return truehealth() >= health.get();
        }
        return false;
    }

    //will fix later, i know it's bloated
    private boolean ShouldDrinkRegenPot() {
        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();
        //Regen
        if (Timed_Potion.get() == Mode.Regeneration && !effects.containsKey(StatusEffects.REGENERATION)) return true;
        else if (Timed_Potion.get() == Mode.Regeneration && effects.containsKey(StatusEffects.REGENERATION)) return false;
        return false;
    }

    private boolean ShouldDrinkStrengthPot() {
        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();
        //Regen
        if (Timed_Potion.get() == Mode.Strength && !effects.containsKey(StatusEffects.STRENGTH)) return true;
        else if (Timed_Potion.get() == Mode.Strength && effects.containsKey(StatusEffects.STRENGTH)) return false;
        return false;
    }

    public enum Mode {
        Regeneration,
        Strength
    }
}
