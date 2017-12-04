package tech.nikolaev.fias.model;

/**
 * Created by andrey.l.nikolaev@mail.ru on 01.09.2017.
 */
public class AddressTypeEntity implements AddressEntity{

    public static final String TYPE = "type";

    private String shortName;
    private String offName;
    private String level;

    public AddressTypeEntity(String shortName, String offName, String level) {
        this.shortName = shortName;
        this.offName = offName;
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    @Override
    public String getId() {
        return getCode();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getCode() {
        return shortName;
    }

    @Override
    public String getName() {
        return offName;
    }
}
