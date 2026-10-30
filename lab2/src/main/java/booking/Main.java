package booking;

import booking.gui.BookingApp;

import javafx.application.Application;

// Отдельная точка входа: класс с main не наследует Application, поэтому JavaFX запускается и с classpath
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Application.launch(BookingApp.class, args);
    }
}
