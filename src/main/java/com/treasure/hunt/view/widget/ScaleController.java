package com.treasure.hunt.view.widget;

import com.treasure.hunt.game.GameManager;
import com.treasure.hunt.game.Turn;
import com.treasure.hunt.jts.awt.PointTransformation;
import com.treasure.hunt.view.CanvasController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.math.Vector2D;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

@Slf4j
public class ScaleController {
    final DecimalFormat percentageFormatter = new DecimalFormat("##.##%");
    public Slider slider;
    public TextField textField;
    public VBox wrapper;
    private static final double SCALE_FACTOR = 1;
    public Canvas navigatorCanvas;

    private CanvasController canvasController;
    private Rectangle symbol = new Rectangle(0, 0, 0, 0);

    private Point treasurePoint = null;

    /**
     * Behaviour of user entering a scale.
     */
    public void onEnter() {
        double cleanScale;

        try {
            cleanScale = percentageFormatter.parse(textField.getText()).doubleValue();
        } catch (ParseException e) {
            try {
                cleanScale = Double.parseDouble(textField.getText().replace(",", "."));
            } catch (NumberFormatException e1) {
                cleanScale = canvasController.getTransformation().getScaleProperty().get();
            }
        }

        canvasController.getTransformation().setScale(cleanScale);
        wrapper.requestFocus();
        textField.setText(percentageFormatter.format(canvasController.getTransformation().getScaleProperty().get()));
    }

    /**
     * Bind the transformation properties to the slider and text field.
     *
     * @param canvasController controller holding the {@link PointTransformation}
     */
    public void init(ObjectProperty<GameManager> gameManagerProperty, CanvasController canvasController) {
        this.canvasController = canvasController;

        final ChangeListener<GameManager> gameManagerChangeListener = (observable, oldValue, newValue) -> bindGameManager(newValue);
        gameManagerProperty.addListener(gameManagerChangeListener);
        gameManagerChangeListener.changed(null, null, gameManagerProperty.get());

        if (gameManagerProperty.isNotNull().get()) {
            bindGameManager(gameManagerProperty.get());
        }

        navigatorCanvasBindings();

        slider.setMax(PointTransformation.MAX_SCALE);
        slider.setMin(PointTransformation.MIN_SCALE);

        final PointTransformation transformer = canvasController.getTransformation();

        transformer
                .getScaleProperty()
                .addListener((observable, oldValue, newValue) ->
                        textField.setText(percentageFormatter.format(newValue)
                        ));

        slider.valueProperty().bindBidirectional(transformer.getScaleProperty());

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!slider.isPressed() || oldValue.equals(0)) {
                return;
            }
            final Canvas canvas = canvasController.getCanvas();
            final Vector2D center = Vector2D.create(canvas.getWidth(), canvas.getHeight()).divide(2);

            transformer.scaleOffset(((double) newValue) / ((double) oldValue), center);
        });
    }

    private void bindGameManager(final GameManager gameManager) {
        final ChangeListener<Number> viewListener = (observable, oldIndex, newViewIndex) -> {
            final List<Turn> visibleTurns = gameManager.getVisibleTurns();
            if (visibleTurns.size() < 1) {
                treasurePoint = null;
            }

            treasurePoint = visibleTurns.get(newViewIndex.intValue()).getTreasureLocation();
            drawNavigatorCanvas();
        };
        gameManager.getViewIndex().addListener(viewListener);
        viewListener.changed(null, null, 0);
    }

    private void navigatorCanvasBindings() {
        final Canvas canvas = canvasController.getCanvas();
        final PointTransformation transformation = canvasController.getTransformation();
        navigatorCanvas.widthProperty().bind(wrapper.widthProperty().subtract(20));
        navigatorCanvas.heightProperty().bind(Bindings.min(200, navigatorCanvas.widthProperty()));

        symbol.widthProperty().bind(canvas.widthProperty()
                .divide(transformation.getScaleProperty())
                .divide(PointTransformation.INITIAL_SCALE)
                .multiply(SCALE_FACTOR)
        );
        symbol.heightProperty().bind(canvas.heightProperty()
                .divide(transformation.getScaleProperty())
                .divide(PointTransformation.INITIAL_SCALE)
                .multiply(SCALE_FACTOR)
        );
        symbol.xProperty().bind(Bindings.createDoubleBinding(
                () -> -transformation.getOffset().getX(),
                transformation.getOffsetProperty()
                )
                        .divide(transformation.getScaleProperty())
                        .divide(PointTransformation.INITIAL_SCALE)
                        .multiply(SCALE_FACTOR)
                        .add(navigatorCanvas.widthProperty().divide(2))
        );
        symbol.yProperty().bind(Bindings.createDoubleBinding(
                () -> -transformation.getOffset().getY(),
                transformation.getOffsetProperty()
                )
                        .divide(transformation.getScaleProperty())
                        .divide(PointTransformation.INITIAL_SCALE)
                        .multiply(SCALE_FACTOR)
                        .add(navigatorCanvas.heightProperty().divide(2))
        );
        transformation.getOffsetProperty().addListener(observable -> drawNavigatorCanvas());
        canvas.widthProperty().addListener(observable -> drawNavigatorCanvas());
        transformation.getScaleProperty().addListener(observable -> drawNavigatorCanvas());
        wrapper.widthProperty().addListener(observable -> drawNavigatorCanvas());
        navigatorCanvas.setOnMouseClicked(event -> setOffset(event.getX(), event.getY()));
        navigatorCanvas.setOnMouseDragged(event -> setOffset(event.getX(), event.getY()));
        navigatorCanvas.setOnScroll(event -> {
            final double scaleFactor = Math.exp(event.getDeltaY() * 1e-2);
            transformation.scaleRelative(scaleFactor, Vector2D.create(canvas.getWidth(), canvas.getHeight()).divide(2));
        });
        drawNavigatorCanvas();
    }

    private Point2D transform(Point point) {
        final double x = navigatorCanvas.getWidth() / 2 + point.getX() * SCALE_FACTOR;
        final double y = navigatorCanvas.getHeight() / 2 - point.getY() * SCALE_FACTOR;
        return new Point2D(x, y);
    }

    private void setOffset(double x, double y) {
        final Vector2D middleOffset = Vector2D.create(navigatorCanvas.getWidth(), navigatorCanvas.getHeight()).divide(2);
        final Vector2D edge = Vector2D.create(symbol.getWidth(), symbol.getHeight()).divide(2);
        final Vector2D subtract = middleOffset.subtract(Vector2D.create(x, y));
        final double scale = canvasController.getTransformation().getScale();
        canvasController.getTransformation().setOffset(
                subtract.add(edge)
                        .divide(SCALE_FACTOR)
                        .multiply(scale)
        );
    }

    private void drawNavigatorCanvas() {
        final GraphicsContext context = navigatorCanvas.getGraphicsContext2D();
        double width = navigatorCanvas.getWidth();
        double height = navigatorCanvas.getHeight();

        context.clearRect(0, 0, width, height);

        context.setLineWidth(1);
        context.setStroke(Color.grayRgb(128));
        context.strokeLine(0, height / 2, width, height / 2);
        context.strokeLine(width / 2, 0, width / 2, height);

        if (treasurePoint != null) {
            final Point2D treasure = transform(treasurePoint);
            context.setFill(Color.RED);
            context.fillOval(treasure.getX() - 1, treasure.getY() - 1, 3, 3);
        }

        context.setStroke(Color.WHITE);
        context.strokeRect(symbol.getX(), symbol.getY(), symbol.getWidth(), symbol.getHeight());

        context.setFill(Color.gray(1, .1));
        context.fillRect(symbol.getX(), symbol.getY(), symbol.getWidth(), symbol.getHeight());

        context.setStroke(Color.grayRgb(42));
        context.setLineWidth(2);
        context.strokeRect(0, 0, width, height);
    }
}
