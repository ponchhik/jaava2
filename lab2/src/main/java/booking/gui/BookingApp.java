package booking.gui;

import booking.service.BookingService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Приложение JavaFX: создаёт сервис и главное окно
public final class BookingApp extends Application {
    private static final String TITLE = "Бронирование переговорных — лабораторная №2 (дерево интервалов)";
    private static final double WIDTH = 1380, HEIGHT = 800;

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(new BookingService(), stage);
        stage.setTitle(TITLE);
        stage.setScene(new Scene(controller.root(), WIDTH, HEIGHT));
        stage.show();
        controller.start();
    }
}
