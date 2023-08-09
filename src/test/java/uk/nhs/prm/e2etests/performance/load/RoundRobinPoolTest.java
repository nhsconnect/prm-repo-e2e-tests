package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.performance.RoundRobinPool;

import java.util.List;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinPoolTest {

    private final List<Integer> integers = of(1, 2, 3);

    @Test
    void shouldGiveFirstItemFirst() {
        RoundRobinPool<Integer> pool = new RoundRobinPool<>(integers);

        int first = pool.next();

        assertThat(first).isEqualTo(1);
    }

    @Test
    void shouldGiveSecondItemSecond() {
        RoundRobinPool<Integer> pool = new RoundRobinPool<>(integers);

        pool.next();
        int second = pool.next();

        assertThat(second).isEqualTo(2);
    }

    @Test
    void shouldLoopRoundToFirstItemAgainAfterPoolExhausted() {
        RoundRobinPool<Integer> poolOfThree = new RoundRobinPool<>(integers);

        poolOfThree.next();
        poolOfThree.next();
        int third = poolOfThree.next();
        int fourth = poolOfThree.next();

        assertThat(third).isEqualTo(3);
        assertThat(fourth).isEqualTo(1);
    }
}