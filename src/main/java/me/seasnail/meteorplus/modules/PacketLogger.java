/*
    Code pasted from https://github.com/haykam821/Packet-Logger
    All credits given to haykam821
 */

package me.seasnail.meteorplus.modules;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.mixin.CustomPayloadC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.Packet;
import net.minecraft.text.BaseText;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import me.seasnail.meteorplus.MeteorPlus;
import me.seasnail.meteorplus.mixins.CustomPayloadS2CPacketAccessor;
import me.seasnail.meteorplus.utils.IntermediaryDeobfuscator;

import java.lang.reflect.Field;
import java.util.Set;

public class PacketLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("logging-mode")
        .description("The method to log the packet data.")
        .defaultValue(Mode.Chat)
        .build()
    );

    public final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("S2C-packets")
        .description("Server-to-client packets to log.")
        .defaultValue(new ObjectOpenHashSet<>(0))
        .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
        .build()
    );

    public final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to log.")
        .defaultValue(new ObjectOpenHashSet<>(0))
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );

    private final Logger LOGGER = LogManager.getLogger("Packet Logger");

    public PacketLogger() {
        super(MeteorPlus.CATEGORY, "packet-logger", "Logs sent and received packets for both the client and server.");
    }

    public void logSentPacket(Packet<?> packet) {
        switch (mode.get()) {
            case Console -> {
                String message = "\nSent: " + trimClassName(IntermediaryDeobfuscator.exactMap(packet.getClass().getName()));
                if (getPacketChannel(packet) != null) message += "\nChannel: " + getPacketChannel(packet).toString();
                message += formatAndMapObjectFields(packet);
                LOGGER.info(message);
            }
            case Chat -> {
                BaseText text = new LiteralText("Sent: ");
                text.formatted(Formatting.GRAY);
                BaseText name = new LiteralText(trimClassName(IntermediaryDeobfuscator.exactMap(packet.getClass().getName())));
                name.formatted(Formatting.GREEN, Formatting.UNDERLINE);
                BaseText tooltip = new LiteralText("");
                Field[] fields = packet.getClass().getDeclaredFields();
                try {
                    for (Field field : fields) {
                        field.setAccessible(true);

                        tooltip.append(new LiteralText(IntermediaryDeobfuscator.exactMap(field.getName()) + ": ").formatted(Formatting.WHITE));
                        tooltip.append(new LiteralText(IntermediaryDeobfuscator.vaugeMap(String.valueOf(field.get(packet))) + (field == fields[fields.length - 1] ? "" : "\n")).formatted(Formatting.GRAY));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip);
                name.setStyle(name.getStyle().withHoverEvent(event));
                text.append(name);
                info(text);
            }
        }
    }

    public void logReceivedPacket(Packet<?> packet) {
        switch (mode.get()) {
            case Console -> {
                String message = "\nReceived: " + trimClassName(IntermediaryDeobfuscator.exactMap(packet.getClass().getName()));
                if (getPacketChannel(packet) != null) message += "\nChannel: " + getPacketChannel(packet).toString();
                message += formatAndMapObjectFields(packet);
                LOGGER.info(message);
            }
            case Chat -> {
                BaseText text = new LiteralText("Received: ");
                text.formatted(Formatting.GRAY);
                BaseText name = new LiteralText(trimClassName(IntermediaryDeobfuscator.exactMap(packet.getClass().getName())));
                name.formatted(Formatting.RED, Formatting.UNDERLINE);
                BaseText tooltip = new LiteralText("");
                Field[] fields = packet.getClass().getDeclaredFields();
                try {
                    for (Field field : fields) {
                        field.setAccessible(true);

                        tooltip.append(new LiteralText(IntermediaryDeobfuscator.exactMap(field.getName()) + ": ").formatted(Formatting.WHITE));
                        tooltip.append(new LiteralText(IntermediaryDeobfuscator.vaugeMap(String.valueOf(field.get(packet))) + (field == fields[fields.length - 1] ? "" : "\n")).formatted(Formatting.GRAY));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip);
                name.setStyle(name.getStyle().withHoverEvent(event));
                text.append(name);
                info(text);
            }
        }
    }

    private String formatAndMapObjectFields(Object obj) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nData: {\n");

        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                sb.append("    \"").append(IntermediaryDeobfuscator.exactMap(f.getName())).append("\"")
                    .append(" : ")
                    .append("\"").append(IntermediaryDeobfuscator.vaugeMap(String.valueOf(f.get(obj)))).append("\"")
                    .append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append("}");

        return sb.toString();
    }

    private Identifier getPacketChannel(Packet<?> packet) {
        if (packet instanceof CustomPayloadC2SPacketAccessor) return ((CustomPayloadC2SPacketAccessor) packet).getChannel();
        else if (packet instanceof CustomPayloadS2CPacketAccessor) return ((CustomPayloadS2CPacketAccessor) packet).getChannel();
        else return null;
    }

    private String trimClassName(String packagedClassName) {
        int lastDot = packagedClassName.lastIndexOf('.');
        if (lastDot != -1) packagedClassName = packagedClassName.substring(lastDot + 1);
        return packagedClassName.replaceAll("\\$", ".");
    }

    public enum Mode {
        Chat,
        Console
    }
}
