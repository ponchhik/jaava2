package booking.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

// Нижняя панель: состояние фоновой задачи, прогресс, отмена
public final class StatusBar extends HBox {
    private static final double SPACING = 8;
    private static final double BAR_WIDTH = 220;
    private static final String CAPTION = "Фоновая задача:";
    private static final String IDLE = "нет";

    private final Label state = new Label(IDLE);
    private final ProgressBar bar = new ProgressBar(0);
    private final Button cancel = new Button("Отмена");
    private final CheckBox slowBox = new CheckBox("Замедлить (демо отмены)");
    private volatile boolean slow;      // читается из фонового потока

    public StatusBar() {
        super(SPACING);
        setPadding(new Insets(SPACING / 2));
        bar.setPrefWidth(BAR_WIDTH);
        cancel.setDisable(true);
        slowBox.selectedProperty().addListener((o, a, b) -> slow = b);
        getChildren().addAll(new Label(CAPTION), state, bar, cancel, slowBox);
    }

    public ProgressBar bar() { return bar; }
    public Button cancelButton() { return cancel; }
    public void setState(String text) { state.setText(text); }
    public String idleText() { return IDLE; }
    public boolean slowDemo() { return slow; }
}
