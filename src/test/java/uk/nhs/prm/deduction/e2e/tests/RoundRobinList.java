package uk.nhs.prm.deduction.e2e.tests;

import java.util.List;

public class RoundRobinList {
    public String lastItem;
    private List<String> items;

    public RoundRobinList(List<String> items) {
        this.items = items;
        lastItem = items.get(0);
    }

    public String next() {
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
