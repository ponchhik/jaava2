package booking.ds;

// Собственный «предикат»
public interface Matcher<T> {
    boolean matches(T item);
}
