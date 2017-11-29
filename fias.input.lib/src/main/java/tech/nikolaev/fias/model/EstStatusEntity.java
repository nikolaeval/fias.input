package tech.nikolaev.fias.model;

/**
 * Created by andrey.l.nikolaev@mail.ru on 13.09.2017.
 */
public class EstStatusEntity implements AddressEntity {

    public static final String TYPE = "eststatus";

    private String id;
    private String name;

    public EstStatusEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getCode() {
        return getId();
    }
}
