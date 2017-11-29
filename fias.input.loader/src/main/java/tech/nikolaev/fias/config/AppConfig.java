package tech.nikolaev.fias.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Created by andrey.l.nikolaev@mail.ru on 06.09.2017.
 */
@Configuration
@ComponentScan({"tech.nikolaev.fias.loader", "tech.nikolaev.fias.service.dao"})
@PropertySources({
        @PropertySource(value="classpath:config/application.properties", encoding="UTF-8"),
        @PropertySource(value="file:config/application.properties", encoding="UTF-8")
})
public class AppConfig {


}
