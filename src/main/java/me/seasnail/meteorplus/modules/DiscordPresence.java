package me.seasnail.meteorplus.modules;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

public class DiscordPresence extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
        .name("line-1")
        .description("The text on the RPC .")
        .defaultValue("{player}")
        .onChanged(booleanSetting -> updateDetails())
        .build()
    );

    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;
    private SmallImage currentSmallImage;
    private int ticks;

    public DiscordPresence() {
        super(Categories.Misc, "discord-presence", "Displays a RPC for you on Discord to show that you're playing Meteor+!");
    }

    @Override
    public void onActivate() {
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("815376230311657502", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;
        rpc.largeImageKey = "meteor_";
        rpc.largeImageText = "Meteor+ " + MeteorPlus.VERSION;
        rpc.state = "Meteor+ owns me!";

        currentSmallImage = SmallImage.MineGame;
        updateDetails();

        instance.Discord_UpdatePresence(rpc);
        instance.Discord_RunCallbacks();
    }

    @Override
    public void onDeactivate() {
        instance.Discord_ClearPresence();
        instance.Discord_Shutdown();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;
        ticks++;

        if (ticks >= 200) {
            currentSmallImage = currentSmallImage.next();
            currentSmallImage.apply();
            instance.Discord_UpdatePresence(rpc);

            ticks = 0;
        }

        updateDetails();
        instance.Discord_RunCallbacks();
    }

    private String getLine(Setting<String> line) {
        if (line.get().length() > 0) return line.get().replace("{player}", getName()).replace("{server}", getServer());
        else return null;
    }

    private void updateDetails() {
        if (isActive() && Utils.canUpdate()) {
            rpc.details = getLine(line1);
            instance.Discord_UpdatePresence(rpc);
        }
    }

    private String getServer() {
        if (mc.isInSingleplayer()) return "SinglePlayer";
        else return Utils.getWorldName();
    }

    private String getName() {
        return mc.player.getGameProfile().getName();
    }

    private enum SmallImage {
        //Add yourself if you want
        SeaSnail("seasnail", "seasnail"),
        MineGame("minegame", "MineGame"),
        Supakeks("supakeks", "supakeks"),
        Gilded("gilded", "Gilded"),
        WideCat("widecat", "WideCat"),
        Tyrannus("tyrannus", "Tyrannus"),
        Bowlz("bowlz", "Bowlz"),
        SamTheClam("samtheclam", "SamTheClam"),
        GL_DONT_CARE("gldontcare", "GL_DONT_CARE"),
        NotGhostTypes("notghosttypes", "NotGhostTypes");

        private final String key, text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        void apply() {
            rpc.smallImageKey = key;
            rpc.smallImageText = text;
        }

        SmallImage next() {
            return switch (this) {
                case SeaSnail -> MineGame;
                case MineGame -> Supakeks;
                case Supakeks -> Gilded;
                case Gilded -> WideCat;
                case WideCat -> Tyrannus;
                case Tyrannus -> Bowlz;
                case Bowlz -> SamTheClam;
                case SamTheClam -> GL_DONT_CARE;
                case GL_DONT_CARE -> NotGhostTypes;
                default -> SeaSnail;
            };
        }
    }
}
