package uk.nhs.prm.deduction.e2e.performance.reporting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import uk.nhs.prm.deduction.e2e.performance.NemsTestRecording;
import uk.nhs.prm.deduction.e2e.performance.load.LoadPhase;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerformanceChartGenerator {
    public static void generateProcessingDurationScatterPlot(NemsTestRecording recording, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (var series : createPerPhaseDataSerieses(recording)) {
            dataset.addSeries(series);
        }

        JFreeChart scatterPlot = ChartFactory.createScatterPlot(
                title,
                "Time since run start (seconds)", // X-Axis Label
                "Processing duration (seconds)", // Y-Axis Label
                dataset
        );

        savePlotAsPngTo(scatterPlot, "build/reports/performance/", "durations.png");
    }

    public static void generateThroughputPlot(NemsTestRecording recording, int throughputBucketSeconds, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(createThroughputDataSeries(recording, throughputBucketSeconds));

        JFreeChart scatterPlot = ChartFactory.createScatterPlot(
                title,
                "Time since run start (seconds)", // X-Axis Label
                "Throughput (average suspensions per second)", // Y-Axis Label
                dataset
        );

        savePlotAsPngTo(scatterPlot, "build/reports/performance/", "throughput.png");
    }

    private static List<XYSeries> createPerPhaseDataSerieses(NemsTestRecording recording) {
        var phaseCount = 1;
        var testEvents = recording.testEvents();

        var seriesPerPhase = new HashMap<LoadPhase, XYSeries>();

        for (var event : testEvents) {
            var phase = event.phase();
            if (!seriesPerPhase.containsKey(phase)) {
                seriesPerPhase.put(phase, new XYSeries("Phase " + phaseCount++ + ": " + phase));
            }
            var series = seriesPerPhase.get(phase);
            var secondsSinceRunStart = recording.runStartTime().until(event.startedAt(), ChronoUnit.SECONDS);
            series.add(secondsSinceRunStart, event.duration());
        }
        return new ArrayList<>(seriesPerPhase.values());
    }

    private static XYSeries createThroughputDataSeries(NemsTestRecording recording, int throughputBucketSeconds) {
        var series = new XYSeries("Throughput in events per second");
        var timeOrderedTestEvents = recording.testEvents();
        var bucketStartTime = recording.runStartTime();
        var bucketEndTime = bucketStartTime;
        var bucketFinishedCount = 0;
        for (var event : timeOrderedTestEvents) {
            while (event.finishedAt().isAfter(bucketEndTime)) {
                addThroughputToSeries(series, throughputBucketSeconds, bucketEndTime, bucketFinishedCount, recording.runStartTime());
                bucketStartTime = bucketEndTime;
                bucketEndTime = bucketStartTime.plusSeconds(throughputBucketSeconds);
                bucketFinishedCount = 0;
            }
            bucketFinishedCount++;
        }
        addThroughputToSeries(series, throughputBucketSeconds, bucketEndTime, bucketFinishedCount, recording.runStartTime());
        return series;
    }

    private static void addThroughputToSeries(XYSeries series, int throughputBucketSeconds, LocalDateTime bucketEndTime, int bucketFinishedCount, LocalDateTime runStartTime) {
        series.add(runStartTime.until(bucketEndTime, ChronoUnit.SECONDS), (float) bucketFinishedCount / throughputBucketSeconds);
    }

    private static void savePlotAsPngTo(JFreeChart scatterPlot, String dir, String filename) {
        try {
            File outputDir = new File(dir);
            outputDir.mkdirs();
            ChartUtils.saveChartAsPNG(new File(outputDir, filename), scatterPlot, 600, 400);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
