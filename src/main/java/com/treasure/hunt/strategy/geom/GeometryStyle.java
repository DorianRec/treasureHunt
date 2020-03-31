package com.treasure.hunt.strategy.geom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jotoh
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GeometryStyle {

    public static EnumMap<GeometryType, GeometryStyle> defaultsMap = new EnumMap<>(GeometryType.class);

    static {
        HashMap defaultsMapAdd = new HashMap(Map.of(
                GeometryType.WAY_POINT, new GeometryStyle(true, new Color(0xFFFFFF)),
                GeometryType.TREASURE, new GeometryStyle(true, new Color(0xFFD700)),
                GeometryType.HINT_ANGLE, new GeometryStyle(true, new Color(0x575757)),
                GeometryType.GRID, new GeometryStyle(true, false, new Color(0x555555), null, -1),
                GeometryType.CURRENT_PHASE, new GeometryStyle(true, Color.YELLOW),
                GeometryType.CURRENT_RECTANGLE, new GeometryStyle(true, Color.RED),
                GeometryType.CURRENT_WAY_POINT, new GeometryStyle(true, new Color(0x007A1D)),
                GeometryType.HALF_PLANE, new GeometryStyle(true, new Color(0x777777), new Color(0x22777777, true)),
                GeometryType.HALF_PLANE_LINE, new GeometryStyle(true, new Color(0x000000)),
                GeometryType.HALF_PLANE_LINE_BLUE, new GeometryStyle(true, Color.BLUE)
        ));
        defaultsMapAdd.put(GeometryType.CURRENT_POLYGON, new GeometryStyle(true, new Color(0x00ff00)));
        defaultsMap.putAll(defaultsMapAdd);
        defaultsMapAdd.put(GeometryType.HALF_PLANE_LINE_BROWN, new GeometryStyle(true, new Color(0x8B4513)));
        defaultsMap.putAll(defaultsMapAdd);
        defaultsMapAdd.put(GeometryType.WAY_POINT_LINE, new GeometryStyle(true, new Color(0xFFFFFF)));
        defaultsMap.putAll(defaultsMapAdd);
        defaultsMapAdd.put(GeometryType.HIGHLIGHTER, new GeometryStyle(true, Color.GREEN, Integer.MAX_VALUE));
        defaultsMap.putAll(defaultsMapAdd);
    }

    private boolean visible;

    private boolean filled;

    private Color outlineColor;

    private Color fillColor;

    private int zIndex = 0;

    public GeometryStyle(boolean visible, Color outlineColor) {
        this(visible, false, outlineColor, Color.black, 0);
    }

    public GeometryStyle(boolean visible, Color outlineColor, Color fillColor) {
        this(visible, true, outlineColor, fillColor, 0);
    }

    public GeometryStyle(boolean visible, Color outlineColor, int zIndex) {
        this.visible = visible;
        this.outlineColor = outlineColor;
        this.zIndex = zIndex;
    }

    public static GeometryStyle getDefaults(GeometryType type) {

        if (defaultsMap.containsKey(type)) {
            return defaultsMap.get(type).clone();
        }
        return new GeometryStyle(true, Color.lightGray);
    }

    public static void registerDefault(GeometryType type, GeometryStyle style) {
        defaultsMap.put(type, style);
    }

    public Stroke getStroke() {
        return new BasicStroke(1);
    }

    protected GeometryStyle clone() {
        return new GeometryStyle(visible, filled, outlineColor, fillColor, zIndex);
    }
}
