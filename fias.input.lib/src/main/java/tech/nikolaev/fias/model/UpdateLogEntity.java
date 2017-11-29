package tech.nikolaev.fias.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by andrey.l.nikolaev@mail.ru on 11.10.2017.
 */
public class UpdateLogEntity implements AddressEntity {

    public static final String TYPE = "updatelog";
    public static final String DATE_FORMAT = "yyyyMMdd";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public enum State {
        SUCCESS,
        ERROR,
        RUNNING;

    }

    private String date;
    private State state;

    public UpdateLogEntity(LocalDate date, State state) {
        this.date = date.format(FORMATTER);
        this.state = state;
    }

    public String getDate() {
        return date;
    }

    public State getState() {
        return state;
    }

    @Override
    public String getId() {
        return getDate();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getCode() {
        return this.getState().name();
    }

    @Override
    public String getName() {
        return this.getState().name();
    }

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, FORMATTER);
    }

    public static String formatDate(LocalDate date) {
        return date.format(FORMATTER);
    }

}
