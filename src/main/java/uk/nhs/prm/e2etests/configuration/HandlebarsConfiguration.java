package uk.nhs.prm.e2etests.configuration;

import com.github.jknack.handlebars.Handlebars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlebarsConfiguration {
    @Bean
    public Handlebars handlebars() {
        return new Handlebars();
    }
}