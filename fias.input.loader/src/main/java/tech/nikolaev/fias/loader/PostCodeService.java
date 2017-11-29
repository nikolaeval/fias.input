package tech.nikolaev.fias.loader;

import com.linuxense.javadbf.DBFReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.AddressObjectEntity;
import tech.nikolaev.fias.model.PostCodeEntity;
import tech.nikolaev.fias.service.dao.ESService;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.util.FileUtils;
import tech.nikolaev.fias.util.HttpUtils;

import javax.annotation.PostConstruct;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andrey.l.nikolaev@mail.ru on 9/16/17.
 */
@Service
public class PostCodeService extends DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(PostCodeService.class);

    @Value("${postcode_db.url}")
    private String postcodeDBUrl;

    @Value("${bulk.size}")
    private Integer bulkSize;

    @Value("${dbf.charset}")
    private String dbfCharset;

    @Value("${custom_region_codes}")
    private String customRegionCodesStr;

    @Value("${postcodes.file.path}")
    private String postcodesFilePath;

    private Map<String, String> customRegionCodes = new HashMap<>();

    @Autowired
    private ESService esService;

    @Autowired
    private AddressTypeService addressTypeService;

    @Autowired
    private RegionFilterService regionFilterService;

    @PostConstruct
    public void init() {
        if (null != customRegionCodesStr && customRegionCodesStr.length() > 0) {
            customRegionCodes = Arrays.asList(customRegionCodesStr.split(","))
                            .stream()
                            .map(elem -> elem.split(":"))
                            .collect(Collectors.toMap(e -> e[0].toLowerCase(), e -> e[1]));
        }
        logger.debug("custom region codes: {}" , customRegionCodes);
    }



    protected void processPostcodesFile(String postCodesFilePath, Map<String, String> regionNamesFilter) throws FiasException {
        try (InputStream dbfis = new FileInputStream(postCodesFilePath)) {
            DBFReader dbfReader = new DBFReader(dbfis);
            Set<String> unknownRegs = new HashSet<>();
            if (null != dbfCharset) {
                dbfReader.setCharactersetName(dbfCharset);
            }
            Map<String, Integer> fields = new HashMap<>();
            for (int i = 0; i < dbfReader.getFieldCount(); i++) {
                fields.put(dbfReader.getField(i).getName(), i);
            }
            Object[] rec = dbfReader.nextRecord();
            Map<String, AddressEntityAction> objects = new HashMap<>();
            while (null != rec) {
                String regionName = ((String)rec[fields.get("REGION")]).trim().toLowerCase();
                if (regionName.length() > 0) {
                    regionName = regionName.split(" ")[0];
                    String regionCode = regionNamesFilter.get(regionName);
                    if (null == regionCode) {
                        unknownRegs.add(((String)rec[fields.get("REGION")]).trim());
                    } else {
                        PostCodeEntity postcode = new PostCodeEntity((String) rec[fields.get("INDEX")], regionCode, ((String) rec[fields.get("OPSNAME")]).trim());
                        objects.put(postcode.getId(), new AddressEntityAction(AddressEntityAction.Action.INDEX, postcode));
                    }
                    if (objects.size() == getBulkSize()) {
                        updateDB(objects, PostCodeEntity.TYPE);
                        objects.clear();
                    }
                }
                rec = dbfReader.nextRecord();
            }
            if (!unknownRegs.isEmpty()) {
                logger.warn("Failed to identify regions: {}", unknownRegs);
            }
            if (objects.size() > 0 ) {
                updateDB(objects, PostCodeEntity.TYPE);
            }
        } catch (IOException dbf) {
            throw new DBException(dbf);
        }

    }

    public void loadPostcodes() throws FiasException {
        if (null != postcodesFilePath && postcodesFilePath.trim().length() > 0) {
            loadPostcodesFile(Paths.get(postcodesFilePath));
        } else {
            loadPostcodesFile();
        }
    }

    protected void loadPostcodesFile() throws FiasException {
        Path postcodesArcFilePath = null;
        try {
            postcodesArcFilePath = HttpUtils.downloadFileToTemp(postcodeDBUrl);
            loadPostcodesFile(HttpUtils.downloadFileToTemp(postcodeDBUrl));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new FiasException(e.getMessage(), e);
        } finally {
            FileUtils.delete(postcodesArcFilePath);
        }
    }

    protected void loadPostcodesFile(Path postcodesArcFilePath) throws FiasException {

        Path tempDirFile = null;
        try {
            Map<String, String> regionNames = new HashMap<>();
            List<AddressObjectEntity> regions = esService.getRegionList();

            regions.forEach(r -> regionNames.put(r.getName().split(" ")[0].toLowerCase(), r.getRegionCode()));
            regionNames.putAll(customRegionCodes);

            Map<String, String> regionNamesFilter = regionNames.entrySet().stream()
                    .filter(map -> regionFilterService.checkRegion(map.getValue()))
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

            tempDirFile = Files.createTempDirectory("postcodes");
            List<String> fileList = FileUtils.unzipToDir(postcodesArcFilePath, tempDirFile);
            if (fileList.size() != 1) {
                throw new FiasException("Unknown postcodes file '" +  postcodesArcFilePath.getFileName() + "' only one file should be in archive");
            }
            processPostcodesFile(tempDirFile.toAbsolutePath() + File.separator + fileList.get(0), regionNamesFilter);

        } catch (DBException | IOException e) {
            logger.error(e.getMessage(), e);
            throw new FiasException(e.getMessage(), e);
        } finally {
            FileUtils.delete(tempDirFile);
        }
    }

    @Override
    protected AddressEntityAction processAttributes(XMLStreamReader r) {
        throw new UnsupportedOperationException("UnsupportedOperation");
    }

    @Override
    protected Integer getBulkSize() {
        return bulkSize;
    }

    @Override
    protected void updateDB(Map<String, AddressEntityAction> object, String type) throws DBException {
        esService.updateEntities(object, PostCodeEntity.TYPE);
    }
}
