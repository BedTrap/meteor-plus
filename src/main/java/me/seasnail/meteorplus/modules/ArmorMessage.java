/*
  Pasted from Phobos lol
 */

package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public class ArmorMessage extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Integer> durability = sgDefault.add(new IntSetting.Builder()
        .name("durability")
        .description("The durability the armor has to be at to notify your friend.")
        .defaultValue(20)
        .min(1).max(100)
        .sliderMax(100)
        .build()
    );

    private final Setting<String> messageToSend = sgDefault.add(new StringSetting.Builder()
        .name("message")
        .description("The message to send.")
        .defaultValue("Watchout {player}, your {piece} have low durability!")
        .build()
    );

    private final Map<PlayerEntity, Integer> entityArmorArraylist = new HashMap<PlayerEntity, Integer>();

    public ArmorMessage() {
        super(MeteorPlus.CATEGORY, "armor-message", "Sends a private message to your friends when one of their armor pieces has low durability.");
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isDead() || Friends.get().shouldAttack(player)) continue;
            for (ItemStack stack : player.getInventory().player.getArmorItems()) {
                if (stack == ItemStack.EMPTY) continue;
                if (Math.round(((stack.getMaxDamage() - stack.getDamage()) * 100f) / (float) stack.getMaxDamage()) <= durability.get() && !entityArmorArraylist.containsKey(player)) {
                    String message = messageToSend.get().replace("{player}", player.getName().asString()).replace("{piece}", getArmorPieceName(stack));
                    mc.player.sendChatMessage("/msg " + player.getName().asString() + " " + message);
                    entityArmorArraylist.put(player, player.getInventory().armor.indexOf(stack));
                }
                if (!entityArmorArraylist.containsKey(player) || player.getInventory().armor.get(entityArmorArraylist.get(player).intValue()) != ItemStack.EMPTY)
                    continue;
                entityArmorArraylist.remove(player);
            }
        }
    }

    private String getArmorPieceName(ItemStack stack) {
        if (stack.getItem() == Items.DIAMOND_HELMET || stack.getItem() == Items.GOLDEN_HELMET || stack.getItem() == Items.IRON_HELMET || stack.getItem() == Items.CHAINMAIL_HELMET || stack.getItem() == Items.LEATHER_HELMET || stack.getItem() == Items.NETHERITE_HELMET) {
            return "helmet";
        }
        if (stack.getItem() == Items.DIAMOND_CHESTPLATE || stack.getItem() == Items.GOLDEN_CHESTPLATE || stack.getItem() == Items.IRON_CHESTPLATE || stack.getItem() == Items.CHAINMAIL_CHESTPLATE || stack.getItem() == Items.LEATHER_CHESTPLATE || stack.getItem() == Items.NETHERITE_CHESTPLATE) {
            return "chestplate";
        }
        if (stack.getItem() == Items.DIAMOND_LEGGINGS || stack.getItem() == Items.GOLDEN_LEGGINGS || stack.getItem() == Items.IRON_LEGGINGS || stack.getItem() == Items.CHAINMAIL_LEGGINGS || stack.getItem() == Items.LEATHER_LEGGINGS || stack.getItem() == Items.NETHERITE_LEGGINGS) {
            return "leggings";
        }
        if (stack.getItem() == Items.DIAMOND_BOOTS || stack.getItem() == Items.GOLDEN_BOOTS || stack.getItem() == Items.IRON_BOOTS || stack.getItem() == Items.CHAINMAIL_BOOTS || stack.getItem() == Items.LEATHER_BOOTS || stack.getItem() == Items.NETHERITE_BOOTS) {
            return "boots";
        }
        return "armor";
    }
}
