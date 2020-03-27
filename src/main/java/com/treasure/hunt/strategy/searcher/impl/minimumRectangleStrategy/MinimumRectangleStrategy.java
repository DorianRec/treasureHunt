package com.treasure.hunt.strategy.searcher.impl.minimumRectangleStrategy;

import com.treasure.hunt.strategy.geom.GeometryItem;
import com.treasure.hunt.strategy.geom.GeometryType;
import com.treasure.hunt.strategy.geom.StatusMessageItem;
import com.treasure.hunt.strategy.geom.StatusMessageType;
import com.treasure.hunt.strategy.hider.Hider;
import com.treasure.hunt.strategy.hint.impl.HalfPlaneHint;
import com.treasure.hunt.strategy.searcher.SearchPath;
import com.treasure.hunt.strategy.searcher.Searcher;
import com.treasure.hunt.strategy.searcher.impl.strategyFromPaper.GeometricUtils;
import com.treasure.hunt.strategy.searcher.impl.strategyFromPaper.LastHintBadSubroutine;
import com.treasure.hunt.strategy.searcher.impl.strategyFromPaper.RoutinesFromPaper;
import com.treasure.hunt.strategy.searcher.impl.strategyFromPaper.StrategyFromPaper;
import com.treasure.hunt.utils.JTSUtils;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

import static com.treasure.hunt.strategy.searcher.impl.minimumRectangleStrategy.ExcludedAreasUtils.intersectHints;
import static com.treasure.hunt.strategy.searcher.impl.minimumRectangleStrategy.ExcludedAreasUtils.visitedPolygon;
import static com.treasure.hunt.utils.JTSUtils.GEOMETRY_FACTORY;
import static com.treasure.hunt.utils.JTSUtils.lineWayIntersection;

/**
 * The strategy MinimumRectangleStrategy works similar to the strategy from the paper
 * "Deterministic Treasure Hunt in the Plane with Angular Hints" from Bouchard et al..
 * We will call the strategy from the paper S in the following text.
 * S gets improved by this strategy.
 * Like S, this strategy has phases 1,2,... in which it searches the treasure in rectangles
 * of the side length 2^i.
 * These rectangles are also centered in the start position of the searcher.
 * But in contrast to S, in this strategy the phase rectangles aren't oriented in order to be axis
 * parallel, but the rectangle is rotated in order to have one side that is parallel
 * to the first gotten hint.
 * By that, already half of the phase rectangle can be ignored.
 * Phase rectangles are indicated by yellow in the visualisation.
 * When the phase gets increased (and at the start of the strategy), the intersection I of all
 * halfplanes of all hints and the current phase rectangle is created.
 * The difference D of I and all already seen areas is created.
 * D is visualized in green.
 * Then the minimum rectangle which has sides parallel to the phase rectangle and where D is
 * inside, is formed and S is applied on it as long as the phase is not increased.
 * The area where the treasure is currently searched is visualized by red.
 * Then the procedure gets repeated with the new phase.
 * Also when applying RectangleScan, this strategy does reduce the rectangle to be  scanned,
 * by ignoring some areas it has already seen and areas which can be ignored due to hints.
 * <p>
 * An annotation to the implementation:
 * The implementation works on two different coordinate systems:
 * 1. the "external" coordinate system: There all coordinates are represented as in the world around this strategy,
 * i.e. the when move() returns, this gets interpreted by the game engine so all coordinates used in the return value
 * are by definition in external coordinates.
 * Also the strategy gets hints which are represented in external coordinates
 * 2. the "internal" coordinate system: Since the strategy from the paper can only work on axis-parallel phase rectangles,
 * the real phase rectangle gets rotated around the center of it in order to be easily processed by the strategy
 * from the paper. For simplification the rectangle also gets shifted so that the center matches (0,0).
 * By rotating and shifting the external coordinates in that manner, the internal coordinate system is created.
 * The two coordinate systems can be translated in one another by using the TransformForAxisParallelism instance
 * transformer.
 *
 * @author Rank
 */

public class MinimumRectangleStrategy extends StrategyFromPaper implements Searcher<HalfPlaneHint> {
    Point realSearcherStartPosition;
    private boolean firstMoveWithHint = true;
    private TransformForAxisParallelism transformer;
    /**
     * received after the last update of the phase's rectangle
     */
    private List<HalfPlaneHint> obtainedHints; // stored in internal coordinates
    /**
     * This points represent the polygon where the treasure must lie in if it is in the current search rectangle,
     * according to all obtained hints.
     * If this List is empty, the treasure is not in the current search rectangle.
     */
    private Geometry currentMultiPolygon; // stored in internal coordinates
    /**
     * This are not real obtained hints.
     * This hints are just the borders of the current phase rectangle interpreted as hints.
     * All hintlines go from one corner of the current phase rectangle to another and show in a direction that the
     * hole phase rectangle lies in the treasure area.
     */
    private ArrayList<HalfPlaneHint> phaseHints; // stored in internal coordinates
    private Polygon hintPolygon;
    /**
     * The polygon the player already has seen because he was there.
     */
    private Polygon visitedPolygon; // stored in internal coordinates
    /**
     * The lastLocation of the previous move
     */
    private Point lastLocation; // stored in internal coordinates
    private HintQuality currentHintQuality = HintQuality.none;
    private LastHintBadSubroutine lastHintBadSubroutine = new LastHintBadSubroutine(this);

    private ArrayList<StatusMessageItem> statusMessagesToBeRemovedNextMoveInMinimumRectangleStrategy = new ArrayList<>();

    /**
     * @param searcherStartPosition the {@link Searcher} starting position,
     *                              he will initialized on.
     */
    @Override
    public void init(Point searcherStartPosition) {
        super.init(JTSUtils.createPoint(0, 0));
        this.realSearcherStartPosition = searcherStartPosition;
        phase = 1;
        phaseHints = new ArrayList<>(4);
        {
            phaseHints.add(null);
            phaseHints.add(null);
            phaseHints.add(null);
            phaseHints.add(null);
        }
        obtainedHints = new ArrayList<>();
        currentMultiPolygon = JTSUtils.GEOMETRY_FACTORY.createPolygon();
        visitedPolygon = JTSUtils.GEOMETRY_FACTORY.createPolygon();
    }

    /**
     * Use this to perform a initial move, without a hint given.
     * In this case, the searcher does nothing and receives a hint.
     *
     * @return {@link SearchPath} the {@link SearchPath} the searcher did
     */
    @Override
    public SearchPath move() {
        SearchPath move = new SearchPath();
        move.addPoint(realSearcherStartPosition);
        StatusMessageItem beginningStatusMessage = new StatusMessageItem(StatusMessageType.EXPLANATION_STRATEGY,
                "The strategy MinimumRectangleStrategy works similar to the strategy from the paper \n" +
                        "\"Deterministic Treasure Hunt in the Plane with Angular Hints\" from Sébastien Bouchard et " +
                        "al..\nWe will call the strategy from the paper S in the following text.\n" +
                        "S gets improved by this strategy.\n" +
                        "Like S, this strategy has phases 1,2,... in which it searches the treasure in rectangles \n" +
                        "of the side length 2^i.\n" +
                        "These rectangles are also centered in the start position of the searcher.\n" +
                        "But in contrast to S, in this strategy the phase rectangles aren't oriented in order to be axis\n" +
                        "parallel, but the rectangle is rotated in order to have one side that is parallel \n" +
                        "to the first gotten hint.\n" +
                        "By that, already half of the phase rectangle can be ignored.\n" +
                        "Phase rectangles are indicated by yellow in the visualisation.\n" +
                        "When the phase gets increased (and at the start of the strategy), the intersection I of all " +
                        "\nhalfplanes of all hints and the current phase rectangle is created.\n" +
                        "The difference D of I and all already seen areas is created.\n" +
                        "D is visualized in green.\n" +
                        "Then the minimum rectangle which has sides parallel to the phase rectangle and where D is\n" +
                        "inside, is formed and S is applied on it as long as the phase is not increased.\n" +
                        "The area where the treasure is currently searched is visualized by red.\n" +
                        "Then the procedure gets repeated with the new phase.\n\n" +
                        "Also when applying RectangleScan, this strategy does reduce the rectangle to be  scanned,\n" +
                        "by ignoring some areas it has already seen and areas which can be ignored due to hints."
        );
        move.getStatusMessageItemsToBeAdded().add(beginningStatusMessage);
        statusMessagesToBeRemovedNextMoveInMinimumRectangleStrategy.add(beginningStatusMessage);
        StatusMessageItem visualisationMessage = new StatusMessageItem(StatusMessageType.EXPLANATION_VISUALISATION_SEARCHER,
                "The previous hint is visualized by a red area which covers the area the treasure is not in.\n" +
                        "The hint before the previous hint is visualized by a orange area which covers the area the treasure is not in.\n" +
                        "\n" +
                        "The phase rectangle is visualized in a dark green color.\n" +
                        "The current rectangle is visualized in a lighter green color.\n" +
                        "\n" +
                        "The polygon (or multiple polygons) where the treasure can lie in when it is in the current phase is drawn at the\n" +
                        "beginning of each phase and is of turquoise color."
        );
        move.getStatusMessageItemsToBeAdded().add(visualisationMessage);
        statusMessagesToBeRemovedNextMoveInMinimumRectangleStrategy.add(visualisationMessage);
        return move;
    }

    /**
     * @param hint the hint, the {@link Hider} gave last.
     * @return {@link SearchPath} the {@link SearchPath}, this searcher chose.
     */
    @Override
    public SearchPath move(HalfPlaneHint hint) {
        if (firstMoveWithHint) {
            firstMoveWithHint = false;
            transformer = new TransformForAxisParallelism(hint, realSearcherStartPosition);
            lastLocation = JTSUtils.createPoint(0, 0); // the point the searcher moved to in the previous move
            visitedPolygon.union(visitedPolygon(lastLocation, new SearchPath()));
        }
        previousHintQuality = currentHintQuality;
        HalfPlaneHint hintInInternal = transformer.toInternal(hint);
        previousHint = currentHint;
        currentHint = hintInInternal;
        obtainedHints.add(hintInInternal);
        SearchPath move = new SearchPath();

        if (previousHintQuality == HintQuality.bad) {
            currentHintQuality = HintQuality.none;
            move = lastHintBadSubroutine.lastHintBadSubroutine(currentHint, previousHint,
                    move);
            updateVisitedPolygon(move);
            reducePolygons();
            if (currentMultiPolygon != null && currentMultiPolygon.getArea() != 0) {// otherwise, the phase is increased
                return returnHandling(GeometricUtils.moveToCenterOfRectangle(searchAreaCornerA, searchAreaCornerB,
                        searchAreaCornerC, searchAreaCornerD, move));
            }
        } else {
            if (!hintIsGood(currentHint) && !rectangleNotLargeEnough()) {
                currentHintQuality = HintQuality.bad;
                Point destination = GEOMETRY_FACTORY.createPoint(GeometricUtils.twoStepsOrthogonal(currentHint,
                        lastLocation));
                move.addPoint(destination);
                return returnHandling(move);
            }
            reducePolygons();
        }

        if (rectangleNotLargeEnough() || currentMultiPolygon == null || currentMultiPolygon.getArea() == 0) {
            currentHintQuality = HintQuality.none;
            return returnHandling(setNewPhaseAndMove(move));
        } else {
            return returnHandling(goodHintSubroutine());
        }
    }

    void reducePolygons() {
        if (hintPolygon != null) {
            hintPolygon = ExcludedAreasUtils.reduceConvexPolygon(hintPolygon, currentHint);
            if (hintPolygon == null) {
                currentMultiPolygon = null;
            } else {
                currentMultiPolygon = hintPolygon.difference(visitedPolygon);
                if (currentMultiPolygon.getArea() != 0) {
                    setABCDinStrategy();
                }
            }
        }
    }

    SearchPath goodHintSubroutine() {
        SearchPath move = new SearchPath();
        currentHintQuality = HintQuality.good;
        GeometricUtils.moveToCenterOfRectangle(searchAreaCornerA, searchAreaCornerB, searchAreaCornerC,
                searchAreaCornerD, move);
        return move;
    }

    SearchPath setNewPhaseAndMove(SearchPath move) {
        scanCurrentRectangle(move, currentHint);
        Geometry newMultiPolygon = null;
        do {
            phase++;
            updatePhaseHints();
            hintPolygon = intersectHints(obtainedHints, phaseHints);
            if (hintPolygon != null) {
                newMultiPolygon = hintPolygon.difference(visitedPolygon);
            }
        }
        while (newMultiPolygon == null || newMultiPolygon.getArea() == 0);
        currentMultiPolygon = newMultiPolygon;

        setABCDinStrategy();
        GeometricUtils.moveToCenterOfRectangle(searchAreaCornerA, searchAreaCornerB, searchAreaCornerC,
                searchAreaCornerD, move);
        return move;
    }

    boolean hintIsGood(HalfPlaneHint hintInInternal) {
        LineSegment hintLine = hintInInternal.getHalfPlaneLine();
        LineSegment AB = new LineSegment(searchAreaCornerA.getCoordinate(), searchAreaCornerB.getCoordinate());
        LineSegment BC = new LineSegment(searchAreaCornerB.getCoordinate(), searchAreaCornerC.getCoordinate());
        LineSegment CD = new LineSegment(searchAreaCornerC.getCoordinate(), searchAreaCornerD.getCoordinate());
        LineSegment AD = new LineSegment(searchAreaCornerA.getCoordinate(), searchAreaCornerD.getCoordinate());
        Coordinate intersectionADHint = lineWayIntersection(hintLine, AD);
        Coordinate intersectionBCHint = lineWayIntersection(hintLine, BC);
        Coordinate intersectionABHint = lineWayIntersection(hintLine, AB);
        Coordinate intersectionCDHint = lineWayIntersection(hintLine, CD);
        if ((intersectionABHint == null || intersectionCDHint == null ||
                ((intersectionABHint.distance(searchAreaCornerA.getCoordinate()) < 1
                        || intersectionABHint.distance(searchAreaCornerB.getCoordinate()) < 1
                        || intersectionCDHint.distance(searchAreaCornerC.getCoordinate()) < 1
                        || intersectionCDHint.distance(searchAreaCornerD.getCoordinate()) < 1))) &&
                (intersectionADHint == null || intersectionBCHint == null ||
                        ((intersectionADHint.distance(searchAreaCornerA.getCoordinate()) < 1
                                || intersectionADHint.distance(searchAreaCornerD.getCoordinate()) < 1
                                || intersectionBCHint.distance(searchAreaCornerB.getCoordinate()) < 1
                                || intersectionBCHint.distance(searchAreaCornerC.getCoordinate()) < 1))
                )
        ) {
            return false;
        }
        return true;
    }


    void updateVisitedPolygon(SearchPath move) {// lastLocation has to be set right
        visitedPolygon = (Polygon) visitedPolygon.union(visitedPolygon(lastLocation, move));
    }

    /**
     * This method has to be called directly before move(HalfPlaneHint) returns
     * In this method, several things are accomplished:
     * 1. the internal fields are updated (visitedPolygon, lastLocation)
     * 2. the move is transformed in the external coordinates
     * 3. the state of the current rectangle is added to the move
     * 4. the status messages which have to be removed, get removed
     *
     * @param move the move which is done this draw in internal coordinates
     * @return the transformed move with added state and removed status messages
     */
    protected SearchPath returnHandling(SearchPath move) {
        // update the visited polygon and the last location
        updateVisitedPolygon(move);
        lastLocation = move.getLastPoint();

        // the move is transformed in external coordinates
        move = transformer.toExternal(move);

        // add state of the current rectangle to move
        if (transformer == null) {
            return super.addState(move);
        }
        // add polygon
        if (currentMultiPolygon != null) {
            move.addAdditionalItem(new GeometryItem<>(transformer.toExternal(currentMultiPolygon),
                    GeometryType.CURRENT_POLYGON));
        }

        //add current and previous hint
        if (currentHint != null) {
            move.addAdditionalItem(new GeometryItem<>(
                    transformer.toExternal(currentHint).getHalfPlaneTheTreasureIsNotIn(),
                    GeometryType.HALF_PLANE_PREVIOUS_LIGHT_RED));
        }
        if (previousHint != null) {
            move.addAdditionalItem(new GeometryItem<>(
                    transformer.toExternal(previousHint).getHalfPlaneTheTreasureIsNotIn(),
                    GeometryType.HALF_PLANE_BEFORE_PREVIOUS_ORANGE));
        }

        // remove status messages
        move.getStatusMessageItemsToBeRemoved().addAll(statusMessagesToBeRemovedNextMoveInMinimumRectangleStrategy);
        // add hint-qualities to the move
        StatusMessageItem lastHintQualityStatus;
        switch (previousHintQuality) {
            case bad:
                lastHintQualityStatus = new StatusMessageItem(StatusMessageType.BEFORE_PREVIOUS_QUALITY, "bad");
                break;
            case good:
                lastHintQualityStatus = new StatusMessageItem(StatusMessageType.BEFORE_PREVIOUS_QUALITY, "good");
                break;
            case none:
                lastHintQualityStatus = new StatusMessageItem(StatusMessageType.BEFORE_PREVIOUS_QUALITY, "none");
                break;
            default:
                throw new AssertionError("The hint before the previous hint has no quality value");
        }
        move.getStatusMessageItemsToBeAdded().add(lastHintQualityStatus);

        StatusMessageItem currentHintQualityStatus;
        switch (previousHintQuality) {
            case bad:
                currentHintQualityStatus = new StatusMessageItem(StatusMessageType.PREVIOUS_HINT_QUALITY, "bad");
                break;
            case good:
                currentHintQualityStatus = new StatusMessageItem(StatusMessageType.PREVIOUS_HINT_QUALITY, "good");
                break;
            case none:
                currentHintQualityStatus = new StatusMessageItem(StatusMessageType.PREVIOUS_HINT_QUALITY, "none");
                break;
            default:
                throw new AssertionError("The hint before the previous hint has no quality value");
        }
        move.getStatusMessageItemsToBeAdded().add(currentHintQualityStatus);

        // add search rectangle and phase rectangle
        return super.addState(move, transformer.toExternal(searchRectangle()),
                transformer.toExternal(phaseRectangle(phase)));
    }

    @Override
    protected SearchPath specificRectangleScan(Coordinate rectangleCorner1, Coordinate rectangleCorner2,
                                               Coordinate rectangleCorner3, Coordinate rectangleCorner4, SearchPath move) {
        return rectangleScanMinimal(rectangleCorner1, rectangleCorner2, rectangleCorner3, rectangleCorner4,
                move);
    }

    private SearchPath rectangleScanMinimal(Coordinate rectangleCorner1, Coordinate rectangleCorner2,
                                            Coordinate rectangleCorner3, Coordinate rectangleCorner4, SearchPath move) {
        TransformForAxisParallelism transformerForRectangleAxisParallelism =
                new TransformForAxisParallelism(new LineSegment(rectangleCorner1, rectangleCorner2));
        ArrayList<HalfPlaneHint> rectangleToScanHints = new ArrayList<>(4);
        rectangleToScanHints.add(new HalfPlaneHint(rectangleCorner2, rectangleCorner1));
        rectangleToScanHints.add(new HalfPlaneHint(rectangleCorner3, rectangleCorner2));
        rectangleToScanHints.add(new HalfPlaneHint(rectangleCorner4, rectangleCorner3));
        rectangleToScanHints.add(new HalfPlaneHint(rectangleCorner1, rectangleCorner4));
        Polygon newPolygonToScan = intersectHints(obtainedHints, rectangleToScanHints);
        if (newPolygonToScan == null || newPolygonToScan.getArea() == 0) {
            return move;
        }
        Polygon newPolygonToScanTransformed = transformerForRectangleAxisParallelism.toInternal(newPolygonToScan);
        visitedPolygon = (Polygon) visitedPolygon.union(visitedPolygon(lastLocation, move));
        Geometry newAreaToScanTransformed;
        try {
            newAreaToScanTransformed = newPolygonToScanTransformed.difference(visitedPolygon);
        } catch (TopologyException e) {
            newAreaToScanTransformed = newPolygonToScanTransformed;
        }
        if (newAreaToScanTransformed.getArea() == 0) {
            return move;
        }

        Coordinate[] envelopeToScanTransformedPoints = newAreaToScanTransformed.getEnvelope().getCoordinates();

        RoutinesFromPaper.rectangleScan(
                transformerForRectangleAxisParallelism.toExternal(envelopeToScanTransformedPoints[0]),
                transformerForRectangleAxisParallelism.toExternal(envelopeToScanTransformedPoints[1]),
                transformerForRectangleAxisParallelism.toExternal(envelopeToScanTransformedPoints[2]),
                transformerForRectangleAxisParallelism.toExternal(envelopeToScanTransformedPoints[3]), move
        );
        return move;
    }

    private Coordinate[] searchRectangle() {
        return new Coordinate[]{searchAreaCornerA.getCoordinate(), searchAreaCornerB.getCoordinate(),
                searchAreaCornerC.getCoordinate(), searchAreaCornerD.getCoordinate()};
    }

    private void updatePhaseHints() {
        Coordinate[] phaseRectangle = phaseRectangle(phase);
        phaseHints = new ArrayList<>(4);
        phaseHints.add(new HalfPlaneHint(phaseRectangle[1], phaseRectangle[0]));
        phaseHints.add(new HalfPlaneHint(phaseRectangle[2], phaseRectangle[1]));
        phaseHints.add(new HalfPlaneHint(phaseRectangle[3], phaseRectangle[2]));
        phaseHints.add(new HalfPlaneHint(phaseRectangle[0], phaseRectangle[3]));
    }

    private void scanCurrentRectangle(SearchPath move, HalfPlaneHint hint) {
        rectangleScanMinimal(searchAreaCornerA.getCoordinate(), searchAreaCornerB.getCoordinate(),
                searchAreaCornerC.getCoordinate(), searchAreaCornerD.getCoordinate(), move);
    }

    private void setABCDinStrategy() {
        Coordinate[] coordinatesABCD = currentMultiPolygon.getEnvelope().getCoordinates();
        searchAreaCornerA = JTSUtils.GEOMETRY_FACTORY.createPoint(coordinatesABCD[1]);
        searchAreaCornerB = JTSUtils.GEOMETRY_FACTORY.createPoint(coordinatesABCD[2]);
        searchAreaCornerC = JTSUtils.GEOMETRY_FACTORY.createPoint(coordinatesABCD[3]);
        searchAreaCornerD = JTSUtils.GEOMETRY_FACTORY.createPoint(coordinatesABCD[0]);
        previousHintQuality = StrategyFromPaper.HintQuality.none;
    }
}

