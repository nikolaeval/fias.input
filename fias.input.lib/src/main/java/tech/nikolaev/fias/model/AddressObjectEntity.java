package tech.nikolaev.fias.model;

import javax.xml.stream.XMLStreamReader;
import java.util.*;

/**
 * Created by andrey.l.nikolaev@mail.ru on 18.08.2017.
 */
public class AddressObjectEntity implements AddressEntity {


    public static final String TYPE = "address";

    public enum Attributes {
        AOGUID,
		PARENTGUID,
        PLAINCODE,
        OFFNAME,
        SHORTNAME,
        AOLEVEL,
        LIVESTATUS;
    }

    private String guid;
    private String parentGuid;
    private String code;
    private String name;
    private String socrName;
    private String fullName;
    private String regionCode;
    private int level;
    private int status;
    private Set<String> words;

    public AddressObjectEntity(String guid, String parentGuid, String code, String name, String socrName, int level, int status) {
        this.guid = guid;
        this.parentGuid = parentGuid;
        this.code = code;
        this.name = name;
        this.socrName = socrName;
        this.regionCode = null == code ? null : code.substring(0, 2);
        this.level = level;
        this.status = status;
        this.words = new HashSet<>();
    }

    public static AddressObjectEntity createFromStream(XMLStreamReader r) {
        return new AddressObjectEntity(
            r.getAttributeValue(null, Attributes.AOGUID.name()),
            r.getAttributeValue(null, Attributes.PARENTGUID.name()),
            r.getAttributeValue(null, Attributes.PLAINCODE.name()),
            r.getAttributeValue(null, Attributes.OFFNAME.name()),
            r.getAttributeValue(null, Attributes.SHORTNAME.name()),
            Integer.parseInt(r.getAttributeValue(null, Attributes.AOLEVEL.name())),
            Integer.parseInt(r.getAttributeValue(null, Attributes.LIVESTATUS.name())));
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSocrName() {
        return socrName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGuid() {
        return guid;
    }

    public String getParentGuid() {
        return parentGuid;
    }

    public Set<String> getWords() {
        return words;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public int getLevel() {
        return level;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getId() {
        return getGuid();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "AddressObjectEntity{" +
                "guid='" + guid + '\'' +
                ", code='" + code + '\'' +
                ", level=" + level +
                ", status=" + status +
                ", fullName='" + fullName + '\'' +
                '}';
    }

}
