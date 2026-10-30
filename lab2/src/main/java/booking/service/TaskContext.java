package booking.service;

// Связь фоновой работы с GUI: прогресс и отмена (аналог updateProgress/isCancelled у javafx Task)
public interface TaskContext {
    void progress(int done, int total);
    boolean isCancellationRequested();

    TaskContext NONE = new TaskContext() {
        @Override public void progress(int done, int total) { }
        @Override public boolean isCancellationRequested() { return false; }
    };
}
