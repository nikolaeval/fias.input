package tech.nikolaev.fias.loader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.AddressObjectEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.xml.stream.XMLStreamReader;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
@Scope("prototype")
public class AddressObjectDeleteService extends DataLoader {

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Autowired
    private ESService esService;

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) throws FiasException {
        if (null == r || !"Object".equals(r.getLocalName())) {
            return null;
        }
        AddressObjectEntity addressObject = AddressObjectEntity.createFromStream(r);
        logRecordInfo(addressObject, Result.DELETE);
        return new AddressEntityAction(AddressEntityAction.Action.DELETE, addressObject);
    }

    @Override
    public Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, AddressObjectEntity.TYPE);
    }

}
