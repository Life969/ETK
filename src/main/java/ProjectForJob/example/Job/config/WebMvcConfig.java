package ProjectForJob.example.Job.config;

import ProjectForJob.example.Job.interceptor.GlobalModelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GlobalModelInterceptor());
    }

    /**
     * Отдаём загруженные пользователем файлы из папки ./uploads/ в корне проекта.
     * Работает для /uploads/couplings/, /uploads/threads/, /uploads/adapters/ и т.д.
     * "file:" в начале обязателен — говорит Spring, что это физический путь, не classpath.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = "file:" + System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
    }
}