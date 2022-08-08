package me.seasnail.meteorplus.modules;

import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Moses extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> lava = sgGeneral.add(new BoolSetting.Builder()
        .name("lava")
        .description("Applies to lava too.")
        .defaultValue(false)
        .build()
    );

    public Moses() {
        super(MeteorPlus.CATEGORY, "moses", "Lets you walk through water as if it was air.");
    }

    public boolean doLava() {
        return lava.get();
    }
}
