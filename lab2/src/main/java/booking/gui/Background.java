package booking.gui;

import javafx.concurrent.Task;

// Запуск Task в фоновом daemon-потоке (чтобы незавершённая задача не мешала закрыть приложение)
public final class Background {
    private Background() { }

    public static void start(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
