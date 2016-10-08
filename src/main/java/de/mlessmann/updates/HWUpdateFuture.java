package de.mlessmann.updates;

import de.mlessmann.common.parallel.IFuture;
import de.mlessmann.common.parallel.IFutureListener;
import de.mlessmann.common.parallel.IFutureProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by Life4YourGames on 07.10.16.
 */
public class HWUpdateFuture<T> implements IFuture<T> {

    private IFutureProvider<T> provider;

    private List<IFutureListener> listeners = new ArrayList<IFutureListener>();

    public HWUpdateFuture(IFutureProvider<T> provider) {
        this.provider = provider;
    }

    public boolean isPresent() {
        return provider.getPayload(this) != null;
    }

    public T get() throws NoSuchElementException {
        T p = provider.getPayload(this);
        if (p == null)
            throw new NoSuchElementException("No payload present");
        return p;
    }

    public T getOrElse(T def) {
        return isPresent() ? provider.getPayload(this) : def;
    }

    public void registerListener(IFutureListener l) {
        listeners.add(l);
    }

    public void unregisterListener(IFutureListener l) {
        listeners.remove(l);
    }

    public void pokeListeners() {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onFutureAvailable(this);
    }
}
