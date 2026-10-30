package booking.gui;

import javafx.scene.control.TextArea;

import java.time.LocalTime;

// Журнал действий
public final class EventLog extends TextArea {
    private static final int ROWS = 7;

    public EventLog() {
        setEditable(false);
        setWrapText(true);
        setPrefRowCount(ROWS);
    }

    public void println(String message) {
        appendText(LocalTime.now().withNano(0) + "  " + message + "\n");
        setScrollTop(Double.MAX_VALUE);
    }
}
