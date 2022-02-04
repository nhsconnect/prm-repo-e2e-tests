package uk.nhs.prm.deduction.e2e.performance;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class ScatterPlotGenerator {
    public static void generateProcessingDurationScatterPlot(NemsTestRecording recording) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(createDataSeriesFromRecording(recording));

        JFreeChart scatterPlot = ChartFactory.createScatterPlot(
                "End to End Performance Test - Event durations vs start time",
                "Time since run start (seconds)", // X-Axis Label
                "Processing duration (seconds)", // Y-Axis Label
                dataset
        );

        savePlotAsPngTo(scatterPlot, "build/reports/performance/", "e2e-durations.png");
    }

    private static XYSeries createDataSeriesFromRecording(NemsTestRecording recording) {
        XYSeries startTimeVsDuration = new XYSeries("Event Processing Times");

        var testEvents = recording.testEvents();
        var runStartTime = testEvents.get(0).startedAt();

        for (var event : testEvents) {
            var secondsSinceRunStart = runStartTime.until(event.startedAt(), ChronoUnit.SECONDS);
            startTimeVsDuration.add(secondsSinceRunStart, event.duration());
        }
        return startTimeVsDuration;
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
