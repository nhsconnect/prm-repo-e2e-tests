package uk.nhs.prm.e2etests.performance.load;

import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.e2etests.performance.RoundRobinPool;
import uk.nhs.prm.e2etests.utility.ThreadUtility;
import uk.nhs.prm.e2etests.timing.Timer;

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
    private ThreadUtility sleeper;

    @Mock
    private Timer timer;

    @Test
    public void shouldProvideFirstItemWithoutDelay() {
        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(1, "1")));

        PhasedInteger item = pool.next();

        assertThat(item).isEqualTo(new PhasedInteger(1));
        verify(sleeper, never()).sleepFor(anyInt());
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateLoad() {
        String ratePerSecond = "1";
        long startTimeMillis = 2000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(10, ratePerSecond)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        assertThat(pool.next()).isEqualTo(new PhasedInteger(1));

        long elapsedMillisDuringFirstItemLoad = 400L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        assertThat(pool.next()).isEqualTo(new PhasedInteger(2));

        verify(sleeper, times(1)).sleepFor(600);
    }

    @Test
    public void shouldProvideSubsequentItemsAfterAppropriateSleepsToAchieveFlatRateLoad() {
        String ratePerSecond = "1";

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(10, ratePerSecond)));

        when(timer.milliseconds()).thenReturn(0L);
        pool.next();
        verify(sleeper, never()).sleepFor(anyInt());

        when(timer.milliseconds()).thenReturn(800L);
        when(sleeper.sleepFor(anyInt())).thenReturn(1000L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(200);

        when(timer.milliseconds()).thenReturn(1700L);
        when(sleeper.sleepFor(anyInt())).thenReturn(2000L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(300);

        when(timer.milliseconds()).thenReturn(2600L);
        when(sleeper.sleepFor(anyInt())).thenReturn(3000L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(400);
    }

    @Test
    public void shouldBeFinishedIfSinglePhaseCompleted() {
        String ratePerSecond = "1";

        int phaseCount = 3;
        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(phaseCount, ratePerSecond)));

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
        String ratePerSecond = "10";
        long startTimeMillis = 3000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(10, ratePerSecond)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        pool.next();

        verify(sleeper, never()).sleepFor(anyInt());

        long elapsedMillisDuringFirstItemLoad = 20L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);
        when(sleeper.sleepFor(anyInt())).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad + 80);

        pool.next();

        verify(sleeper, times(1)).sleepFor(80);
    }

    @Test
    public void shouldUseNotSleepIfAlreadyElapsedMoreThanRequiredDelayForRate() {
        String ratePerSecond = "10";
        long startTimeMillis = 1000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(10, ratePerSecond)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        pool.next();

        verify(sleeper, never()).sleepFor(anyInt());

        long elapsedMillisDuringFirstItemLoad = 100L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        pool.next();

        verify(sleeper, never()).sleepFor(anyInt());
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateSlowerThanOnePerSecond() {
        String oneEvery100SecondsRatePerSecond = "0.01";

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(10, oneEvery100SecondsRatePerSecond)));

        when(timer.milliseconds()).thenReturn(0l);
        pool.next();

        verify(sleeper, never()).sleepFor(anyInt());

        when(timer.milliseconds()).thenReturn(0L);
        when(sleeper.sleepFor(anyInt())).thenReturn(100 * 1000L);
        pool.next();

        verify(sleeper, times(1)).sleepFor(100 * 1000);
    }

    @Test
    public void shouldMoveToSecondPhaseAndOnlyBeFinishedIfThatRunsOut() {
        String ratePerSecond = "1";

        pool = createPool(integers, timer, sleeper, of(
                LoadPhase.atFlatRate(3, ratePerSecond),
                LoadPhase.atFlatRate(2, ratePerSecond)
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
        String initialRatePerSecond = "1";
        String secondRatePerSecondFor200msDelay = "5";
        pool = createPool(integers, timer, sleeper, of(
                LoadPhase.atFlatRate(2, initialRatePerSecond),
                LoadPhase.atFlatRate(2, secondRatePerSecondFor200msDelay)
        ));

        when(timer.milliseconds()).thenReturn(0L);
        pool.next();
        verify(sleeper, never()).sleepFor(1000);

        when(sleeper.sleepFor(anyInt())).thenReturn(1000L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(1000);

        when(timer.milliseconds()).thenReturn(1000L);
        when(sleeper.sleepFor(anyInt())).thenReturn(1200L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(200); // first 200ms delay

        when(timer.milliseconds()).thenReturn(1300L);
        when(sleeper.sleepFor(anyInt())).thenReturn(1400L);
        pool.next();
        verify(sleeper, times(1)).sleepFor(100); // second 200ms delay
    }

    private LoadRegulatingPool createPool(List<Integer> items, Timer timer, ThreadUtility sleeper, List<LoadPhase> phases) {
        List<PhasedInteger> phasedItems = items.stream().map(integer -> new PhasedInteger(integer)).collect(toList());
        return new LoadRegulatingPool(new RoundRobinPool(phasedItems), phases);
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