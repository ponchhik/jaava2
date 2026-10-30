package booking.gui;

import booking.ds.ArrayBuf;
import booking.model.Booking;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// Таблица броней — только ВИД снимка дерева (по возрастанию начала).
// Сортировка по клику на заголовок отключена: порядок задаёт дерево, а сортировка 10^5 строк в потоке GUI была бы долгой.
public final class BookingTable extends TableView<Booking> {

    public BookingTable() {
        for (BookingColumn column : BookingColumn.values()) {
            TableColumn<Booking, String> c = new TableColumn<>(column.title());
            c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(column.text(cell.getValue())));
            c.setPrefWidth(column.width());
            c.setSortable(false);
            getColumns().add(c);
        }
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    public ReadOnlyObjectProperty<Booking> selectedProperty() {
        return getSelectionModel().selectedItemProperty();
    }

    public Booking selected() { return getSelectionModel().getSelectedItem(); }

    public void setRows(ArrayBuf<Booking> rows) { setItems(new ArrayBufList<>(rows)); }

    // Выделить конкретную бронь (по ссылке) и прокрутить к ней
    public void select(Booking booking) {
        for (int i = 0; i < getItems().size(); i++) {
            if (getItems().get(i) == booking) {
                getSelectionModel().select(i);
                scrollTo(i);
                return;
            }
        }
    }
}
