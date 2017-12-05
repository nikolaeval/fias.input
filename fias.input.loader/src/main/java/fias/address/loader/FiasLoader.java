package fias.address.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import tech.nikolaev.fias.model.UpdateLogEntity;
import tech.nikolaev.fias.loader.AddressLoader;
import tech.nikolaev.fias.config.AppConfig;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by andrey.l.nikolaev@mail.ru on 16.08.2017.
 */
public class FiasLoader {

    private static final Logger logger = LoggerFactory.getLogger(FiasLoader.class);

    /**
     * You can only or load on date (witch clean database option)
     * or you can update DB on date (incremented)
     * If the DB is empty - only load
     * @param args
     */
    public static void main(String[] args) {

        GenericApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        String fiasXmlPath = ctx.getEnvironment().getProperty("fias.file.path");
        String date = ctx.getEnvironment().getProperty("fias.file.date");
        String clean = ctx.getEnvironment().getProperty("clean");

        try {
            AddressLoader addressLoader = ctx.getBean(AddressLoader.class);
            if (!addressLoader.checkForUpdate()) {
                logger.info("The actualy version is already loaded");
                return;
            }

            if (null != clean && Boolean.parseBoolean(clean)) {
                addressLoader.clearDB();
            }

            if (null != date) {
                if (fiasXmlPath != null) {
                    addressLoader.load(Paths.get(fiasXmlPath), LocalDate.parse(date, DateTimeFormatter.ofPattern(UpdateLogEntity.DATE_FORMAT)));
                } else {
                    addressLoader.update(LocalDate.parse(date, DateTimeFormatter.ofPattern(UpdateLogEntity.DATE_FORMAT)));
                }
            } else {
                addressLoader.update();
            }
        } catch (Exception e) {
            logger.error("Error loading fias db: {}", e.getMessage(), e);
        } finally {
            ctx.close();
        }
    }

}
