package uk.nhs.prm.e2etests.performance.reporting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import uk.nhs.prm.e2etests.performance.RepoInPerfMessageWrapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RepoInPerformanceChartGenerator {
    public static void generateThroughputPlot(List<RepoInPerfMessageWrapper> messagesProcessed) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(createThroughputDataSeries(messagesProcessed));

        JFreeChart scatterPlot = ChartFactory.createScatterPlot(
                "Repo-in throughput chart",
                "Time since run start (seconds)", // X-Axis Label
                "Throughput (average transfer competed per second)", // Y-Axis Label
                dataset
        );

        savePlotAsPngTo(scatterPlot, "build/reports/performance/", "repo-in-throughput.png");
    }

    private static XYSeries createThroughputDataSeries(List<RepoInPerfMessageWrapper> messagesProcessed) {
        var throughputBucketSeconds = 1;
        var series = new XYSeries("Throughput in events per second");
        var processingStartTime = messagesProcessed.get(0).getStartedAt();
        var bucketStartTime = processingStartTime;
        var bucketEndTime = bucketStartTime;
        var bucketFinishedCount = 0;
        for (var message : messagesProcessed) {
            while (bucketEndTime.isBefore(message.getFinishedAt())) {
                addThroughputToSeries(series, throughputBucketSeconds, bucketEndTime, bucketFinishedCount, processingStartTime);
                bucketStartTime = bucketEndTime;
                bucketEndTime = bucketStartTime.plusSeconds(throughputBucketSeconds);
                bucketFinishedCount = 0;
            }
            bucketFinishedCount++;
        }
        addThroughputToSeries(series, throughputBucketSeconds, bucketEndTime, bucketFinishedCount, processingStartTime);
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
