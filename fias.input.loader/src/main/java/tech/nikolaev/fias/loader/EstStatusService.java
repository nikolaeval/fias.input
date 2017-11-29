package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.EstStatusEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
@Service
public class EstStatusService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(EstStatusService.class);

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    private Map<String, EstStatusEntity> estStatuses = new HashMap<>();

    public Map<String, EstStatusEntity> getEstStatuses() throws DBException {
        if (estStatuses.isEmpty()) {
            List<EstStatusEntity> statuses = esService.getAddressEntityList(EstStatusEntity.class, EstStatusEntity.TYPE);
            statuses.forEach(t -> estStatuses.put(t.getId(), t));
        }
        return estStatuses;
    }

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) {
        try {
            if (r.getLocalName().equals("EstateStatus")) {
                EstStatusEntity estStatusEntity = new EstStatusEntity(r.getAttributeValue(null, "ESTSTATID"), r.getAttributeValue(null, "NAME"));
                estStatuses.put(estStatusEntity.getId(), estStatusEntity);
                logRecordInfo(estStatusEntity, Result.INDEX);
                return new AddressEntityAction(AddressEntityAction.Action.INDEX, estStatusEntity);
            }
        } catch (Exception e) {
            logger.error("Error processing record {}: '{}': {}", EstStatusEntity.TYPE, r.getAttributeValue(null, "ESTSTATID"), e.getMessage());
        }
        return null;
    }

    @Override
    protected Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, EstStatusEntity.TYPE);
    }
}
