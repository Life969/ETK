package ProjectForJob.example.Job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads")
                .toAbsolutePath()
                .normalize();

        String location = uploadDir.toUri().toString();
        // location будет вида: file:///app/uploads/   или   file:///C:/Users/.../uploads/

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}