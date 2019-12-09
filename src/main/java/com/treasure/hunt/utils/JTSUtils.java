package com.treasure.hunt.utils;

import com.treasure.hunt.strategy.hint.impl.AngleHint;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.*;

import static org.locationtech.jts.algorithm.Angle.angleBetweenOriented;

public class JTSUtils {
    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * @param x coordinate.
     * @param y coordinate.
     * @return a {@link Point} defined by (x,y)
     */
    public static Point createPoint(double x, double y) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
    }

    /**
     * @param A a point
     * @param B a point
     * @return a {@link LineString} containing only {@code A} and {@code B}
     */
    public static LineString createLineString(Point A, Point B) {
        Coordinate[] coords = {A.getCoordinate(), B.getCoordinate()};
        return GEOMETRY_FACTORY.createLineString(coords);
    }

    /**
     * Tests whether the line line intersects with the linesegment segment and returns the intersecting Coordinate
     * (if one exists).
     *
     * @param line    line with infinite extend
     * @param segment part of a line
     * @return an intersection {@link Coordinate} of the {@link LineSegment} objects {@code line} and {@code lineSegment}
     */
    public static Coordinate lineWayIntersection(LineSegment line, LineSegment segment) {
        Coordinate intersection = line.lineIntersection(segment);
        if (intersection == null)
            return null;
        double distance = GEOMETRY_FACTORY.getPrecisionModel().makePrecise(segment.distance(intersection));
        if (distance != 0)
            return null;
        return intersection;
    }

    /**
     * @param angleHint where we want the middle point to go, from.
     * @return {@link Point} going through the middle of the {@link AngleHint}
     */
    public static Coordinate middleOfAngleHint(AngleHint angleHint) {
        double betweenAngle = angleBetweenOriented(
                angleHint.getAnglePointRight().getCoordinate(),
                angleHint.getCenter().getCoordinate(),
                angleHint.getAnglePointLeft().getCoordinate()
        );

        double rightAngle = Angle.angle(angleHint.getCenter().getCoordinate(),
                angleHint.getAnglePointRight().getCoordinate());
        double resultAngle = Angle.normalizePositive(rightAngle + betweenAngle / 2);
        if (betweenAngle < 0) {
            resultAngle += Math.PI;
        }
        double x = angleHint.getCenter().getX() + (Math.cos(resultAngle));
        double y = angleHint.getCenter().getY() + (Math.sin(resultAngle));
        return new Coordinate(x, y);
    }
}
