package de.mlessmann.util;

/**
 * Created by Life4YourGames on 09.07.16.
 */
public class ObjectBox<T> {

    private T payload;
    private int error;

    /**
     * Some sort of an own implementation of an Optional
     * Just that now the supplier can send an error code
     * @param obj Object to supply
     * @param errorCode Error code to supply
     */
    public ObjectBox(T obj, int errorCode) {

        error = errorCode;
        payload = obj;

    }

    public int getError() { return error; }

    public T get() { return payload; }

    public T getOrElse(T def) { return isPresent() ? get() : def; }

    public boolean isPresent() { return payload != null; }

}
