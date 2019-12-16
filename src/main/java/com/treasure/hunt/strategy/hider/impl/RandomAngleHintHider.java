package com.treasure.hunt.strategy.hider.impl;

import com.treasure.hunt.strategy.hider.Hider;
import com.treasure.hunt.strategy.hint.impl.AngleHint;
import com.treasure.hunt.strategy.searcher.Movement;
import com.treasure.hunt.utils.JTSUtils;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Point;

/**
 * This type of {@link Hider} returns a random {@link AngleHint},
 * which is correct and opens an angle of [0, PI).
 *
 * @author dorianreineccius
 */
public class RandomAngleHintHider implements Hider<AngleHint> {
    private Point treasurePos = JTSUtils.createPoint(Math.random() * 100, Math.random() * 100);

    /**
     * @param movement the {@link Movement}, the {@link com.treasure.hunt.strategy.searcher.Searcher} did last
     * @return A random but correct {@link AngleHint} opening an angle of [0, PI)
     */
    @Override
    public AngleHint move(Movement movement) {
        Point searcherPos = movement.getEndPoint();

        // generate angle
        double randomAngle = Math.random() * Math.PI; // in [0, PI)

        // generate the spinning of the angle
        double random = Math.random();

        double leftAngle = Angle.angle(searcherPos.getCoordinate(),
                treasurePos.getCoordinate()) + random * randomAngle;
        double leftX = searcherPos.getX() + (Math.cos(leftAngle) * 1);
        double leftY = searcherPos.getY() + (Math.sin(leftAngle) * 1);
        double rightAngle = Angle.angle(searcherPos.getCoordinate(),
                treasurePos.getCoordinate()) - (1 - random) * randomAngle;
        double rightX = searcherPos.getX() + (Math.cos(rightAngle) * 1);
        double rightY = searcherPos.getY() + (Math.sin(rightAngle) * 1);

        return new AngleHint(
                JTSUtils.createPoint(rightX, rightY),
                searcherPos,
                JTSUtils.createPoint(leftX, leftY)
        );
    }

    /**
     * @return {@link Point} containing treasure location of [0,100)x[0x100)
     */
    @Override
    public Point getTreasureLocation() {
        return treasurePos;
    }
}
