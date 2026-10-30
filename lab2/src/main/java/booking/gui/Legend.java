package booking.gui;

import booking.ds.Trace;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

// Легенда цветов подсветки
public final class Legend extends HBox {
    private static final double SPACING = 8;

    public Legend() {
        super(SPACING);
        for (Trace.Kind kind : Trace.Kind.values()) {
            Label l = new Label(TreeStyle.legend(kind));
            l.setPadding(new Insets(0, 4, 0, 4));
            l.setBackground(new Background(new BackgroundFill(TreeStyle.fill(kind), CornerRadii.EMPTY, Insets.EMPTY)));
            l.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    BorderStroke.THIN)));
            getChildren().add(l);
        }
    }
}
