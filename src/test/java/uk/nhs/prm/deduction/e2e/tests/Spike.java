package uk.nhs.prm.deduction.e2e.tests;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.ActiveMqClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;


@SpringBootTest(classes = {
        Spike.class,
        DbClient.class,
        TestConfiguration.class

})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Spike {

    @Autowired
    DbClient dbClient;

    @Test
    void playingAroundWithDbQueries(){
        dbClient.runDbScan();
    }
}
