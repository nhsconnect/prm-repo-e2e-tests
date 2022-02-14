package uk.nhs.prm.deduction.e2e.performance.load;

import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deduction.e2e.performance.RoundRobinPool;
import uk.nhs.prm.deduction.e2e.timing.Sleeper;
import uk.nhs.prm.deduction.e2e.timing.Timer;

import java.util.List;

import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadRegulatingPoolTest {
    private LoadRegulatingPool<PhasedInteger> pool;
    private List<Integer> integers = of(1, 2, 3, 4, 5);

    @Mock
    private Sleeper sleeper;

    @Mock
    private Timer timer;

    @Test
    public void shouldProvideFirstItemWithoutDelay() {
        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate("1", 1)));

        var item = pool.next();

        assertThat(item).isEqualTo(new PhasedInteger(1));
        verify(sleeper, never()).sleep(anyInt());
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateLoad() {
        var ratePerSecond = "1";
        long startTimeMillis = 2000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        assertThat(pool.next()).isEqualTo(new PhasedInteger(1));

        long elapsedMillisDuringFirstItemLoad = 400L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        assertThat(pool.next()).isEqualTo(new PhasedInteger(2));

        verify(sleeper, times(1)).sleep(600);
    }

    @Test
    public void shouldProvideSubsequentItemsAfterAppropriateSleepsToAchieveFlatRateLoad() {
        var ratePerSecond = "1";

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(0L);
        pool.next();

        when(timer.milliseconds()).thenReturn(800L);
        pool.next();

        when(timer.milliseconds()).thenReturn(1700L);
        pool.next();

        when(timer.milliseconds()).thenReturn(2600L);
        pool.next();

        verify(sleeper, times(1)).sleep(200);
        verify(sleeper, times(1)).sleep(300);
        verify(sleeper, times(1)).sleep(400);
    }

    @Test
    public void shouldBeFinishedIfSinglePhaseCompleted() {
        var ratePerSecond = "1";

        int phaseCount = 3;
        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, phaseCount)));

        when(timer.milliseconds()).thenReturn(0L);

        pool.next();
        assertThat(pool.unfinished()).isTrue();
        pool.next();
        assertThat(pool.unfinished()).isTrue();
        pool.next();
        assertThat(pool.unfinished()).isFalse();
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateAboveOnePerSecond() {
        var ratePerSecond = "10";
        long startTimeMillis = 3000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        pool.next();

        long elapsedMillisDuringFirstItemLoad = 20L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        pool.next();

        verify(sleeper, times(1)).sleep(80);
    }

    @Test
    public void shouldUseNotSleepIfAlreadyElapsedMoreThanRequiredDelayForRate() {
        var ratePerSecond = "10";
        long startTimeMillis = 1000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        pool.next();

        long elapsedMillisDuringFirstItemLoad = 100L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        pool.next();

        verify(sleeper, never()).sleep(anyInt());
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateSlowerThanOnePerSecond() {
        var oneEvery100SecondsRatePerSecond = "0.01";

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(oneEvery100SecondsRatePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(0l);
        pool.next();

        when(timer.milliseconds()).thenReturn(0L);
        pool.next();

        verify(sleeper, times(1)).sleep(100 * 1000);
    }

    @Test
    public void shouldMoveToSecondPhaseAndOnlyBeFinishedIfThatRunsOut() {
        var ratePerSecond = "1";

        pool = createPool(integers, timer, sleeper, of(
                LoadPhase.atFlatRate(ratePerSecond, 3),
                LoadPhase.atFlatRate(ratePerSecond, 2)
        ));

        when(timer.milliseconds()).thenReturn(0L);

        pool.next();
        pool.next();
        pool.next();
        assertThat(pool.unfinished()).isTrue();
        pool.next();
        assertThat(pool.unfinished()).isTrue();
        pool.next();
        assertThat(pool.unfinished()).isFalse();
    }


    @Test
    public void shouldMoveToSecondPhaseAndUseItsRateAfterFirstPhaseCompletes() {
        var initialRatePerSecond = "1";
        var secondRatePerSecondFor200msDelay = "5";
        pool = createPool(integers, timer, sleeper, of(
                LoadPhase.atFlatRate(initialRatePerSecond, 2),
                LoadPhase.atFlatRate(secondRatePerSecondFor200msDelay, 2)
        ));

        when(timer.milliseconds()).thenReturn(0L);
        pool.next();
        pool.next();
        verify(sleeper, times(1)).sleep(1000);
        when(timer.milliseconds()).thenReturn(1000L);
        pool.next();
        verify(sleeper, times(1)).sleep(200); // first 200ms delay
        when(timer.milliseconds()).thenReturn(1300L);
        pool.next();
        verify(sleeper, times(1)).sleep(100); // second 200ms delay
    }

    private LoadRegulatingPool createPool(List<Integer> items, Timer timer, Sleeper sleeper, List<LoadPhase> phases) {
        List<PhasedInteger> phasedItems = items.stream().map(integer -> new PhasedInteger(integer)).collect(toList());
        return new LoadRegulatingPool(new RoundRobinPool(phasedItems), phases, timer, sleeper);
    }

    @EqualsAndHashCode
    private class PhasedInteger implements Phased {
        private Integer integer;

        public PhasedInteger(Integer integer) {
            this.integer = integer;
        }

        @Override
        public void setPhase(LoadPhase phase) {

        }

        @Override
        public LoadPhase phase() {
            return null;
        }
    }
}