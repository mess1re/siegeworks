package me.mss1r.siegeworks.client.guide;

import java.util.Arrays;
import java.util.List;

enum ManualEntry {
    INK(Chapter.CONSTRUCTION, "ink", "axiomata:inkwell", "axiomata:ink_block_ink_sac", "axiomata:ink_block_coals"),
    TOOLS(Chapter.CONSTRUCTION, "tools", "axiomata:drawing_table", "axiomata:construction_hammer"),
    DRAW(Chapter.CONSTRUCTION, "draw"),
    PARTS(Chapter.CONSTRUCTION, "parts", "siegeworks:wheel", "siegeworks:barrel"),
    BUILD(Chapter.CONSTRUCTION, "build"),
    PLACEMENT(Chapter.CONSTRUCTION, "placement"),

    CONTROLS(Chapter.SIEGE, "controls"),
    TOWER_CROSSBOW(Chapter.SIEGE, "tower_crossbow"),
    ARCBALLISTA(Chapter.SIEGE, "arcballista"),
    CULVERIN(Chapter.SIEGE, "culverin"),
    SERPENTINE(Chapter.SIEGE, "serpentine"),
    MONS_MEG(Chapter.SIEGE, "mons_meg"),
    ONAGER(Chapter.SIEGE, "onager"),
    TREBUCHET(Chapter.SIEGE, "trebuchet"),
    HWACHA(Chapter.SIEGE, "hwacha"),

    LADDER_CRAFT(Chapter.SIEGE, "ladder_craft", "siegeworks:siege_ladder_spawner"),
    LADDER(Chapter.SIEGE, "siege_ladder"),
    FORKED_POLE(Chapter.SIEGE, "forked_pole", "siegeworks:forked_pole"),
    RAM(Chapter.SIEGE, "battering_ram"),
    MANTLET(Chapter.SIEGE, "mantlet"),
    TOWER(Chapter.SIEGE, "siege_tower"),
    TOWER_CREW(Chapter.SIEGE, "tower_crew"),

    SUPPLIES(Chapter.AMMUNITION, "supplies", "siegeworks:black_powder", "siegeworks:ramrod"),
    CASTING(Chapter.AMMUNITION, "casting", "siegeworks:casting_table"),
    STONE_SHOT(Chapter.AMMUNITION, "stone_shot", "siegeworks:stone_cannon_ball", "siegeworks:giant_cannon_ball"),
    BOLTS(Chapter.AMMUNITION, "bolts", "siegeworks:tower_crossbow_bolt", "siegeworks:arcballista_bolt"),
    GRAPESHOT(Chapter.AMMUNITION, "grapeshot", "siegeworks:grapeshot"),
    ROCKETS(Chapter.AMMUNITION, "rockets", "siegeworks:so_singijeon", "siegeworks:jung_singijeon"),
    POT(Chapter.AMMUNITION, "pot", "siegeworks:clay_pot"),
    POT_FILLING(Chapter.AMMUNITION, "pot_filling"),
    BALLISTICS(Chapter.AMMUNITION, "ballistics", "siegeworks:fire_arrow"),

    FIREARM_CRAFT(Chapter.FIREARMS, "firearm_craft"),
    FIREARM_TOOLS(Chapter.FIREARMS, "firearm_tools", "siegeworks:firearm_lock", "siegeworks:firearm_ramrod"),
    FIREARM_LOAD(Chapter.FIREARMS, "firearm_load"),
    HAND_CANNON(Chapter.FIREARMS, "hand_cannon"),
    ARQUEBUS(Chapter.FIREARMS, "arquebus"),
    MATCH(Chapter.FIREARMS, "match"),

    TOWING(Chapter.SERVICE, "towing"),
    MAINTENANCE(Chapter.SERVICE, "maintenance"),
    WEATHER(Chapter.SERVICE, "weather"),
    CUSTOMIZATION(Chapter.SERVICE, "customization"),
    OWNERSHIP(Chapter.SERVICE, "ownership"),
    CAPTURE(Chapter.SERVICE, "capture"),
    RECRUITS(Chapter.SERVICE, "recruits"),
    SUPPLY(Chapter.SERVICE, "supply"),
    COMPAT(Chapter.SERVICE, "compat");

    final Chapter chapter;
    final String id;
    final List<String> recipes;

    ManualEntry(Chapter chapter, String id, String... recipes) {
        this.chapter = chapter;
        this.id = id;
        this.recipes = List.of(recipes);
    }

    String modelId() {
        return switch (this) {
            case CONTROLS, PLACEMENT -> "culverin";
            case TOWER_CROSSBOW, ARCBALLISTA, CULVERIN, SERPENTINE, MONS_MEG, ONAGER, TREBUCHET, HWACHA,
                    RAM, MANTLET, TOWER, LADDER -> id;
            case TOWER_CREW -> "siege_tower";
            default -> null;
        };
    }

    enum Chapter {
        CONSTRUCTION("construction"),
        SIEGE("siege"),
        AMMUNITION("ammunition"),
        FIREARMS("firearms"),
        SERVICE("service");

        final String id;

        Chapter(String id) {
            this.id = id;
        }

        List<ManualEntry> entries() {
            return Arrays.stream(ManualEntry.values()).filter(entry -> entry.chapter == this).toList();
        }
    }
}
