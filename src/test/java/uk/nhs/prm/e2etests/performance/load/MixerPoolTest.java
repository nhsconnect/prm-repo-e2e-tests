package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.performance.NemsTestEvent;

import static org.assertj.core.api.Assertions.assertThat;

class MixerPoolTest {

    @Test
    void shouldProvideAlternatelyFromEachSourcePoolAtFiftyFiftyMix() {
        NemsTestEvent eventOne = NemsTestEvent.nonSuspensionEvent("1111" , "111");
        NemsTestEvent eventTwo = NemsTestEvent.nonSuspensionEvent("2222" , "222");

        Pool sourceOne = new NemsTestEventPool(eventOne);
        Pool sourceTwo = new NemsTestEventPool(eventTwo);

        MixerPool mixerPool = new MixerPool(50, sourceOne, 50, sourceTwo);

        assertThat(mixerPool.next()).isEqualTo(eventOne);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
        assertThat(mixerPool.next()).isEqualTo(eventOne);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
    }

    @Test
    void shouldProvideFirstAndEveryFourFromFirstPoolForOneQuarterMix() {
        NemsTestEvent eventOne = NemsTestEvent.nonSuspensionEvent("1111" , "111");
        NemsTestEvent eventTwo = NemsTestEvent.nonSuspensionEvent("2222" , "222");

        Pool sourceOne = new NemsTestEventPool(eventOne);
        Pool sourceTwo = new NemsTestEventPool(eventTwo);

        MixerPool mixerPool = new MixerPool(1000, sourceOne, 3000, sourceTwo);

        assertThat(mixerPool.next()).isEqualTo(eventOne);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
        assertThat(mixerPool.next()).isEqualTo(eventOne);
        assertThat(mixerPool.next()).isEqualTo(eventTwo);
    }
}