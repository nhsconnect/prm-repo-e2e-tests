package uk.nhs.prm.e2etests.configuration;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class HandlebarsConfiguration {
    @Bean
    public Handlebars handlebars() {
        TemplateLoader loader = new ClassPathTemplateLoader();
        loader.setPrefix("/resources/templates");
        loader.setSuffix(".hbs");
        loader.setCharset(StandardCharsets.UTF_8);
        return new Handlebars(loader);
    }
}
