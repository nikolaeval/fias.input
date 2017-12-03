package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.ArchiveFileNotFoundException;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressObjectEntity;
import tech.nikolaev.fias.model.AddressTypeEntity;
import tech.nikolaev.fias.model.PostCodeEntity;
import tech.nikolaev.fias.model.UpdateLogEntity;
import tech.nikolaev.fias.service.dao.ESService;
import tech.nikolaev.fias.exception.FiasException;
import tech.nikolaev.fias.util.FileUtils;
import tech.nikolaev.fias.util.HttpUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
public class AddressLoader {

    private static final Logger logger = LoggerFactory.getLogger(AddressLoader.class);

    private static final String FIRST_FIAS_PUBLIC_DATE = "20161114";
    private static final String FIAS_LAST_VERSION_FORMAT = "dd.MM.yyyy";

    @Value("${max.level}")
    private Integer maxLevel;

    @Value("${fias_xml.url}")
    private String fiasXmlUrlMask;

    @Value("${fias_delta_xml.url}")
    private String fiasDeltaXmlUrlMask;

    @Value("${fias_xml_version.url}")
    private String fiasXmlLastVersionInfoUrl;

    @Autowired
    private ESService esService;

    @Autowired
    private AddressTypeService addressTypeService;

    @Autowired
    private EstStatusService estStatusService;

    @Autowired
    private StrStatusService strStatusService;

    @Autowired
    private HouseService houseService;

    @Autowired
    private PostCodeService postCodeService;

    @Autowired
    private UpdateLogService updateLogService;

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    RegionFilterService regionFilterService;

    public void clearDB () throws DBException {
        dropDB();
        createDB();
    }

    public void dropDB() throws DBException {
            esService.dropDB();
    }

    public void createDB() throws DBException {
        esService.createDB();
    }

    /**
     * Loaded full database on current date
     * @throws IOException
     * @throws FiasException
     */
    public void load() throws IOException, FiasException {
        loadData(getLastFiasVersion());
    }


    public void load(Path fiasXmlPath, LocalDate date) throws FiasException {
        loadData(fiasXmlPath, date);
    }

    /**
     * Update database on current date
     * @throws IOException
     * @throws FiasException
     */
    public void update() throws IOException, FiasException {
        update(getLastFiasVersion());
    }

    /**
     * Update database on current date
     * If database is never been loaded, loaded full database on the date
     * @param date
     * @throws IOException
     * @throws FiasException
     */
    public void update(LocalDate date) throws IOException, FiasException {
        UpdateLogEntity updateLog = updateLogService.getLastSuccessLog();
        if (null == updateLog) {
            loadData(date);
        } else {
            LocalDate nextUpdateDate = getNextFiasVersionDate(UpdateLogEntity.parseDate(updateLog.getDate()));
            while (null != nextUpdateDate && nextUpdateDate.isBefore(date)) {
                updateData(nextUpdateDate);
				nextUpdateDate = getNextFiasVersionDate(nextUpdateDate);
            }
        }
    }

    /**
     * Update database on the date
     * @param date
     * @throws IOException
     * @throws FiasException
     */
    protected void updateData(LocalDate date) throws IOException, FiasException {
        Path fiasDeltaXmlPath = null;
        try {
            String fiasUpdateXmlUrl = String.format(fiasDeltaXmlUrlMask, UpdateLogEntity.formatDate(date));
            fiasDeltaXmlPath = HttpUtils.downloadFileToTemp(fiasUpdateXmlUrl);
            loadData(fiasDeltaXmlPath, date);
        } finally {
            FileUtils.delete(fiasDeltaXmlPath);
        }
    }


    protected LocalDate getLastFiasVersion() throws IOException {
        return LocalDate.parse(new String(HttpUtils.downloadToArray(fiasXmlLastVersionInfoUrl)), DateTimeFormatter.ofPattern(FIAS_LAST_VERSION_FORMAT));
    }

    /**
     * Return last actual data of fias database (the first date is past the parameter).
     * @param date
     * @return
     * @throws IOException
     */
    protected LocalDate getActualFiasVersionDate(LocalDate date) throws IOException {
        final LocalDate first = UpdateLogEntity.parseDate(FIRST_FIAS_PUBLIC_DATE);
        LocalDate current = date;
        while (current.isAfter(first)) {
            String url = String.format(fiasXmlUrlMask, UpdateLogEntity.formatDate(current));
            if (HttpUtils.checkResourceExists(url)) {
                return current;
            }
            current = current.plusDays(-1);
        }
        return null;
    }

    protected LocalDate getNextFiasVersionDate(LocalDate date) throws IOException {
        LocalDate now = LocalDate.now();
        LocalDate current = date.plusDays(1);
        while (!current.isAfter(now)) {
            String url = String.format(fiasXmlUrlMask, UpdateLogEntity.formatDate(current));
            if (HttpUtils.checkResourceExists(url)) {
                return current;
            }
            current = current.plusDays(1);
        }
        return null;
    }


    protected void loadData(LocalDate date) throws IOException, FiasException {
        Path fiasXmlPath = null;
        try {
            date = getActualFiasVersionDate(date);
            String fiasXmlUrl = String.format(fiasXmlUrlMask, UpdateLogEntity.formatDate(date));
            fiasXmlPath = HttpUtils.downloadFileToTemp(fiasXmlUrl);
            loadData(fiasXmlPath, date);
        } finally {
            FileUtils.delete(fiasXmlPath);
        }
    }

    protected void deleteData(AddressBaseFile addressBaseFile) throws FiasException {

        try {
            HouseDeleteService houseDeleteService = beanFactory.getBean(HouseDeleteService.class);
            addressBaseFile.processFile("AS_DEL_HOUSE", houseDeleteService);
        } catch (ArchiveFileNotFoundException e) {
            logger.warn("File 'AS_DEL_HOUSE' not found in archive");
        }

        try {
            AddressObjectDeleteService addressObjectDeleteService = beanFactory.getBean(AddressObjectDeleteService.class);
            addressBaseFile.processFile("AS_DEL_ADDROBJ", addressObjectDeleteService);
        } catch (ArchiveFileNotFoundException e) {
            logger.warn("File 'AS_DEL_ADDROBJ' not found in archive");
        }

    }


    /**
     * Return parent leve of address struct
     * @param parentLevel
     * @return
     * @throws FiasException
     */
    public static int getParentLevel(int parentLevel) throws FiasException {
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
    public static String getParentCode(String code, int level) throws FiasException {
        final int[] sizeOfCodeLevel = new int[]{-1, 2, -1, 5, 8, -1, 11, 15, 19, 23};
        int parentlevel = getParentLevel(level);
        final String emptyCode = "000000000000000000000";
        code = code.substring(0, sizeOfCodeLevel[getParentLevel(level)]) + emptyCode;
        return  code.substring(0, parentlevel < 7 ? 11 : 15);
    }

    /**
     * Загружает полную базу с файловой системы. Дата ее актуальности передается параметром для актуализации лога загрузки в БД
     * @param fiasXmlPath
     * @param date
     * @throws FiasException
     */
    protected void loadData(Path fiasXmlPath, LocalDate date) throws FiasException {

        logger.debug("load data from {} on date {}", fiasXmlPath, date);
        try {

            updateLogService.updateStatus(new UpdateLogEntity(date, UpdateLogEntity.State.RUNNING));
            AddressBaseFile addressBaseFile = beanFactory.getBean(AddressBaseFile.class, fiasXmlPath);

            deleteData(addressBaseFile);

            esService.prepareIndexSettings();

            addressBaseFile.processFile("AS_SOCRBASE", addressTypeService);
            addressBaseFile.processFile("AS_ESTSTAT", estStatusService);
            addressBaseFile.processFile("AS_STRSTAT", strStatusService);

            Set<Integer> levelsSet = new HashSet<>(16);
            addressTypeService.getAddressTypes().values().forEach(addrType -> levelsSet.add(addrType.getLevel()));
            ArrayList<Integer> levels = new ArrayList(levelsSet);
            Collections.sort(levels);
            for (Integer level : levels) {
                if (level > maxLevel.intValue()) {
                    break;
                }
                logger.info("process address level: {}", level);
                AddressObjectService addressObject = beanFactory.getBean(AddressObjectService.class, level);
                addressBaseFile.processFile("AS_ADDROBJ", addressObject);
                esService.flushDB(AddressObjectEntity.TYPE);
            }

            if (regionFilterService.isEnabled()) {
                postCodeService.loadPostcodes();
                esService.flushDB(PostCodeEntity.TYPE);
            }

            addressBaseFile.processFile("AS_HOUSE", houseService);

            esService.restoreIndexSettings();

            updateLogService.updateStatus(new UpdateLogEntity(date, UpdateLogEntity.State.SUCCESS));
        } catch (DBException e) {
            updateLogService.updateStatus(new UpdateLogEntity(date, UpdateLogEntity.State.ERROR));
            throw e;
        }
    }
}
