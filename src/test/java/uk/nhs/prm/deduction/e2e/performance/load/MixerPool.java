package uk.nhs.prm.deduction.e2e.performance.load;

public class MixerPool<T> implements Pool<T> {
    private boolean oneNext;
    private Pool<T> sourceOne;
    private Pool<T> sourceTwo;

    public MixerPool(int weighting1, Pool<T> sourceOne, int weighting2, Pool<T> sourceTwo) {
        this.sourceOne = sourceOne;
        this.sourceTwo = sourceTwo;
        oneNext = true;
    }

    @Override
    public T next() {
        T next;
        if (oneNext) {
            next = sourceOne.next();
        }
        else {
            next = sourceTwo.next();
        }
        oneNext = !oneNext;
        return next;
    }
}
