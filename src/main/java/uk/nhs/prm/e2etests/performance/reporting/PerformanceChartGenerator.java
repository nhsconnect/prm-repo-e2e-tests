package uk.nhs.prm.e2etests.performance.reporting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import uk.nhs.prm.e2etests.performance.NemsTestEvent;
import uk.nhs.prm.e2etests.performance.NemsTestRecording;
import uk.nhs.prm.e2etests.performance.load.LoadPhase;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class PerformanceChartGenerator {
    public static void generateProcessingDurationScatterPlot(NemsTestRecording recording, String title) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (XYSeries series : createPerPhaseDurationSerieses(recording)) {
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

    private static List<XYSeries> createPerPhaseDurationSerieses(NemsTestRecording recording) {
        int phaseCount = 1;
        List<NemsTestEvent> testEvents = recording.startOrderedEvents();

        Map<LoadPhase,XYSeries> seriesPerPhase = new HashMap<>();

        for (NemsTestEvent event : testEvents) {
            LoadPhase phase = event.phase();
            if (!seriesPerPhase.containsKey(phase)) {
                seriesPerPhase.put(phase, new XYSeries("Phase " + phaseCount++ + ": " + phase));
            }
            if (event.getProcessingTimeSeconds() > 0) {
                XYSeries series = seriesPerPhase.get(phase);
                long secondsSinceRunStart = recording.runStartTime().until(event.getStartedAt(), ChronoUnit.SECONDS);
                series.add(secondsSinceRunStart, event.getProcessingTimeSeconds());
            }
        }
        return new ArrayList<>(seriesPerPhase.values());
    }

    private static XYSeries createThroughputDataSeries(NemsTestRecording recording, int throughputBucketSeconds) {
        XYSeries series = new XYSeries("Throughput in events per second");
        List<NemsTestEvent> finishOrderedEvents = recording.finishOrderedEvents();
        LocalDateTime bucketStartTime = recording.runStartTime();
        LocalDateTime bucketEndTime = bucketStartTime;
        int bucketFinishedCount = 0;
        for (NemsTestEvent event : finishOrderedEvents) {
            if (event.isFinished()) {
                while (bucketEndTime.isBefore(event.getFinishedAt())) {
                    addThroughputToSeries(series, throughputBucketSeconds, bucketEndTime, bucketFinishedCount, recording.runStartTime());
                    bucketStartTime = bucketEndTime;
                    bucketEndTime = bucketStartTime.plusSeconds(throughputBucketSeconds);
                    bucketFinishedCount = 0;
                }
                bucketFinishedCount++;
            }
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
