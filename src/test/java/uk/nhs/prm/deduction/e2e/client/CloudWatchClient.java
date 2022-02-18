package uk.nhs.prm.deduction.e2e.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;


@Component
public class CloudWatchClient {

    TestConfiguration configuration;

    @Autowired
    public CloudWatchClient(TestConfiguration configuration) {
        this.configuration = configuration;
    }

    public void getLogs() {

        String logGroupName = configuration.getNemsEventProcessorLogGroup();
        CloudWatchLogsClient cloudWatchLogsClient = CloudWatchLogsClient.builder()
                .region(Region.EU_WEST_2)
                .build();
        var logEvents = findLogsContainingTenDigitNumbers(cloudWatchLogsClient, logGroupName, 1440);
        System.out.println("Number of log events containing PII like information is : " + logEvents.size());
        cloudWatchLogsClient.close();
    }

    public List<List<ResultField>> findLogsContainingTenDigitNumbers(CloudWatchLogsClient client, String logGroupName, int minutes) {
        var insightsQueryRequest = StartQueryRequest.builder()
                .logGroupName(logGroupName)
                .startTime(LocalDateTime.now().minusMinutes(minutes).toEpochSecond(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .queryString("fields @timestamp, service, message, @message\n" +
                        "\n" +
                        "| sort @timestamp desc\n" +
                        "\n" +
                        "| filter message like /\\D+\\d{10}\\D+/")
                .build();
        var startQueryResponse = client.startQuery(insightsQueryRequest);
        var resultsRequest = GetQueryResultsRequest.builder().queryId(startQueryResponse.queryId()).build();
        var queryResult = client.getQueryResults(resultsRequest);
        while (queryResult.status() != QueryStatus.COMPLETE) {
            System.out.println(queryResult.status());
            sleep(1000);
            queryResult = client.getQueryResults(resultsRequest);
        }
        return queryResult.results();
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
