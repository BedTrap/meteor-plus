package me.seasnail.meteorplus.modules;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.structure.*;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.mcutils.util.pos.RPos;
import kaptainwutax.mcutils.version.MCVersion;
import me.seasnail.meteorplus.MeteorPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StructureFinder extends Module {

    public enum OwStructures {
        buried_treasure,
        desert_temple,
        igloo,
        jungle_temple,
        mansion,
        monument,
        outpost,
        ruined_portal,
        witch_hut,
        village
    }
    public enum NetherStructures {
        bastion_remnant,
        ruined_portal,
        fortress
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> seed = sgGeneral.add(new StringSetting.Builder()
            .name("seed")
            .description("The seed of the world you want to locate the structure in.")
            .defaultValue("0")
            .build()
    );

    private final Setting<OwStructures> owStructure = sgGeneral.add(new EnumSetting.Builder<OwStructures>()
            .name("ow-structure")
            .description("The overworld structure to find.")
            .defaultValue(OwStructures.desert_temple)
            .build()
    );

    private final Setting<NetherStructures> netherStructure = sgGeneral.add(new EnumSetting.Builder<NetherStructures>()
            .name("nether-structure")
            .description("The nether structure to find.")
            .defaultValue(NetherStructures.fortress)
            .build()
    );

    private final Setting<Integer> structureCount = sgGeneral.add(new IntSetting.Builder()
            .name("structure-count")
            .description("How many structures to find (higher values will take longer to complete, may crash).")
            .sliderMax(512)
            .sliderMin(1)
            .defaultValue(10)
            .build()
    );

    private final Setting<Boolean> addWaypoint = sgGeneral.add(new BoolSetting.Builder()
            .name("add-waypoint")
            .description("Whether or not to add a waypoint at the found structure/s")
            .defaultValue(false)
            .build()
    );

    //Overworld
    private final DesertPyramid desert_temple = new DesertPyramid(MCVersion.v1_16);
    private final RuinedPortal ruined_portal_ow = new RuinedPortal(Dimension.OVERWORLD, MCVersion.v1_16);
    private final BuriedTreasure buried_treasure = new BuriedTreasure(MCVersion.v1_16);
    private final Igloo igloo = new Igloo(MCVersion.v1_16);
    private final JunglePyramid jungle_temple = new JunglePyramid(MCVersion.v1_16);
    private final Mansion mansion = new Mansion(MCVersion.v1_16);
    private final Monument monument = new Monument(MCVersion.v1_16);
    private final PillagerOutpost outpost = new PillagerOutpost(MCVersion.v1_16);
    private final SwampHut witch_hut = new SwampHut(MCVersion.v1_16);
    private final Village village = new Village(MCVersion.v1_16);
    //    private final Mineshaft mineshaft = new Mineshaft(MCVersion.v1_16);
    //    private final Stronghold stronghold = new Stronghold(MCVersion.v1_16);

    //End
    private final EndCity end_city = new EndCity(MCVersion.v1_16);

    //Nether
    private final Fortress fortress = new Fortress(MCVersion.v1_16);
    private final BastionRemnant bastion_remnant = new BastionRemnant(MCVersion.v1_16);
    private final RuinedPortal ruined_portal_nether = new RuinedPortal(Dimension.NETHER, MCVersion.v1_16);

    private RegionStructure<?, ?> structure;
    private Dimension dimension;
    private final ArrayList<BPos> bPosArrayList = new ArrayList<>();
    private Waypoint waypoint;

    public StructureFinder() {
        super(MeteorPlus.CATEGORY, "Structure Finder", "Finds the closest n given structure/s, works kinda like /locate (atm broken with mineshafts and strongholds");
    }

    @Override
    public void onActivate(){
        boolean foundStructure = false;
        bPosArrayList.clear();
        assert mc.player != null;
        ChunkRand chunkRand = new ChunkRand();
        meteordevelopment.meteorclient.utils.world.Dimension currentDim = PlayerUtils.getDimension();
        BPos playerPos = new BPos(mc.player.getBlockPos().getX(), mc.player.getBlockPos().getY(), mc.player.getBlockPos().getZ());
        long spSeed = mc.isInSingleplayer() ? mc.getServer().getWorld(mc.world.getRegistryKey()).getSeed() : Long.parseLong(seed.get());

        switch (currentDim){
            case Overworld: dimension = Dimension.OVERWORLD; break;
            case Nether:    dimension = Dimension.NETHER; break;
            case End:       dimension = Dimension.END; break;
        }

        if (currentDim == meteordevelopment.meteorclient.utils.world.Dimension.Overworld){
            switch (owStructure.get()){
                case buried_treasure:   structure = buried_treasure; break;
                case desert_temple:     structure = desert_temple; break;
                case igloo:             structure = igloo; break;
                case jungle_temple:     structure = jungle_temple; break;
                case mansion:           structure = mansion; break;
                case monument:          structure = monument; break;
                case outpost:           structure = outpost; break;
                case witch_hut:         structure = witch_hut; break;
                case village:           structure = village; break;
                case ruined_portal:     structure = ruined_portal_ow; break;
            }
            List<BPos> bPosList = StructureHelper.getClosest(structure, playerPos, spSeed, chunkRand, BiomeSource.of(dimension, MCVersion.v1_16, Long.parseLong(seed.get())), 0)
                    .sequential()
                    .limit(structureCount.get())
                    .collect(Collectors.toList());
            bPosArrayList.addAll(bPosList);
            foundStructure = true;
        }
        if (currentDim == meteordevelopment.meteorclient.utils.world.Dimension.Nether){
            switch (netherStructure.get()) {
                case bastion_remnant:   structure = bastion_remnant; break;
                case fortress:          structure = fortress; break;
                case ruined_portal:     structure = ruined_portal_nether; break;
            }
            List<BPos> bPosList = StructureHelper.getClosest(structure, playerPos, spSeed, chunkRand, BiomeSource.of(dimension, MCVersion.v1_16, Long.parseLong(seed.get())), 0)
                    .sequential()
                    .limit(structureCount.get())
                    .collect(Collectors.toList());
            bPosArrayList.addAll(bPosList);
            foundStructure = true;
        }
        if (currentDim == meteordevelopment.meteorclient.utils.world.Dimension.End){
            structure = end_city;
            List<BPos> bPosList = StructureHelper.getClosest(structure, playerPos, spSeed, chunkRand, BiomeSource.of(dimension, MCVersion.v1_16, Long.parseLong(seed.get())), 0)
                    .sequential()
                    .limit(structureCount.get())
                    .collect(Collectors.toList());
            bPosArrayList.addAll(bPosList);
            foundStructure = true;
        }
        if (!foundStructure) {
            ChatUtils.error("Selected structure can't generate in current dimension.");
            this.toggle();
        }
        if (foundStructure) {
            for (int i = 0; i < structureCount.get(); i++){
                ChatUtils.info("Found " + structure.getName() + " at " + bPosArrayList.get(i).getX() + " " + bPosArrayList.get(i).getZ());
                if (addWaypoint.get()){
                    waypoint = new Waypoint();
                    waypoint.name = structure.getName();
                    waypoint.x = bPosArrayList.get(i).getX();
                    waypoint.y = 64;
                    waypoint.z = bPosArrayList.get(i).getZ();
                    waypoint.maxVisibleDistance = Integer.MAX_VALUE;
                    waypoint.actualDimension = PlayerUtils.getDimension();
                    waypoint.scale = 2;

                    switch (PlayerUtils.getDimension()) {
                        case
                                Overworld: waypoint.overworld = true;
                            break;
                        case
                                Nether:    waypoint.nether = true;
                            break;
                        case
                                End:       waypoint.end = true;
                            break;
                    }
                    waypoint.color.set(Color.fromHsv(ThreadLocalRandom.current().nextDouble() * 360, 1, 1));
                    Waypoints.get().add(waypoint);
                }
            }
            this.toggle();
        }
    }
    public static class StructureHelper {

        public static Stream<BPos> getClosest(RegionStructure<?, ?> structure, BPos currentPos , long worldseed, ChunkRand chunkRand, BiomeSource source, int dimCoeff) {
            int chunkInRegion = structure.getSpacing();
            int regionSize=chunkInRegion*16;
            RPos centerRPos = currentPos.toRegionPos(regionSize);
            SpiralIterator spiral=new SpiralIterator(centerRPos,regionSize);

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(spiral.iterator(), Spliterator.ORDERED), false)
                    .map(rPos-> StructureHelper.getInRegion(structure,worldseed,chunkRand,rPos))
                    .filter(Objects::nonNull) // remove for methods like bastion that use a float and is not in each region
                    .filter(cPos -> StructureHelper.canSpawn(structure,cPos,source))
                    .map(cPos -> {
                        BPos dimPos = cPos.toBlockPos().add(9, 0, 9);
                        return new BPos(dimPos.getX() << dimCoeff, 0, dimPos.getZ() << dimCoeff);
                    });
        }

        public static CPos getInRegion(RegionStructure<?, ?> structure, long worldseed, ChunkRand chunkRand, RPos rPos) {
            return structure.getInRegion(worldseed, rPos.getX(), rPos.getZ(), chunkRand);
        }

        public static boolean canSpawn(RegionStructure<?, ?> structure, CPos cPos, BiomeSource source) {
            return structure.canSpawn(cPos.getX(), cPos.getZ(), source);
        }

        static class SpiralIterator implements Iterable<RPos> {
            private RPos currentPos;
            private final RPos lowerBound;
            private final RPos upperBound;
            private int currentLength = 1;
            private int currentLengthPos = 0;
            private DIRECTION currentDirection = DIRECTION.NORTH;

            public SpiralIterator(RPos currentRPos, RPos lowerBound, RPos upperBound) {
                this.currentPos = currentRPos;
                this.lowerBound = lowerBound;
                this.upperBound = upperBound;
            }

            public SpiralIterator(RPos currentRPos, int regionSize) {
                this(currentRPos,
                        new BPos(-30_000_000, 0, -30_000_000).toRegionPos(regionSize),
                        new BPos(30_000_000, 0, 30_000_000).toRegionPos(regionSize)
                );
            }

            public SpiralIterator(RPos currentRPos) {
                this(currentRPos,32*16);
            }

            @Override
            public Iterator<RPos> iterator() {
                return new Iterator<RPos>() {

                    @Override
                    public boolean hasNext() {
                        return currentPos.getZ() <= upperBound.getZ() && currentPos.getZ() >= lowerBound.getZ() && currentPos.getX() <= upperBound.getX() && currentPos.getX() >= lowerBound.getX();
                    }

                    @Override
                    public RPos next() {
                        RPos result = currentPos;
                        if (isAlmostOutside()) {
                            // if we are at one of the border we know that we already turned before coming here
                            // finish current loop and go full outside the range
                            for (; currentLengthPos < currentLength; currentLengthPos++) {
                                this.update();
                            }
                            int counter = 0;
                            boolean keepRunning = true;
                            while (!this.hasNext() && counter < 4 && keepRunning) {
                                // if we reach the max on that length
                                if (currentLengthPos == currentLength) {
                                    counter++;
                                    // if it is a direction on which we need to make an extra step
                                    if (currentDirection == DIRECTION.EAST || currentDirection == DIRECTION.WEST) {
                                        currentLength += 1;
                                    }
                                    // switch to next direction
                                    currentDirection = currentDirection.next();
                                    // reset counter for this direction
                                    currentLengthPos = 0;
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                                // skip that entire section since it is out for the previous direction
                                for (; currentLengthPos < currentLength; currentLengthPos++) {
                                    this.update();
                                    if (this.hasNext()) {
                                        keepRunning = false;
                                        currentLengthPos+=1;
                                        break;
                                    }
                                }
                            }
                            return result;
                        }
                        // update on that direction
                        this.update();
                        currentLengthPos += 1;

                        // if we reach the max on that length
                        if (currentLengthPos == currentLength) {
                            // if it is a direction on which we need to make an extra step
                            if (currentDirection == DIRECTION.EAST || currentDirection == DIRECTION.WEST) {
                                currentLength += 1;
                            }
                            // switch to next direction
                            currentDirection = currentDirection.next();
                            // reset counter for this direction
                            currentLengthPos = 0;
                        } else if (currentLengthPos > currentLength) {
                            throw new UnsupportedOperationException();
                        }
                        return result;
                    }

                    public void update() {
                        switch (currentDirection) {
                            case NORTH:
                                currentPos = new RPos(currentPos.getX(), currentPos.getZ() - 1, currentPos.getRegionSize());
                                break;
                            case EAST:
                                currentPos = new RPos(currentPos.getX() + 1, currentPos.getZ(), currentPos.getRegionSize());
                                break;
                            case SOUTH:
                                currentPos = new RPos(currentPos.getX(), currentPos.getZ() + 1, currentPos.getRegionSize());
                                break;
                            case WEST:
                                currentPos = new RPos(currentPos.getX() - 1, currentPos.getZ(), currentPos.getRegionSize());
                                break;
                        }
                    }

                    public void backtrack() {
                        switch (currentDirection) {
                            case NORTH:
                                currentPos = new RPos(currentPos.getX(), currentPos.getZ() + 1, currentPos.getRegionSize());
                                break;
                            case EAST:
                                currentPos = new RPos(currentPos.getX() - 1, currentPos.getZ(), currentPos.getRegionSize());
                                break;
                            case SOUTH:
                                currentPos = new RPos(currentPos.getX(), currentPos.getZ() - 1, currentPos.getRegionSize());
                                break;
                            case WEST:
                                currentPos = new RPos(currentPos.getX() + 1, currentPos.getZ(), currentPos.getRegionSize());
                                break;
                        }
                    }

                    public boolean isAlmostOutside() {
                        switch (currentDirection) {
                            case NORTH:
                                return currentPos.getZ() <= lowerBound.getZ();
                            case EAST:
                                return currentPos.getX() >= upperBound.getX();
                            case SOUTH:
                                return currentPos.getZ() >= upperBound.getZ();
                            case WEST:
                                return currentPos.getX() <= lowerBound.getX();
                        }
                        return false;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            public enum DIRECTION {
                NORTH,
                EAST,
                SOUTH,
                WEST,
                NONE;

                public DIRECTION next() {
                    switch (this) {
                        case NORTH:
                            return EAST;
                        case EAST:
                            return SOUTH;
                        case SOUTH:
                            return WEST;
                        case WEST:
                            return NORTH;
                    }
                    return NONE;
                }

                public DIRECTION opposite() {
                    switch (this) {
                        case NORTH:
                            return SOUTH;
                        case EAST:
                            return WEST;
                        case SOUTH:
                            return NORTH;
                        case WEST:
                            return EAST;
                    }
                    return NONE;
                }
            }
        }
    }
}
