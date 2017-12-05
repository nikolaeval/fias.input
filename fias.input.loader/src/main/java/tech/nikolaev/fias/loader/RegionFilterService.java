package tech.nikolaev.fias.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.service.dao.ESService;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by andrey.l.nikolaev@mail.ru on 26.09.2017.
 */
@Service
public class RegionFilterService {

    private static final Logger logger = LoggerFactory.getLogger(RegionFilterService.class);

    @Value("${region.filter}")
    private String regionFilterStr;

    private Set<String> regionFilter;
    private Set<String> postcodesFilter;

    @Autowired
    private ESService esService;

    @PostConstruct
    public void init() {
        if (null != regionFilterStr) {
            regionFilter = new HashSet<>();
            Arrays.stream(regionFilterStr.split(",")).filter(w -> w.length() > 0).forEach(r -> regionFilter.add(r.trim()));
            regionFilter = regionFilter.isEmpty() ? null : regionFilter;
        }
        logger.info("region filter: {}" , regionFilter);
    }

    public boolean isEnabled() {
        return regionFilter != null;
    }

    public boolean checkRegion(String regionCode) {
        return regionFilter == null || regionFilter.contains(regionCode);
    }

    public boolean checkPostcode(String postcode) throws DBException {
        if (regionFilter == null) {
            return true;
        }
        if (postcodesFilter == null ) {
            postcodesFilter = new HashSet<>();
            for (String regionCode : regionFilter) {
                Set<String> postcodes = esService.getRegionPostcodes(regionCode);
                if (postcodes.isEmpty()) {
                    logger.warn("Not found postcodes for '{}' region", regionCode);
                } else {
                    postcodesFilter.addAll(postcodes);
                }
            }
            logger.info("postcodes filter: {}", postcodesFilter);
        }
        return postcodesFilter.contains(postcode);
    }

    public void addToPostcodesFilter(String postcode) {
        if (null == postcodesFilter) {
            postcodesFilter = new HashSet<>();
        }
        postcodesFilter.add(postcode);
    }

}