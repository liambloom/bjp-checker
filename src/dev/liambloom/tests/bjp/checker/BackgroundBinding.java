package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;

public class BackgroundBinding extends ObjectBinding<Background> {
    private final ObservableValue<? extends Paint> paint;
    private final ObservableValue<? extends CornerRadii> radii;
    private final ObservableValue<? extends Insets> insets;

    public BackgroundBinding(ObservableValue<? extends Paint> paint) {
        this(paint, new SimpleObjectProperty<>(CornerRadii.EMPTY));
    }

    public BackgroundBinding(ObservableValue<? extends Paint> paint, ObservableValue<? extends CornerRadii> radii) {
        this(paint, radii, new SimpleObjectProperty<>(Insets.EMPTY));
    }

    public BackgroundBinding(ObservableValue<? extends Paint> paint, ObservableValue<? extends CornerRadii> radii, ObservableValue<? extends Insets> insets) {
        bind(
                this.paint = paint,
                this.radii = radii,
                this.insets = insets
        );
    }

    @Override
    protected Background computeValue() {
        return new Background(new BackgroundFill(paint.getValue(), radii.getValue(), insets.getValue()));
    }
}