package tech.nikolaev.fias.loader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.AddressEntityAction;
import tech.nikolaev.fias.model.UpdateLogEntity;
import tech.nikolaev.fias.service.dao.ESService;
import tech.nikolaev.fias.exception.FiasException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.10.2017.
 */
@Service
public class UpdateLogService {

    @Autowired
    private ESService esService;

    public void updateStatus(UpdateLogEntity status) throws FiasException {
        if (null == status.getDate()) {
            throw new FiasException("Empty date for update log");
        }
        Map<String, AddressEntityAction> map = new HashMap<>();
        map.put(status.getId(), new AddressEntityAction(AddressEntityAction.Action.INDEX, status));
        esService.updateEntities(map, status.getType());
    }

    public UpdateLogEntity getLastSuccessLog() throws DBException {
        return esService.getLastSuccesUpdateLog();
    }

}
