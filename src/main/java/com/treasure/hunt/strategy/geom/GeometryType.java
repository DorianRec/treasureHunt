package com.treasure.hunt.strategy.geom;

import lombok.Getter;

/**
 * This is conventions for GeometryItems,
 * how to display them.
 *
 * @author jotoh
 */
public enum GeometryType {
    /**
     * Hint relevant
     */
    HINT_CENTER(false, "hint-center"),
    HINT_CIRCLE(true, "Circle hint"),

    /**
     * HalfPlaneHint relevant
     */
    HALF_PLANE(true, "the treasure is not here", true),
    HALF_PLANE_CURRENT_RED(false, "current half plane hint", true),
    HALF_PLANE_PREVIOUS_BROWN(true, "previous half plane hint", true),
    HALF_PLANE_BEFORE_PREVIOUS_LIGHT_BROWN(true, "the half plane hint received before the previous half plane hint", true),

    /**
     * treasure/no-treasure (areas) relevant
     */
    TREASURE(true, "treasure", true),
    TREASURE_FLAG(true, "treasure flag", true),
    NO_TREASURE(false, "no treasure", true),
    POSSIBLE_TREASURE(false, "possible treasure", true),
    HINT_ANGLE(true, "angle hint", true),

    /**
     * searcher movements relevant
     */
    WAY_POINT(true, "way point"),
    WAY_POINT_LINE(true, "way point line"),
    CURRENT_WAY_POINT(true, "Current way point", true),

    HELPER_LINE(true, "helper line"),

    BOUNDING_CIRCE(false, "bounding circle", true),
    OUTER_CIRCLE(false, "outer circle", true),
    WORST_CONSTANT(false, "Point with worst Constant", true),
    INNER_BUFFER(true, "inner buffer area", true),
    CENTROID(true, "Centroid of remaining possible Area", true),

    /**
     * StrategyFromPaper relevant
     */
    CURRENT_PHASE(false, "current phase", true),
    CURRENT_RECTANGLE(true, "current rectangle", true),
    PREVIOUS_RECTANGLE(true, "previous rectangle", true),
    CURRENT_POLYGON(true, "current polygon", true),
    L1_DOUBLE_APOS(true, "L1''", true),

    // XY Searcher
    MAX_X(true, "max x", true),
    MAX_Y(true, "max x", true),
    MIN_X(true, "max x", true),
    MIN_Y(true, "max x", true),

    STANDARD(true, ""),
    GRID(true, "Grid", false, false, false),
    HIGHLIGHTER(true, "Highlighter", true, false, false),
    POLYHEDRON(true, "Polyhedron", true);

    @Getter
    private final String displayName;
    @Getter
    private boolean enabled;
    /**
     * If a {@link GeometryItem} has override set, only the last {@link GeometryItem} of this GeometryType
     * will be displayed and all previous ones will be invisible.
     */
    @Getter
    private boolean override;
    @Getter
    private boolean multiStyle = false;
    @Getter
    private boolean selectable = true;

    GeometryType(boolean enabled, String displayName, boolean override) {
        this.displayName = displayName;
        this.enabled = enabled;
        this.override = override;
    }

    GeometryType(boolean enabled, String displayName, boolean override, boolean multiStyle, boolean selectable) {
        this(enabled, displayName, override);
        this.multiStyle = multiStyle;
        this.selectable = selectable;
    }

    GeometryType(boolean enabledByDefault, String displayName) {
        this(enabledByDefault, displayName, false);
    }
}
