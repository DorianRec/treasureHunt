package com.treasure.hunt.strategy.geom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * Color and shape style of a {@link GeometryItem}.
 *
 * @author jotoh
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
public class GeometryStyle {

    public static final GeometryStyle WAY_POINT = new GeometryStyle(true, new Color(0xFFFFFF), 1);
    public static final GeometryStyle TREASURE = new GeometryStyle(true, new Color(0xFFD700), 2);
    public static final GeometryStyle HINT_ANGLE = new GeometryStyle(true, new Color(0x575757));
    public static final GeometryStyle GRID = new GeometryStyle(true, false, new Color(0x555555), null, -1);
    public static final GeometryStyle CURRENT_WAY_POINT = new GeometryStyle(true, new Color(0x007A1D), 2);
    public static final GeometryStyle CURRENT_PHASE = new GeometryStyle(true, new Color(0x008504));
    public static final GeometryStyle CURRENT_RECTANGLE = new GeometryStyle(true, new Color(0x00e007), new Color(0x0800e007, true));
    public static final GeometryStyle PREVIOUS_RECTANGLE = new GeometryStyle(true, new Color(0xBB67aa57, true));
    public static final GeometryStyle CURRENT_POLYGON = new GeometryStyle(true, new Color(0x40E0D0), new Color(0x0840E0D0, true));
    public static final GeometryStyle HALF_PLANE_CURRENT_RED = new GeometryStyle(true, new Color(0x99ff0000, true), new Color(0x11ff0000, true));
    public static final GeometryStyle HALF_PLANE_PREVIOUS_BROWN = new GeometryStyle(true, new Color(0x99fc532d, true), new Color(0x09fc532d, true));
    public static final GeometryStyle HALF_PLANE_BEFORE_PREVIOUS_LIGHT_BROWN = new GeometryStyle(true, new Color(0x99ff9933, true), new Color(0x09ff9933, true));
    public static final GeometryStyle L1_DOUBLE_APOS = new GeometryStyle(true, Color.BLUE);
    public static final GeometryStyle HALF_PLANE = new GeometryStyle(true, new Color(0x777777), new Color(0x22777777, true));
    public static final GeometryStyle WAY_POINT_LINE = new GeometryStyle(true, new Color(0xFFFFFF));
    public static final GeometryStyle HIGHLIGHTER = new GeometryStyle(true, Color.GREEN, Integer.MAX_VALUE);
    public static final GeometryStyle HELPER_LINE = new GeometryStyle(true, Color.DARK_GRAY, -1);
    public static final GeometryStyle STANDARD = new GeometryStyle(true, Color.lightGray);
    public static final GeometryStyle HINT_CIRCLE = new GeometryStyle(true, Color.WHITE, new Color(1, 1, 1, 0.01f));
    public static final GeometryStyle MAX_X = new GeometryStyle(true, new Color(0x000000));
    public static final GeometryStyle MAX_Y = new GeometryStyle(true, new Color(0x000000));
    public static final GeometryStyle MIN_X = new GeometryStyle(true, new Color(0x000000));
    public static final GeometryStyle MIN_Y = new GeometryStyle(true, new Color(0x000000));

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
        try {
            return (GeometryStyle) GeometryStyle.class.getField(type.name()).get(null);
        } catch (IllegalAccessException e) {
            log.warn("Illegal access to default style " + type.name());
        } catch (NoSuchFieldException ignored) {
        }
        return STANDARD;
    }

    public Stroke getStroke() {
        return new BasicStroke(1);
    }

    protected GeometryStyle clone() {
        return new GeometryStyle(visible, filled, outlineColor, fillColor, zIndex);
    }
}
