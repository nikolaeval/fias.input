package tech.nikolaev.fias.model;

import javax.xml.stream.XMLStreamReader;

/**
 * Created by andrey.l.nikolaev@mail.ru on 12.09.2017.
 */
public class HouseEntity implements AddressEntity {

    public static final String TYPE = "house";

    public enum Attributes {
        HOUSEGUID,
        AOGUID,
        POSTALCODE,
        HOUSENUM,
        BUILDNUM,
        STRUCNUM;
    }

    private String guid;
    private String parentGuid;
    private String postalCode;
    private String num;
    private String buildNum;
    private String structNum;
    private String name;

    public HouseEntity(String guid, String parentGuid, String postalCode, String num, String buildNum, String structNum) {
        this.guid = guid;
        this.parentGuid = parentGuid;
        this.postalCode = postalCode;
        this.num = num;
        this.buildNum = buildNum;
        this.structNum = structNum;
    }

    public static HouseEntity createFromStream(XMLStreamReader r) {
        return new HouseEntity(
                r.getAttributeValue(null, Attributes.HOUSEGUID.name()),
                r.getAttributeValue(null, Attributes.AOGUID.name()),
                r.getAttributeValue(null, Attributes.POSTALCODE.name()),
                r.getAttributeValue(null, Attributes.HOUSENUM.name()),
                r.getAttributeValue(null, Attributes.BUILDNUM.name()),
                r.getAttributeValue(null, Attributes.STRUCNUM.name()));
    }


    public String getGuid() {
        return guid;
    }

    public String getParentGuid() {
        return parentGuid;
    }

    @Override
    public String getCode() {
        return postalCode;
    }

    public String getNum() {
        return num;
    }

    public String getBuildNum() {
        return buildNum;
    }

    public String getStructNum() {
        return structNum;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return "HouseEntity{" +
                "guid='" + guid + '\'' +
                ", parentGuid='" + parentGuid + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
