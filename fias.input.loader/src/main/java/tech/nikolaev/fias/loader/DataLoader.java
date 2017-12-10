package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.exception.FiasRuntimeException;
import tech.nikolaev.fias.model.AddressEntity;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.util.FileUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
public abstract class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    public static final Logger recordsLogger = LoggerFactory.getLogger("records");

    public enum Result {
        INDEX,
        DELETE,
        ERROR;
    }

    public void logRecordInfo(AddressEntity addressObject, Result action) {
        logRecordInfo(addressObject, action, "");
    }

    public void logRecordInfo(AddressEntity addressObject, Result action, String description) {
        recordsLogger.info("{};{};{};{};{};{}", action, addressObject.getType(), addressObject.getId(), addressObject.getCode(), addressObject.getName(), description);
    }

    public void loadData(InputStream is) {
        XMLStreamReader r = null;
        int n = 0;
        int c = 0;
        long time = System.currentTimeMillis();
        try  {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            r = factory.createXMLStreamReader(is);
            int event = r.getEventType();
            AddressEntityAction o = null;
            Map<String, AddressEntityAction> objects = new HashMap<>();
            while (true) {
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        ++n;
                        o = processAttributes(r);
                        if (null == o) {
                            break;
                        }
                        ++c;
                        objects.put(o.getEntity().getId(), o);
                        if (objects.size() == getBulkSize()) {
                            updateDB(objects, o.getEntity().getType());
                            objects.clear();
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        break;
                    default:
                        break;
                }
                if (!r.hasNext()) {
                    if (objects.size() > 0 ) {
                        updateDB(objects, objects.values().iterator().next().getEntity().getType());
                    }
                    break;
                }
                event = r.next();
            }
            time = System.currentTimeMillis() - time;
            logger.info("processed {} recs in {}ms {} rec in sec ", n, time, (n * 1000L / time));
            logger.info("updated {} recs ", c);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            FileUtils.close(is);
            throw new FiasRuntimeException(e);
        }
    }

    protected abstract AddressEntityAction processAttributes(XMLStreamReader r) throws FiasException;
    protected abstract Integer getBulkSize();
    protected abstract void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException;
}
