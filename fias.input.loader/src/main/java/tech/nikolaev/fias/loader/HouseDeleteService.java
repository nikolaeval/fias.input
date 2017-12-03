package tech.nikolaev.fias.loader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.HouseEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
@Service
public class HouseDeleteService extends DataLoader {

    @Value("${house.bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    @Autowired
    RegionFilterService regionFilterService;

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) throws FiasException {
        if (null == r || !"House".equals(r.getLocalName())) {
            return null;
        }
        HouseEntity house = HouseEntity.createFromStream(r);
        logRecordInfo(house, Result.DELETE);
        return new AddressEntityAction(AddressEntityAction.Action.DELETE, house);
    }

    @Override
    protected Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, HouseEntity.TYPE);
    }
}
