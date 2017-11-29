package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.AddressTypeEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.*;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
public class AddressTypeService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(AddressTypeService.class);

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    private Map<String, AddressTypeEntity> addressTypes = new HashMap<>();
    private Set<String> names = new HashSet<>();

    public Map<String, AddressTypeEntity> getAddressTypes() throws DBException {
        if (addressTypes.isEmpty()) {
            List<AddressTypeEntity> types = esService.getAddressEntityList(AddressTypeEntity.class, AddressTypeEntity.TYPE);
            types.forEach(t -> addressTypes.put(t.getId(), t));
        }
        return addressTypes;
    }

    public Set<String> getNames() throws DBException {
        if (names.isEmpty()) {
            getAddressTypes().values().forEach(t -> names.add(t.getName().toLowerCase()));
        }
        return names;
    }

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) {
        try {
            if (r.getLocalName().equals("AddressObjectType") && r.getAttributeValue(null, "SOCRNAME").length() > 0) {
                AddressTypeEntity addressTypeEntity = new AddressTypeEntity(r.getAttributeValue(null, "SCNAME"), r.getAttributeValue(null, "SOCRNAME"), Integer.parseInt(r.getAttributeValue(null, "LEVEL")));
                addressTypes.put(addressTypeEntity.getId(), addressTypeEntity);
                logRecordInfo(addressTypeEntity, Result.INDEX);
                return new AddressEntityAction(AddressEntityAction.Action.INDEX, addressTypeEntity);
            }
        } catch (Exception e) {
            logger.error("Error processing record {}: '{}': {}", AddressTypeEntity.TYPE, r.getAttributeValue(null, "SCNAME"), e.getMessage());
        }
        return null;
    }

    @Override
    protected Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, AddressTypeEntity.TYPE);
    }
}
