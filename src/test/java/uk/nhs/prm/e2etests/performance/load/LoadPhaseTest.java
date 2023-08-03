package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.e2etests.timing.Timer;
import uk.nhs.prm.e2etests.utility.ThreadUtility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadPhaseTest {

    @Mock
    private ThreadUtility sleeper;

    @Mock
    private Timer timer;

    @Test
    void applyDelayShouldNotDelayIfThereIsNoPreviousTimeThatDelayApplied_WhichCouldBeMovingToNextPhaseWhichIsWhyTheLastTimeStateIsExternalised() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "1");

        loadPhase.applyDelay(null);

        verify(sleeper, never()).sleepFor(anyInt());
    }

    @Test
    void applyDelayShouldReturnTimeNowIfNoDelayApplied() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(123L);

        Long timeAfterDelay = loadPhase.applyDelay(null);

        assertThat(timeAfterDelay).isEqualTo(123L);
    }

    @Test
    void applyDelayShouldSleepTheFullRequiredDelayTimeIfNoTimeHasElapsedSinceLastTime() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(1000L);

        loadPhase.applyDelay(timeAfterFirstDelay);

        verify(sleeper, times(1)).sleepFor(1000);
    }

    @Test
    void applyDelayShouldReturnTheTimeAfterDelayIfSleptAsReturnedByTheSleeper() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(1000L);
        when(sleeper.sleepFor(anyInt())).thenReturn(2000L);

        Long timeAfterSecondDelay = loadPhase.applyDelay(timeAfterFirstDelay);

        assertThat(timeAfterSecondDelay).isEqualTo(2000L);
    }

    @Test
    void applyDelayShouldMakeUpTheTimeAppropriatelyIfSomeTimeElapsedFromLastTime() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "1");

        when(timer.milliseconds()).thenReturn(1000L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(1500L);
        when(sleeper.sleepFor(anyInt())).thenReturn(2000L);

        Long timeAfterSecondDelay = loadPhase.applyDelay(timeAfterFirstDelay);

        verify(sleeper, times(1)).sleepFor(500);
        assertThat(timeAfterSecondDelay).isEqualTo(2000L);
    }

    @Test
    void applyDelayShouldWorkForRatesHigherThanOnePerSecond() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "20"); // 20 per second, every 50 ms

        when(timer.milliseconds()).thenReturn(0L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(10L);
        when(sleeper.sleepFor(anyInt())).thenReturn(50L);

        Long timeAfterSecondDelay = loadPhase.applyDelay(timeAfterFirstDelay);

        verify(sleeper, times(1)).sleepFor(40);

        when(timer.milliseconds()).thenReturn(72L);
        when(sleeper.sleepFor(anyInt())).thenReturn(100L);

        loadPhase.applyDelay(timeAfterSecondDelay);

        verify(sleeper, times(1)).sleepFor(28);
    }

    @Test
    void applyDelayShouldWorkForRatesLowerThanOnePerSecond() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "0.02"); // one every 50 seconds

        when(timer.milliseconds()).thenReturn(0L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(10 * 1000L);
        when(sleeper.sleepFor(anyInt())).thenReturn(50 * 1000L);

        Long timeAfterSecondDelay = loadPhase.applyDelay(timeAfterFirstDelay);

        verify(sleeper, times(1)).sleepFor(40 * 1000);

        when(timer.milliseconds()).thenReturn(72 * 1000L);
        when(sleeper.sleepFor(anyInt())).thenReturn(100 * 1000L);

        loadPhase.applyDelay(timeAfterSecondDelay);

        verify(sleeper, times(1)).sleepFor(28 * 1000);
    }

    @Test
    void applyDelayShouldApplyNoDelayIfAlreadyTooSlowForDesiredRate() {
        LoadPhase loadPhase = LoadPhase.atFlatRate(5, "10");

        when(timer.milliseconds()).thenReturn(0L);

        Long timeAfterFirstDelay = loadPhase.applyDelay(null);

        when(timer.milliseconds()).thenReturn(200L);

        loadPhase.applyDelay(timeAfterFirstDelay);

        verify(sleeper, never()).sleepFor(anyInt());
    }
}