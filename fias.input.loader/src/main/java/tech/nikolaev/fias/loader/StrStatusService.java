package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.StrStatusEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
@Service
public class StrStatusService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(EstStatusService.class);

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    private Map<String, StrStatusEntity> strStatuses = new HashMap<>();

    public Map<String, StrStatusEntity> getStrStatuses() throws DBException {
        if (strStatuses.isEmpty()) {
            List<StrStatusEntity> statuses = esService.getAddressEntityList(StrStatusEntity.class, StrStatusEntity.TYPE);
            statuses.forEach(t -> strStatuses.put(t.getId(), t));
        }
        return strStatuses;
    }

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) {
        try {
            if (r.getLocalName().equals("StructureStatus")) {
                StrStatusEntity strStatusEntity = new StrStatusEntity(r.getAttributeValue(null, "STRSTATID"), r.getAttributeValue(null, "NAME"), r.getAttributeValue(null, "SHORTNAME"));
                strStatuses.put(strStatusEntity.getId(), strStatusEntity);
                logRecordInfo(strStatusEntity, Result.INDEX);
                return new AddressEntityAction(AddressEntityAction.Action.INDEX, strStatusEntity);
            }
        } catch (Exception e) {
            logger.error("Error processing record {}: '{}': {}", StrStatusEntity.TYPE, r.getAttributeValue(null, "STRSTATID"), e.getMessage());
        }
        return null;
    }

    @Override
    protected Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, StrStatusEntity.TYPE);
    }
}
