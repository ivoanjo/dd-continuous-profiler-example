package movies;

import java.util.function.Supplier;

public final class CachedSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private volatile T data;

    public CachedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (data == null) {
            synchronized(this) {
                if (data == null) {
                    data = supplier.get();
                }
            }
        }
        return data;
    }
}