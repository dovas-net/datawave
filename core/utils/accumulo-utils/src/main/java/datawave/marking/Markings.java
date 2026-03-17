package datawave.marking;

public interface Markings<T> {

    T getMarkings();

    default boolean isEmpty() {
        return getMarkings() == null;
    }
}
