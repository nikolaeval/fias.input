package tech.nikolaev.fias.input.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressObjectEntity;
import tech.nikolaev.fias.model.HouseEntity;
import tech.nikolaev.fias.service.dao.ESService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andrey.l.nikolaev@mail.ru on 25.09.2017.
 */
@RestController
public class AddressInputController {

    private static final Logger logger = LoggerFactory.getLogger(AddressInputController.class);

    @Autowired
    private ESService searchService;

    private Set<String> addressTypes = new HashSet<>();


    @PostConstruct
    public void init() {
        try {
            searchService.getAddressTypeList().forEach(w -> addressTypes.add(w.toLowerCase()));
			logger.info("addressTypes: {}", addressTypes);
        } catch (DBException e) {
            logger.error("Error init: {}", e.getMessage(), e);
        }
    }


    @RequestMapping(value = "/searchAddress", method = RequestMethod.GET)
    public ResponseEntity<List<AddressObjectEntity>> searchAddress(@RequestParam(value = "address") String address) {
        List<String> words = new ArrayList<>();
        words.addAll(Arrays.stream(address.toLowerCase()
                .replace('\u0451', '\u0435')
                .split("(\\s|\\p{Punct})+"))
                .filter(w -> !addressTypes.contains(w))
                .collect(Collectors.toList()));
		logger.info("search: {}", words);
        try {
            return new ResponseEntity<List<AddressObjectEntity>>(searchService.searchAddress(words), HttpStatus.OK);
        } catch (DBException e) {
            logger.error("Error request with address '{}': {}", address, e.getMessage(), e);
            return new ResponseEntity<List<AddressObjectEntity>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/searchHouse", method = RequestMethod.GET)
    public ResponseEntity<List<HouseEntity>> searchHouse(@RequestParam(value = "aoguid") String parentGuid) {
        try {
            return new ResponseEntity<List<HouseEntity>>(searchService.searchHouse(parentGuid), HttpStatus.OK);
        } catch (DBException e) {
            logger.error("Error request house with aoguid '{}': {}", parentGuid, e.getMessage(), e);
            return new ResponseEntity<List<HouseEntity>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
