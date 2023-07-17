package uk.nhs.prm.e2etests.performance;

import uk.nhs.prm.e2etests.performance.load.FinitePool;

import java.util.List;

public class RoundRobinPool<T> implements FinitePool<T> {
    private final List<T> items;
    private int nextIndex = 0;

    public RoundRobinPool(List<T> items) {
        this.items = items;
        nextIndex = 0;
    }

    public T next() {
        var item = items.get(nextIndex);
        if (++nextIndex >= items.size()) {
            nextIndex = 0;
        }
        return item;
    }

    @Override
    public boolean unfinished() {
        return true;
    }
}
