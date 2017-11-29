package tech.nikolaev.fias.service;

import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.AddressObjectEntity;
import tech.nikolaev.fias.model.UpdateLogEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by andrey.l.nikolaev@mail.ru on 25.09.2017.
 */
public interface DBService {

    void dropDB() throws DBException;
    void createDB() throws DBException;
    List<AddressObjectEntity> searchAddress(List<String> keywords) throws DBException;
    <T> List<T> getAddressEntityList(Class<T> clazz, String type)  throws DBException;
    <T> List<T> getAddressEntityList(Class<T> clazz, String type, Integer from, Integer size)  throws DBException;
    AddressObjectEntity getAddress(String guid) throws DBException;
    void updateEntities(Map<String, AddressEntityAction> objects, String type) throws DBException;

    List<AddressObjectEntity> getRegionList() throws DBException;
    Set<String> getRegionPostcodes(String regionCode) throws DBException;
    UpdateLogEntity getLastSuccesUpdateLog() throws DBException;
    void flushDB(String index) throws DBException;

}
