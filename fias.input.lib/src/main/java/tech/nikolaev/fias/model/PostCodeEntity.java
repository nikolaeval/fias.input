package tech.nikolaev.fias.model;

/**
 * Created by andrey.l.nikolaev@mail.ru on 9/16/17.
 */
public class PostCodeEntity implements AddressEntity {

    public static final String TYPE = "postcode";

    private String postcode;
    private String regionCode;
    private String name;

    public PostCodeEntity(String postcode, String regionCode, String name) {
        this.postcode = postcode;
        this.regionCode = regionCode;
        this.name = name;
    }

    @Override
    public String getId() {
        return this.postcode;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getCode() {
        return postcode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "PostCodeEntity{" +
                "postcode='" + postcode + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
