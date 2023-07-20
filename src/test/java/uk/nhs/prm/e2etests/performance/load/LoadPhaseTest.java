package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.e2etests.timing.Sleeper;
import uk.nhs.prm.e2etests.timing.Timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadPhaseTest {

    @Mock
    private Sleeper sleeper;

    @Mock
    private Timer timer;

    @Test
    public void applyDelayShouldNotDelayIfThereIsNoPreviousTimeThatDelayApplied_WhichCouldBeMovingToNextPhaseWhichIsWhyTheLastTimeStateIsExternalised() {
        var loadPhase = LoadPhase.atFlatRate(5, "1");

        loadPhase.applyDelay(timer, sleeper, null);

        verify(sleeper, never()).sleep(anyInt());
    }

    @Test
    public void applyDelayShouldReturnTimeNowIfNoDelayApplied() {
        var loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(123L);

        var timeAfterDelay = loadPhase.applyDelay(timer, sleeper, null);

        assertThat(timeAfterDelay).isEqualTo(123L);
    }

    @Test
    public void applyDelayShouldSleepTheFullRequiredDelayTimeIfNoTimeHasElapsedSinceLastTime() {
        var loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(1000L);

        loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        verify(sleeper, times(1)).sleep(1000);
    }

    @Test
    public void applyDelayShouldReturnTheTimeAfterDelayIfSleptAsReturnedByTheSleeper() {
        var loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(1000L);
        when(sleeper.sleep(anyInt())).thenReturn(2000L);

        var timeAfterSecondDelay = loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        assertThat(timeAfterSecondDelay).isEqualTo(2000L);
    }

    @Test
    public void applyDelayShouldMakeUpTheTimeAppropriatelyIfSomeTimeElapsedFromLastTime() {
        var loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(1500L);
        when(sleeper.sleep(anyInt())).thenReturn(2000L);

        var timeAfterSecondDelay = loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        verify(sleeper, times(1)).sleep(500);
        assertThat(timeAfterSecondDelay).isEqualTo(2000L);
    }

    @Test
    public void applyDelayShouldWorkForRatesHigherThanOnePerSecond() {
        var loadPhase = LoadPhase.atFlatRate(5, "20"); // 20 per second, every 50 ms

        when(timer.milliseconds()).thenReturn(0L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(10L);
        when(sleeper.sleep(anyInt())).thenReturn(50L);

        var timeAfterSecondDelay = loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        verify(sleeper, times(1)).sleep(40);

        when(timer.milliseconds()).thenReturn(72L);
        when(sleeper.sleep(anyInt())).thenReturn(100L);

        loadPhase.applyDelay(timer, sleeper, timeAfterSecondDelay);

        verify(sleeper, times(1)).sleep(28);
    }

    @Test
    public void applyDelayShouldWorkForRatesLowerThanOnePerSecond() {
        var loadPhase = LoadPhase.atFlatRate(5, "0.02"); // one every 50 seconds

        when(timer.milliseconds()).thenReturn(0L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(10 * 1000L);
        when(sleeper.sleep(anyInt())).thenReturn(50 * 1000L);

        var timeAfterSecondDelay = loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        verify(sleeper, times(1)).sleep(40 * 1000);

        when(timer.milliseconds()).thenReturn(72 * 1000L);
        when(sleeper.sleep(anyInt())).thenReturn(100 * 1000L);

        loadPhase.applyDelay(timer, sleeper, timeAfterSecondDelay);

        verify(sleeper, times(1)).sleep(28 * 1000);
    }

    @Test
    public void applyDelayShouldApplyNoDelayIfAlreadyTooSlowForDesiredRate() {
        var loadPhase = LoadPhase.atFlatRate(5, "10");

        when(timer.milliseconds()).thenReturn(0L);

        var timeAfterFirstDelay = loadPhase.applyDelay(timer, sleeper, null);

        when(timer.milliseconds()).thenReturn(200L);

        loadPhase.applyDelay(timer, sleeper, timeAfterFirstDelay);

        verify(sleeper, never()).sleep(anyInt());
    }
}