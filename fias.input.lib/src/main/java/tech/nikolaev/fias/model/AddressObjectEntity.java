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
        FORMALNAME,
        SHORTNAME,
        AOLEVEL,
        LIVESTATUS,
        REGIONCODE,
		AREACODE,
		CITYCODE,
        CTARCODE,
		PLACECODE,
        PLANCODE,
		STREETCODE,
        EXTRCODE;
    }

    private String guid;
    private String parentGuid;
    private String code;
    private String name;
    private String socrName;
    private String fullName;
    private String regionCode;
    private String level;
    private int status;
    private Set<String> words;

    public AddressObjectEntity(String guid, String parentGuid, String code, String name, String socrName, String level, int status) {
        this.guid = guid;
        this.parentGuid = parentGuid;
        this.code = code;
        this.name = name;
        this.socrName = socrName;
        this.regionCode = code.substring(0, 2);
        this.level = level;
        this.status = status;
        this.words = new HashSet<>();
    }

    public static AddressObjectEntity createFromStream(XMLStreamReader r) {
        return new AddressObjectEntity(
            r.getAttributeValue(null, Attributes.AOGUID.name()),
            r.getAttributeValue(null, Attributes.PARENTGUID.name()),
            getPlainCode(r),
            r.getAttributeValue(null, Attributes.FORMALNAME.name()),
            r.getAttributeValue(null, Attributes.SHORTNAME.name()),
            r.getAttributeValue(null, Attributes.AOLEVEL.name()),
            Integer.parseInt(r.getAttributeValue(null, Attributes.LIVESTATUS.name())));
    }

    private static String getPlainCode(XMLStreamReader r) {
        String plainCode = r.getAttributeValue(null, Attributes.PLAINCODE.name());
		if (null != plainCode) {
			return plainCode;
		}
		String level = r.getAttributeValue(null, Attributes.AOLEVEL.name());
		StringBuilder builder = new StringBuilder(27);
		builder.append(r.getAttributeValue(null, Attributes.REGIONCODE.name()));
		String area = r.getAttributeValue(null, Attributes.AREACODE.name());
		builder.append(null == area ? "000" : area);
		String city = r.getAttributeValue(null, Attributes.CITYCODE.name());
		builder.append(null == city ? "000" : city);
		String place = r.getAttributeValue(null, Attributes.PLACECODE.name());
		builder.append(null == place ? "000" : place);
		if (level.compareTo("6") > 0) {
			if (!"0000".equals(r.getAttributeValue(null, Attributes.EXTRCODE.name()))) {
				builder.append(r.getAttributeValue(null, Attributes.EXTRCODE.name()));
			} else if (!"0000".equals(r.getAttributeValue(null, Attributes.STREETCODE.name()))) {
				builder.append(r.getAttributeValue(null, Attributes.STREETCODE.name()));
			} else if (!"0000".equals(r.getAttributeValue(null, Attributes.PLANCODE.name()))) {
				builder.append(r.getAttributeValue(null, Attributes.PLANCODE.name()));
			}
		}
        return builder.toString();
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

    public String getLevel() {
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
