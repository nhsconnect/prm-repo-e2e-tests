package uk.nhs.prm.deduction.e2e.tests;


import uk.nhs.prm.deduction.e2e.mesh.MeshClient;
import org.apache.http.HttpException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MeshClient.class)
class MeshClientTest {

    @Test
    public void testWeCanConnectToMeshMailbox() throws HttpException {

        MeshClient meshClient = new MeshClient();
        meshClient.postMessageToMeshMailbox();
    }
}