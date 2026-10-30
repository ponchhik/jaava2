package booking.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

//Регулярная бронь. Новое поле — дни недели
public class RecurringBooking extends Booking implements Editable {
    private static final Locale RU = Locale.forLanguageTag("ru");

    private final Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

    public RecurringBooking(int id, String room, String employee,
                            LocalDateTime start, LocalDateTime end, Set<DayOfWeek> days) {
        super(id, room, employee, start, end);
        this.days.addAll(days);
    }

    public Set<DayOfWeek> getDays() {
        return Collections.unmodifiableSet(days);
    }

    public void setDays(Set<DayOfWeek> newDays) {
        days.clear();
        days.addAll(newDays);
    }

    @Override
    public List<String> validate() {
        List<String> errors = validateCommon();
        if (days.isEmpty()) {
            errors.add("Выберите хотя бы один день недели");
        }
        return errors;
    }

    @Override public String csvType() { return "RECURRING"; }
    @Override public String typeTitle() { return "Регулярная"; }

    @Override
    public String extraCsv() {
        return days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    @Override
    public String extraText() {
        return days.stream()
                .map(d -> d.getDisplayName(TextStyle.SHORT, RU))
                .collect(Collectors.joining(", "));
    }
}
