package booking.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

// Стандартные диалоги: сообщения и выбор файла. Без прикладной логики.
public final class UserDialogs {
    private static final String ERROR_TITLE = "Ошибка";
    private static final String CSV_NAME = "CSV";
    private static final String CSV_MASK = "*.csv";
    private static final String ANY_NAME = "Все файлы";
    private static final String ANY_MASK = "*.*";

    private final Stage owner;

    public UserDialogs(Stage owner) { this.owner = owner; }

    public Stage owner() { return owner; }

    public void error(String message) { show(Alert.AlertType.ERROR, ERROR_TITLE, message); }

    public void info(String title, String message) { show(Alert.AlertType.INFORMATION, title, message); }

    public Optional<Path> chooseFileToOpen() {
        File file = chooser().showOpenDialog(owner);
        return Optional.ofNullable(file).map(File::toPath);
    }

    public Optional<Path> chooseFileToSave() {
        File file = chooser().showSaveDialog(owner);
        return Optional.ofNullable(file).map(File::toPath);
    }

    private void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static FileChooser chooser() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(Path.of("").toAbsolutePath().toFile());
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(CSV_NAME, CSV_MASK),
                new FileChooser.ExtensionFilter(ANY_NAME, ANY_MASK));
        return fc;
    }
}
