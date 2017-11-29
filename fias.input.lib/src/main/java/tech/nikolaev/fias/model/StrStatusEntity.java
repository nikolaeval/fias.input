package tech.nikolaev.fias.model;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
public class StrStatusEntity implements AddressEntity {

    public static final String TYPE = "strstatus";

    private String id;
    private String shortName;
    private String name;

    public StrStatusEntity(String id, String shortName, String name) {
        this.id = id;
        this.shortName = shortName;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCode() {
        return shortName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
