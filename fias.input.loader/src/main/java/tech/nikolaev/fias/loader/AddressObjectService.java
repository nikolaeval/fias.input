package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
@Scope("prototype")
public class AddressObjectService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(AddressObjectService.class);

    private String level;

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Autowired
    AddressTypeService addressTypeService;

    @Autowired
    private ESService esService;

    @Autowired
	RegionFilterService regionFilterService;

    public AddressObjectService(String level) {
        this.level = level;
    }



    /**
     * Return parent leve of address struct
     * @param parentLevel
     * @return
     * @throws FiasException
     */
    public int getParentLevel(int parentLevel) throws FiasException {
        int level = parentLevel;
        if (level > 10) level = level / 10;
        if (level > 7) {
            throw new FiasException("Incorrect parent level: " + parentLevel);
        }
        --level;
        //ignore deprecate levels
        switch (level) {
            case 2:
            case 5:
                --level;
                break;
        }
        return level;
    }

    /**
     * Return parent fias code of level
     * @param code
     * @param level
     * @return
     */
    public String getParentCode(String code, String level) throws FiasException {
        final String emptyCode = "000000000000000000000";
        final int[] sizeOfCodeLevel = new int[]{-1, 2, -1, 5, 8, -1, 11, 15, 19, 23};
        int parentLevel = getParentLevel(Integer.parseInt(level));
        code = code.substring(0, sizeOfCodeLevel[parentLevel]) + emptyCode;
        return  code.substring(0, parentLevel < 7 ? 11 : 15);
    }

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) throws FiasException {
        AddressObjectEntity addressObject = null;
		try {
            if (null == r || !"Object".equals(r.getLocalName())) {
				return null;
			}
			if (!"1".equals(r.getAttributeValue(null, "ACTSTATUS"))) {
                return null;
            }
            addressObject = AddressObjectEntity.createFromStream(r);

			if (!regionFilterService.checkRegion(addressObject.getRegionCode()) || !level.equals(addressObject.getLevel())) {
				return null;
			}

			if (addressObject.getStatus() == 0) {
				logRecordInfo(addressObject, Result.DELETE);
                return new AddressEntityAction(AddressEntityAction.Action.DELETE, addressObject);
            }

			addressObject.getWords().addAll(Arrays.stream(addressObject.getName().toLowerCase().replace('\u0451', '\u0435')
					.split("(\\s|\\p{Punct})+"))
                    .filter(w -> w.length() > 0)
					.filter(w -> (w.length() > 2 || Character.isDigit(w.charAt(0))))
					.collect(Collectors.toList()));
			String name = addressTypeService.getAddressTypes().get(addressObject.getSocrName()).getName() + " " + addressObject.getName();
			if (null != addressObject.getParentGuid()) {
                AddressObjectEntity parent = esService.getAddress(addressObject.getParentGuid());
                //find parent by code
                if (null == parent ) {
                    // impossible situation. but just in case
                    parent = esService.getAddressByCode(getParentCode(addressObject.getCode(), addressObject.getLevel()));
                }
				if (null != parent) {
					addressObject.setFullName(parent.getFullName() + ", " + name);
					addressObject.getWords().addAll(parent.getWords());
					logRecordInfo(addressObject, Result.INDEX);
				} else {
					logRecordInfo(addressObject, Result.ERROR, "Parent address not found");
					logger.warn("Parent address with id '{}' not found", addressObject.getParentGuid());
					return null;
				}
			} else {
				addressObject.setFullName(name);
				logRecordInfo(addressObject, Result.INDEX);
			}
			return new AddressEntityAction(AddressEntityAction.Action.INDEX, addressObject);
		} catch (DBException e) {
			logger.error("Error processing record {}: '{}': {}", AddressObjectEntity.TYPE, addressObject, e.getMessage());
			throw e;
		}
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
