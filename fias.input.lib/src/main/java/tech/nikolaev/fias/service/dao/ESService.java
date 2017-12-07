package tech.nikolaev.fias.service.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import tech.nikolaev.fias.exception.DBException;
import tech.nikolaev.fias.model.*;
import tech.nikolaev.fias.service.DBService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Service
public class ESService implements DBService {

    private static final Logger logger = LoggerFactory.getLogger(ESService.class);
    public static final String INDEX = "fiasdb";
    public static final int MAX_SELECT_RECORDS = 10000;
    private static final int ES_SOCKET_TIMEOUT_MILLIS = 60000;
    private static final String MAPPING_ENDPOINT = INDEX + "/_mapping/";
    private static final String SETTINGS_ENDPOINT = INDEX + "/_settings/";

    @Value("${es.url}")
    private String urlStr;

    private HttpHost[] esHosts;

    private RestClient restClient;

    private Map<String, String> scriptCache = new HashMap<String, String>();

    @PostConstruct
    protected void init() {
        restClient = RestClient.builder(getESHosts()).setRequestConfigCallback(b -> b.setSocketTimeout(ES_SOCKET_TIMEOUT_MILLIS)).build();
    }

    @PreDestroy
    protected void destroy() {
        try {
            restClient.close();
        } catch (Exception e) {
            logger.error("Error destroy restClient: {}", e.getMessage(), e);

        }
    }

    private static String getSearchUrl(String objectType) {
        return INDEX + "/" + objectType + "/_search";
    }

    protected HttpHost[] getESHosts() {
        if (null == esHosts) {
            esHosts = Arrays.stream(urlStr.split(",")).map(w -> toHttpHost(w)).filter(h -> null != h).toArray(HttpHost[]::new);
        }
        return esHosts;
    }

    private static HttpHost toHttpHost(String urlStr) {
        try {
            URL url = new URL(urlStr);
            return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        } catch (MalformedURLException e) {
            logger.warn("Error parsing url '{}': {}", urlStr, e.getMessage());
        }
        return null;
    }


    @Override
    public void dropDB() throws DBException {
        try {
            if (checkIndexExists()) {
                deleteIndex();
            }
        } catch (IOException e) {
            throw new DBException(e);
        }
    }

    @Override
    public void createDB() throws DBException {
        try {
            if (checkIndexExists()) {
                logger.info("Index '{}' already exists", INDEX);
            } else {
                executeScriptResource("/es/CreateIndex.json", "PUT", INDEX);
            }
            executeScriptResource("/es/UpdateLogMapping.json", "PUT", MAPPING_ENDPOINT + UpdateLogEntity.TYPE);
            executeScriptResource("/es/AddressTypeMapping.json", "PUT", MAPPING_ENDPOINT + AddressTypeEntity.TYPE);
            executeScriptResource("/es/AddressMapping.json", "PUT", MAPPING_ENDPOINT + AddressObjectEntity.TYPE);
            executeScriptResource("/es/EstStatusMapping.json", "PUT", MAPPING_ENDPOINT + EstStatusEntity.TYPE);
            executeScriptResource("/es/StrStatusMapping.json", "PUT", MAPPING_ENDPOINT + StrStatusEntity.TYPE);
            executeScriptResource("/es/PostcodeMapping.json", "PUT", MAPPING_ENDPOINT + PostCodeEntity.TYPE);
            executeScriptResource("/es/HouseMapping.json", "PUT", MAPPING_ENDPOINT + HouseEntity.TYPE);
        } catch (IOException e) {
            throw new DBException(e);
        }
    }


     public void prepareIndexSettings() throws DBException {
        try {
            executeScriptResource("/es/ResetRefreshInterval.json", "PUT", SETTINGS_ENDPOINT);
        } catch (IOException e) {
            throw new DBException(e);
        }
     }

    public void restoreIndexSettings() throws DBException {
        try {
            executeScriptResource("/es/RestoreRefreshInterval.json", "PUT", SETTINGS_ENDPOINT);
        } catch (IOException e) {
            throw new DBException(e);
        }
    }


    protected boolean checkIndexExists() throws IOException {
        try {
            restClient.performRequest("GET", SETTINGS_ENDPOINT);
            return true;
        } catch (ResponseException re) {
			if (re.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return false;
			}
			throw re;
        }
    }

    protected void deleteIndex() throws IOException {
        try {
            logger.info("Delete index '{}'", INDEX);
            restClient.performRequest("DELETE", "/"+ INDEX);
            logger.info("Index '{}' deleted successfully", INDEX);
        } catch (IOException ioe) {
            logger.error("Error deleted index '{}': {}", INDEX, ioe.getMessage(), ioe);
            throw ioe;
        }
    }

    protected String getScriptResource(String resource)  throws IOException {
        String script = scriptCache.get(resource);
        if (null != script) {
            return script;
        }
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            script = StreamUtils.copyToString(is, Charset.defaultCharset());
            scriptCache.put(resource, script);
            return script;
        } catch (IOException ioe) {
            logger.error("Error copy from resource: '{}'", resource, ioe);
            throw ioe;
        }
    }

    public Response executeScriptResource(String scriptResource, String method, String endpoint) throws IOException {
        return executeScript(getScriptResource(scriptResource), method, endpoint);
    }

    public Response executeScript(String script, String method, String endpoint) throws IOException {
        try {
            logger.debug("Execute Script: \n{}", script);
            Response response = restClient.performRequest(method, endpoint, Collections.<String, String>emptyMap(), new NStringEntity(script, ContentType.APPLICATION_JSON));
            logger.debug("Execute Script id ok");
            return response;
        } catch (IOException ioe) {
            logger.error("Error execute script: {}", ioe.getMessage(), ioe);
            throw ioe;
        }
    }

    public void updateEntities(Map<String, AddressEntityAction> objects, String type) throws DBException {
        try {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, AddressEntityAction> entry : objects.entrySet()) {
                String actionMetaData = String.format(Locale.ROOT, "{ \"%s\" : { \"_index\" : \"%s\", \"_type\" : \"%s\", \"_id\" : \"%s\" } }%n", entry.getValue().getAction(), INDEX, type, entry.getKey());
				builder.append(actionMetaData);
				if (AddressEntityAction.Action.DELETE != entry.getValue().getAction()) {
					builder.append(new Gson().toJson(entry.getValue().getEntity())).append('\n');
				}
            }
			String request = builder.toString();
            HttpEntity entity = new NStringEntity(request, ContentType.APPLICATION_JSON);
            restClient.performRequest("POST", "/_bulk", Collections.emptyMap(), entity);
            logger.trace("update index '{}': {} recs", type, objects.size());
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    public AddressObjectEntity getAddress(String guid) throws DBException {
        try {
            Response response = restClient.performRequest("GET", INDEX + "/" + AddressObjectEntity.TYPE + "/" + guid + "/_source");
            return new Gson().fromJson(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8), AddressObjectEntity.class);
        } catch (ResponseException re) {
            if (re.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            logger.error("Error get record '{}': {}", guid, re.getMessage());
            throw new DBException(re);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    public AddressObjectEntity getAddressByCode(String code) throws DBException {
        try {
            String request = String.format(getScriptResource("/es/SearchByCode.json"), code);
            logger.debug("\n{}", request);
            Response response =  restClient.performRequest("GET", getSearchUrl(AddressObjectEntity.TYPE), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
            List<AddressObjectEntity> result = parseResult(response, AddressObjectEntity.class);
            if (result.isEmpty()) {
                return null;
            }
            if (result.size() > 1) {
                throw new DBException("Non unique result for " + code);
            }
            return result.get(0);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    public <T> List<T> getAddressEntityList(Class<T> clazz, String type)  throws DBException {
        return getAddressEntityList(clazz,  type, 0, MAX_SELECT_RECORDS);
    }

    public <T> List<T> getAddressEntityList(Class<T> clazz, String type, Integer from, Integer size)  throws DBException {
        try {
            String request = String.format(getScriptResource("/es/SearchAll.json"), from, size);
            logger.debug("\n{}", request);
            Response response =  restClient.performRequest("GET", getSearchUrl(type), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
			return parseResult(response, clazz);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    public List<AddressObjectEntity> getRegionList() throws DBException {
        try {
            Response response = executeScriptResource("/es/GetRegionList.json", "GET", getSearchUrl(AddressObjectEntity.TYPE));
            return parseResult(response, AddressObjectEntity.class);
        } catch (IOException e) {
            throw new DBException(e);
        }
    }

    public Set<String> getAddressTypeList() throws DBException {
        List<AddressTypeEntity> list = getAddressEntityList(AddressTypeEntity.class, AddressTypeEntity.TYPE);
        HashSet<String> result = new HashSet<>(list.size());
        list.forEach(a -> result.add(a.getName()));
        return result;
    }

    public List<AddressObjectEntity> searchAddress(List<String> keywords) throws DBException {
        try {
            StringBuilder match = new StringBuilder();
            keywords.forEach(k -> match.append(",{\"fuzzy\":{\"words\":\"").append(k).append("\"}}"));
            String request = String.format(getScriptResource("/es/SearchAddress.json"), match.substring(1));
            logger.trace("\n{}", request);
            Response response =  restClient.performRequest("GET", getSearchUrl(AddressObjectEntity.TYPE), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
			return parseResult(response, AddressObjectEntity.class);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    public List<HouseEntity> searchHouse(String parentGuid) throws DBException {
        try {
            String request = String.format(getScriptResource("/es/SearchByParent.json"), parentGuid);
            logger.debug("\n{}", request);
            Response response =  restClient.performRequest("GET", getSearchUrl(HouseEntity.TYPE), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
			return parseResult(response, HouseEntity.class);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    /**
     * Used for retrive postcodes of region (if used filter for load) to filter house loading
     * @param regionCode region code
     * @return postcodes list
     */
    public Set<String> getRegionPostcodes(String regionCode) throws DBException {
        Set<String> result = new HashSet<>();
        try {
            String request = String.format(getScriptResource("/es/SearchRegionPostcodes.json"), regionCode);
            logger.debug("\n{}", request);
            Response response =  restClient.performRequest("GET", getSearchUrl(PostCodeEntity.TYPE), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
			List<PostCodeEntity> postcodes = parseResult(response, PostCodeEntity.class);
			postcodes.forEach(p -> result.add(p.getCode()));
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
        return result;
    }

    public UpdateLogEntity getLastSuccesUpdateLog() throws DBException {
        try {
            String request = getScriptResource("/es/SearchLastSuccessUpdateLog.json");
            logger.debug("\n{}", request);
            Response response = restClient.performRequest("GET", getSearchUrl(UpdateLogEntity.TYPE), Collections.<String, String>emptyMap(), new NStringEntity(request, ContentType.APPLICATION_JSON));
			List<UpdateLogEntity> result = parseResult(response, UpdateLogEntity.class);
			return result.isEmpty() ? null : result.get(0);

        } catch (ResponseException re) {
            if (re.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
		} catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
        return null;
    }


    public void flushDB() throws DBException {
        flushDB(null);
    }

    /**
     * Flush ES
     * @param index to flush. Null to flush all indexes
     * @throws IOException
     */
    @Override
    public void flushDB(String index) throws DBException {
        try {
            restClient.performRequest("POST", (null == index ? "" : INDEX) + "/_flush/synced");
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new DBException(ioe);
        }
    }

    protected <T> List<T> parseResult(Response response, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        JsonElement element = new JsonParser().parse(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
        JsonArray resultList = element.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
        for (int i=0; i<resultList.size(); i++) {
            result.add(new Gson().fromJson(resultList.get(i).getAsJsonObject().get("_source"), clazz));
        }
        return result;
    }

}
