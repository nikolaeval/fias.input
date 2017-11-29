package tech.nikolaev.fias.input.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by al.nikolaev on 7/7/16.
 */
@Configuration
@ComponentScan({"tech.nikolaev.fias.input.web.service", "tech.nikolaev.fias.service"})
public class ContextConfig {

}
