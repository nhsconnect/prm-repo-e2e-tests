package uk.nhs.prm.e2etests.repository;

import io.ebean.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import io.ebean.Database;
import uk.nhs.prm.e2etests.exception.NotFoundException;
import uk.nhs.prm.e2etests.model.database.Acknowledgement;

import java.util.Optional;
import java.util.UUID;

@Repository
public class EhrOutDatabaseAcknowledgeRepository {
    private final Database database;

    @Autowired
    public EhrOutDatabaseAcknowledgeRepository(
            Database ehrOutDatabase
    ) {
        this.database = ehrOutDatabase;
    }

    public Acknowledgement findAcknowledgementById(UUID id) {
        return Optional
                .ofNullable(this.database.find(Acknowledgement.class, id))
                .orElseThrow(() -> new NotFoundException(id.toString()));
    }
}
