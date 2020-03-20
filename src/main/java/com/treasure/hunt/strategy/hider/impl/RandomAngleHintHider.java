package com.treasure.hunt.strategy.hider.impl;

import com.treasure.hunt.jts.geom.GeometryAngle;
import com.treasure.hunt.service.preferences.PreferenceService;
import com.treasure.hunt.strategy.geom.StatusMessageItem;
import com.treasure.hunt.strategy.geom.StatusMessageType;
import com.treasure.hunt.strategy.hider.Hider;
import com.treasure.hunt.strategy.hint.impl.AngleHint;
import com.treasure.hunt.strategy.searcher.SearchPath;
import com.treasure.hunt.utils.JTSUtils;
import com.treasure.hunt.utils.Preference;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.math.Vector2D;

import static org.locationtech.jts.algorithm.Angle.interiorAngle;

/**
 * A type of {@link Hider}, generating randomly chosen {@link AngleHint}'s
 *
 * @author dorianreineccius
 */
@Preference(name = RandomAngleHintHider.TREASURE_DISTANCE, value = 100)
@Preference(name = RandomAngleHintHider.ANGLE_UPPER_BOUND, value = 2 * Math.PI)
@Preference(name = RandomAngleHintHider.ANGLE_LOWER_BOUND, value = Math.PI)
public class RandomAngleHintHider implements Hider<AngleHint> {
    public static final String TREASURE_DISTANCE = "TREASURE_DISTANCE";
    public static final String ANGLE_UPPER_BOUND = "ANGLE_UPPER_BOUND";
    public static final String ANGLE_LOWER_BOUND = "ANGLE_LOWER_BOUND";
    private Point treasurePosition;

    /**
     * @return {@link Point} containing treasure location of [0,100)x[0x100)
     */
    @Override
    public Point getTreasureLocation() {
        double distance = PreferenceService.getInstance().getPreference(TREASURE_DISTANCE, 100).doubleValue();
        Coordinate treasure = Vector2D.create(Math.random() * distance, 0).rotate(2 * Math.PI * Math.random()).translate(new Coordinate());
        treasurePosition = JTSUtils.GEOMETRY_FACTORY.createPoint(treasure);
        return treasurePosition;
    }

    @Override
    public void init(Point searcherStartPosition) {
    }

    @Override
    public AngleHint move(SearchPath searchPath) {
        Coordinate searcherPos = searchPath.getLastPoint().getCoordinate();
        double upperBound = PreferenceService.getInstance().getPreference(ANGLE_UPPER_BOUND, Math.PI * 2).doubleValue();
        double lowerBound = PreferenceService.getInstance().getPreference(ANGLE_LOWER_BOUND, 0).doubleValue();

        GeometryAngle angle = JTSUtils.validRandomAngle(searcherPos, treasurePosition.getCoordinate(), upperBound, lowerBound);
        double angleDegree = interiorAngle(angle.getRight(), angle.getCenter(), angle.getLeft());

        AngleHint angleHint = new AngleHint(
                angle
        );
        angleHint.getStatusMessageItemsToBeAdded().
                add(new StatusMessageItem(StatusMessageType.ANGLE_HINT_DEGREE, String.valueOf(angleDegree)));
        return angleHint;
    }
}
