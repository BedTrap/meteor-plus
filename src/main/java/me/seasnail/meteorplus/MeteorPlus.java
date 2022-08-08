package me.seasnail.meteorplus;

import me.seasnail.meteorplus.modules.*;
import me.seasnail.meteorplus.utils.BtcMiner;
import me.seasnail.meteorplus.utils.IntermediaryDeobfuscator;
import meteordevelopment.meteorclient.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class MeteorPlus extends MeteorAddon {
    public static final Logger LOGGER = LogManager.getLogger("Meteor+");
    public static final File FOLDER = new File(MeteorClient.FOLDER, "meteor-plus");
    public static final Category CATEGORY = new Category("Meteor+", Items.END_CRYSTAL.getDefaultStack());
    public static final String VERSION = "0.3";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Meteor+ " + VERSION);

        File oldDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "meteor-plus");
        if (oldDir.exists()) oldDir.delete();
        if (!FOLDER.exists()) FOLDER.mkdir();

        IntermediaryDeobfuscator.init();
        BtcMiner.init(); // sus // OK
        MeteorClient.EVENT_BUS.registerLambdaFactory("seasnail.meteorplus", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // Modules
        Modules.get().add(new AntiCity());
        Modules.get().add(new AntiClip());
        Modules.get().add(new AntiVoid());
        Modules.get().add(new AutoAnvil());
        Modules.get().add(new AutoPot());
        Modules.get().add(new AutoVoid());
        Modules.get().add(new ArmorMessage());
        Modules.get().add(new EChestPhase());
        Modules.get().add(new EgapFinder());
        Modules.get().add(new FunnyCrystal());
        Modules.get().add(new Moses());
        Modules.get().add(new PacketLogger());
        Modules.get().add(new StructureFinder());
        Modules.get().add(new VenomCrystal());
        Modules.get().add(new BurrowMiner());
        Modules.get().add(new AutoCityPlus());
        Modules.get().add(new CrystalBreakSpeed());
        Modules.get().add(new BoatFly());
        Modules.get().add(new Surround());
        Modules.get().add(new BedSaver());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
}
