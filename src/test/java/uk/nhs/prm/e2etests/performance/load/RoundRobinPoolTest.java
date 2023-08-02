package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.performance.RoundRobinPool;

import java.util.List;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinPoolTest {

    private List<Integer> integers = of(1, 2, 3);

    @Test
    public void shouldGiveFirstItemFirst() {
        RoundRobinPool<Integer> pool = new RoundRobinPool<>(integers);

        Integer first = pool.next();

        assertThat(first).isEqualTo(1);
    }

    @Test
    public void shouldGiveSecondItemSecond() {
        RoundRobinPool<Integer> pool = new RoundRobinPool<>(integers);

        pool.next();
        Integer second = pool.next();

        assertThat(second).isEqualTo(2);
    }

    @Test
    public void shouldLoopRoundToFirstItemAgainAfterPoolExhausted() {
        RoundRobinPool<Integer> poolOfThree = new RoundRobinPool<>(integers);

        poolOfThree.next();
        poolOfThree.next();
        Integer third = poolOfThree.next();
        Integer fourth = poolOfThree.next();

        assertThat(third).isEqualTo(3);
        assertThat(fourth).isEqualTo(1);
    }
}