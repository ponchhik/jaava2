package booking.model;

import java.time.LocalDateTime;
import java.util.List;

// Обычная бронь
public class SingleBooking extends Booking implements Editable {
    private String comment;

    public SingleBooking(int id, String room, String employee,
                         LocalDateTime start, LocalDateTime end, String comment) {
        super(id, room, employee, start, end);
        this.comment = comment == null ? "" : comment;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public List<String> validate() {
        List<String> errors = validateCommon();
        if (hasBadChar(comment)) {
            errors.add("В комментарии нельзя использовать символ «;» и переводы строк");
        }
        return errors;
    }

    @Override public String csvType() { return "SINGLE"; }
    @Override public String typeTitle() { return "Обычная"; }
    @Override public String extraCsv() { return comment; }
    @Override public String extraText() { return comment; }
}
