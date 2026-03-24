package datawave.marking;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Markings<T> {

    T getMarkings();

    default boolean isEmpty() {
        return getMarkings() == null;
    }
}
