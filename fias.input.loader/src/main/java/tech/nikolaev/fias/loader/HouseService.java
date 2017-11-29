package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.AddressTypeEntity;
import tech.nikolaev.fias.model.HouseEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
@Service
public class HouseService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(HouseService.class);

    @Value("${house.bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    @Autowired
    EstStatusService estStatusService;

    @Autowired
    StrStatusService strStatusService;

    @Autowired
    RegionFilterService regionFilterService;

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r){
        HouseEntity house = null;
        try {
            if (r.getLocalName().equals("House")) {
                if (regionFilterService.isEnabled() && !regionFilterService.checkPostcode(r.getAttributeValue(null, "POSTALCODE"))) {
                    return null;
                }
                house = HouseEntity.createFromStream(r);
                house.setName(buildName(house, r.getAttributeValue(null, "ESTSTATUS"), r.getAttributeValue(null, "STRSTATUS")));
                logRecordInfo(house, Result.INDEX);
                return new AddressEntityAction(AddressEntityAction.Action.INDEX, house);
            }
        } catch (Exception e) {
            logger.error("Error processing record {}: '{}': {}", AddressTypeEntity.TYPE, house, e.getMessage());
        }
        return null;
    }

    protected String buildName(HouseEntity house,String estStatus, String structStatus) throws DBException {
        StringBuilder builder = new StringBuilder(32);
        if (!"0".equals(estStatus)) {
            builder.append(estStatusService.getEstStatuses().get(estStatus).getName()).append(" ").append(house.getNum());
            builder.append(house.getBuildNum() != null ? " Корпус " + house.getBuildNum() : "");
        }
        if (!"0".equals(structStatus)) {
            builder.append(builder.length() == 0 ? "" : " ").append(strStatusService.getStrStatuses().get(structStatus).getName()).append(" ").append(house.getStructNum());
        }
        return builder.toString();
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
