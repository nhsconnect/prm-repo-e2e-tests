package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deduction.e2e.timng.Sleeper;
import uk.nhs.prm.deduction.e2e.tests.RoundRobinPool;
import uk.nhs.prm.deduction.e2e.timing.Timer;

import java.util.List;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadRegulatingPoolTest {
    private LoadRegulatingPool<Integer> pool;
    private List<Integer> integers = of(1, 2, 3, 4, 5);

    @Mock
    private Sleeper sleeper;

    @Mock
    private Timer timer;

    @Test
    public void shouldProvideFirstItemWithoutDelay() {
        pool = createPool(integers, timer, sleeper, of());

        int item = pool.next();

        assertThat(item).isEqualTo(1);
        verify(sleeper, never()).sleep(anyInt());
    }

    @Test
    public void shouldProvideSecondItemAfterAppropriateSleepToAchieveFlatRateLoad() {
        var ratePerSecond = "1";
        long startTimeMillis = 2000L;

        pool = createPool(integers, timer, sleeper, of(LoadPhase.atFlatRate(ratePerSecond, 10)));

        when(timer.milliseconds()).thenReturn(startTimeMillis);
        pool.next();

        long elapsedMillisDuringFirstItemLoad = 400L;
        when(timer.milliseconds()).thenReturn(startTimeMillis + elapsedMillisDuringFirstItemLoad);

        pool.next();

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

    private LoadRegulatingPool createPool(List<Integer> items, Timer timer, Sleeper sleeper, List<LoadPhase> phases) {
        return new LoadRegulatingPool(new RoundRobinPool(items), phases, timer, sleeper);
    }

}