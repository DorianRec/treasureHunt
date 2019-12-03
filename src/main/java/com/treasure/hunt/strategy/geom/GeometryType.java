package com.treasure.hunt.strategy.geom;

import lombok.Getter;
import lombok.Setter;

public enum GeometryType {
    // hints
    FALSE_HINT(false, "False Hint"),
    HINT_CENTER(false, "hint-center"),
    HINT_RADIUS(false, "hint-center"),
    TRUE_HINT(false, "True Hint"),

    // treasure/no-treasure (areas)
    NO_TREASURE(false, "no treasure"),
    POSSIBLE_TREASURE(false, "possible treasure"),
    HINT_ANGLE(true, "angle hint"),

    // searcher movements
    SEARCHER_POSITION(true, "searcher position"),
    SEARCHER_MOVEMENT(true, "searcher movement"),

    // treasure location
    TREASURE(true, "no treasure"),

    // Obstacle add-on
    OBSTACLE(false, "no treasure"),
    WAY_POINT(false, "no treasure"),

    // StrategyFromPaper
    CURRENT_PHASE(false, "current phase"),
    CURRENT_RECTANGLE(true, "current rectangle"),
    RECTANGLE_SCAN(false, "rectangleScan"),

    STANDARD(true, "")

    // TODO add more..
    ;

    @Getter
    private final String displayName;
    @Getter
    @Setter
    private boolean enabled;
    @Getter
    @Setter
    private boolean override;

    GeometryType(String displayName, boolean enabled, boolean override) {
        this.displayName = displayName;
        this.enabled = enabled;
        this.override = override;
    }

    GeometryType(boolean enabledByDefault, String displayName) {
        this(displayName, enabledByDefault, false);
    }
}