package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.e2e.tests.RoundRobinPool;

import java.util.List;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinPoolTest {

    private List<Integer> integers = of(1, 2, 3);

    @Test
    public void shouldGiveFirstItemFirst() {
        var pool = new RoundRobinPool<>(integers);

        var first = pool.next();

        assertThat(first).isEqualTo(1);
    }

    @Test
    public void shouldGiveSecondItemSecond() {
        var pool = new RoundRobinPool<>(integers);

        pool.next();
        var second = pool.next();

        assertThat(second).isEqualTo(2);
    }

    @Test
    public void shouldLoopRoundToFirstItemAgainAfterPoolExhausted() {
        var poolOfThree = new RoundRobinPool<>(integers);

        poolOfThree.next();
        poolOfThree.next();
        var third = poolOfThree.next();
        var fourth = poolOfThree.next();

        assertThat(third).isEqualTo(3);
        assertThat(fourth).isEqualTo(1);
    }
}