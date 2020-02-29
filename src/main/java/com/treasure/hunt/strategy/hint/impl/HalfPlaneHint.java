package com.treasure.hunt.strategy.hint.impl;

import com.treasure.hunt.strategy.geom.GeometryItem;
import com.treasure.hunt.strategy.geom.GeometryType;
import lombok.Getter;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static com.treasure.hunt.strategy.hint.impl.HalfPlaneHint.Direction.down;
import static com.treasure.hunt.strategy.hint.impl.HalfPlaneHint.Direction.up;
import static com.treasure.hunt.utils.JTSUtils.GEOMETRY_FACTORY;

/**
 * @author bsen
 */

public class HalfPlaneHint extends AngleHint {
    static final double visual_extent = 1000;
    private LineString halfPlaneLine = null;
    @Getter
    /**
     *
     * when the line indicated by anglePointLeft and anglePointRight is not horizontal,
     * right and left indicate where the target is (right indicates the target is in positive x-Direction
     * in relationship to the line)
     * when the line is horizontal, up signals the target is in positive y-Direction in relationship
     * to the line (the up and down enumerators are only used when the line is horizontal)
     * left and down respectively
     */
    private Direction direction;

    @Getter
    private HalfPlaneHint lastHint; // if it is not null it also gets drawn by this HalfPlaneHint

    public HalfPlaneHint(Coordinate center, Coordinate right) {
        super(right, center, new Coordinate(2 * center.x - right.x, 2 * center.y - right.y));
        Direction dir = null;

        if (center.getY() == right.getY()) {
            if (center.getX() < right.getX()) {
                dir = up;
            }
            if (center.getX() > right.getX()) {
                dir = down;
            }
            if (center.getX() == right.getX()) {
                throw new IllegalArgumentException("anglePointLeft must not equal anglePointRight in the " +
                        "construction of a new HalfPlaneHint");
            }
        }
        if (center.getY() < right.getY()) {
            dir = Direction.left;
        }
        if (center.getY() > right.getY()) {
            dir = Direction.right;
        }
        direction = dir;
    }

    public HalfPlaneHint(Coordinate center, Coordinate right, HalfPlaneHint lastHint) {
        this(center, right);
        this.lastHint = lastHint;
    }

    /**
     * This constructor can be used when unsure whether pointOne or pointTwo is the right/left point of the
     * hint but the direction of the hint is known.
     *
     * @param pointOne
     * @param pointTwo
     * @param direction
     */
    public HalfPlaneHint(Coordinate pointOne, Coordinate pointTwo, Direction direction) {
        super(new Coordinate(), new Coordinate(), new Coordinate());

        Coordinate right = null;
        Coordinate center = null;
        switch (direction) {
            case up:
                if (pointOne.getX() < pointTwo.getX()) {
                    center = pointOne;
                    right = pointTwo;
                }
                if (pointOne.getX() > pointTwo.getX()) {
                    center = pointTwo;
                    right = pointOne;
                }
                if (pointOne.getX() == pointTwo.getX()) {
                    throw new IllegalArgumentException("If the direction is up, the x values of pointOne and pointTwo" +
                            "must not be identical");
                }
                break;
            case down:
                if (pointOne.getX() < pointTwo.getX()) {
                    center = pointTwo;
                    right = pointOne;
                }
                if (pointOne.getX() > pointTwo.getX()) {
                    center = pointOne;
                    right = pointTwo;
                }
                if (pointOne.getX() == pointTwo.getX()) {
                    throw new IllegalArgumentException("If the direction is down, the x values of pointOne and pointTwo" +
                            "must not be identical");
                }
                break;
            case left:
                if (pointOne.getY() < pointTwo.getY()) {
                    center = pointOne;
                    right = pointTwo;
                }
                if (pointOne.getY() > pointTwo.getY()) {
                    center = pointTwo;
                    right = pointOne;
                }
                if (pointOne.getY() == pointTwo.getY()) {
                    throw new IllegalArgumentException("If the direction is left, the y values of pointOne and " +
                            "pointTwo must not be identical.");
                }
                break;
            case right:
                if (pointOne.getY() < pointTwo.getY()) {
                    center = pointTwo;
                    right = pointOne;
                }
                if (pointOne.getY() > pointTwo.getY()) {
                    center = pointOne;
                    right = pointTwo;
                }
                if (pointOne.getY() == pointTwo.getY()) {
                    throw new IllegalArgumentException("If the direction is right, the y values of pointOne and " +
                            "pointTwo must not be identical");
                }
                break;
        }
        geometryAngle.setRight(right);
        geometryAngle.setCenter(center);
        geometryAngle.setLeft(new Coordinate(2 * center.x - right.x, 2 * center.y - right.y));
        this.direction = direction;
    }

    public Coordinate getCenter() {
        return geometryAngle.getCenter();
    }

    public Coordinate getRight() {
        return geometryAngle.getRight();
    }

    public Coordinate getLeft() {
        return geometryAngle.getLeft();
    }

    public LineString getHalfPlaneLineGeometry() {
        if (halfPlaneLine == null) {
            Vector2D l_to_r = new Vector2D(getCenter(), getRight());
            Vector2D r_to_l = new Vector2D(getRight(), getCenter());

            l_to_r = l_to_r.multiply(visual_extent / l_to_r.length());
            r_to_l = r_to_l.multiply(visual_extent / r_to_l.length());

            Coordinate extendedL = new Coordinate(
                    getRight().x + r_to_l.getX(),
                    getRight().y + r_to_l.getY()
            );
            Coordinate extendedR = new Coordinate(
                    getCenter().x + l_to_r.getX(),
                    getCenter().y + l_to_r.getY()
            );
            Coordinate[] line = new Coordinate[]{extendedL, extendedR};
            halfPlaneLine = GEOMETRY_FACTORY.createLineString(line);
        }
        return halfPlaneLine;
    }

    public LineSegment getHalfPlaneLine() {
        return new LineSegment(getCenter(), getRight());
    }

    public boolean inHalfPlane(Coordinate P) {
        double angleHintLine = new LineSegment(getCenter(), getRight()).angle();
        double angleCenterP = new LineSegment(getCenter(), P).angle();
        return Angle.normalizePositive((angleCenterP - angleHintLine)) <= Math.PI;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public List<GeometryItem<?>> getGeometryItems() {
        List<GeometryItem<?>> output = new ArrayList<>();
        output.add(new GeometryItem(getHalfPlaneLineGeometry(), GeometryType.HALF_PLANE_LINE));
        if (lastHint != null && lastHint.getHalfPlaneLineGeometry() != null) {
            output.add(new GeometryItem(lastHint.getHalfPlaneLineGeometry(), GeometryType.HALF_PLANE_LINE_BLUE));
        }
        return output;
    }

    public Coordinate getLowerHintPoint() {
        if (getCenter().getY() < getRight().getY()) {
            return getCenter();
        } else {
            return getRight();
        }
    }

    public Coordinate getUpperHintPoint() {
        if (getCenter().getY() < getRight().getY()) {
            return getRight();
        } else {
            return getCenter();
        }
    }

    public boolean pointsUpwards() {
        return (getDirection() == Direction.left && getLowerHintPoint().getX() < getUpperHintPoint().getX()) ||
                (getDirection() == Direction.right && getLowerHintPoint().getX() > getUpperHintPoint().getX());
    }

    public boolean pointsDownwards() {
        return (getDirection() == Direction.left && getLowerHintPoint().getX() > getUpperHintPoint().getX()) ||
                (getDirection() == Direction.right && getLowerHintPoint().getX() < getUpperHintPoint().getX());
    }

    public enum Direction {
        right, left, up, down
    }
}
