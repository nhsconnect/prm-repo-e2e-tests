package uk.nhs.prm.e2etests.performance.load;

public class MixerPool<T> implements Pool<T> {
    private final float targetRatio;
    private int count1;
    private int count2;
    private Pool<T> sourceOne;
    private Pool<T> sourceTwo;

    public MixerPool(int weighting1, Pool<T> sourceOne, int weighting2, Pool<T> sourceTwo) {
        this.sourceOne = sourceOne;
        this.sourceTwo = sourceTwo;
        this.targetRatio = ratio(weighting1, weighting2);
        this.count1 = 0;
        this.count2 = 0;
    }

    @Override
    public T next() {
        T next;
        if (currentRatio() <= targetRatio) {
            next = sourceOne.next();
            count1++;
        }
        else {
            next = sourceTwo.next();
            count2++;
        }
        return next;
    }

    private float currentRatio() {
        if (count1 == 0) {
            return 0;
        }
        return ratio(count1, count2);
    }

    private float ratio(float weighting1, float weighting2) {
        return weighting1 / (weighting1 + weighting2);
    }
}
