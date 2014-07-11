package org.zenoss.app.metricservice.buckets;

/**
 * Implementation of Welford algorithm for computing running mean/variance/standard deviation.
 */
public class NewValue {
    private long numDataValues = 0;
    private double oldMean, newMean, oldS, newS;
    private double sum;

    public void clear() {
        numDataValues = 0;
        sum = 0;
    }

    public void push(double x) {
        numDataValues++;
        sum += x;
        if (numDataValues == 1) {
            oldMean = newMean = x;
            oldS = 0.0;
        } else {
            newMean = oldMean + (x - oldMean) / numDataValues;
            newS = oldS + (x - oldMean) * (x - newMean);
            oldMean = newMean;
            oldS = newS;
        }
    }

    public long getNumDataValues() {
        return numDataValues;
    }

    public double getMean() {
           return (numDataValues > 0) ? newMean : Double.NaN;
    }

    public double getVariance() {
        return ((numDataValues > 1) ? newS /(numDataValues - 1) : Double.NaN);
    }

    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

    public double getSum() {
        return sum;
    }
}
