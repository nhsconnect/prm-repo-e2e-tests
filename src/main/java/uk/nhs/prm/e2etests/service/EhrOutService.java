package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.model.database.Acknowledgement;
import uk.nhs.prm.e2etests.repository.EhrOutDatabaseAcknowledgementRepository;

import java.util.UUID;

@Service
@Log4j2
public class EhrOutService {

    private EhrOutDatabaseAcknowledgementRepository ehrOutDatabaseAcknowledgementRepository;

    @Autowired
    public EhrOutService(EhrOutDatabaseAcknowledgementRepository ehrOutDatabaseAcknowledgementRepository) {
        this.ehrOutDatabaseAcknowledgementRepository = ehrOutDatabaseAcknowledgementRepository;
    }

    public Acknowledgement findAcknowledgementByMessageId(UUID messageId) {
        return ehrOutDatabaseAcknowledgementRepository.findByMessageId(messageId);
    }
}
