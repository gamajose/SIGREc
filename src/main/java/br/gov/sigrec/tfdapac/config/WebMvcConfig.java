package br.gov.sigrec.tfdapac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final PerfilCompletoInterceptor perfilCompletoInterceptor;

    public WebMvcConfig(PerfilCompletoInterceptor perfilCompletoInterceptor) {
        this.perfilCompletoInterceptor = perfilCompletoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perfilCompletoInterceptor)
                .addPathPatterns("/**");
    }
}
