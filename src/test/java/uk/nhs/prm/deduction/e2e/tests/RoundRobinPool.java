package uk.nhs.prm.deduction.e2e.tests;

import uk.nhs.prm.deduction.e2e.performance.Pool;

import java.util.List;

public class RoundRobinPool<T> implements Pool<T> {
    public T lastItem;
    private List<T> items;

    public RoundRobinPool(List<T> items) {
        this.items = items;
        lastItem = items.get(0);
    }

    public T next() {
        var list = items;
        int index = list.indexOf(lastItem);
        if (index < list.size() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        lastItem = list.get(index);
        return lastItem;
    }

}
