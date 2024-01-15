package no.nav.dagpenger.oppdrag.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val BEARER = "JWT"

@Configuration
internal class SwaggerConfig {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI().info(Info().title("API dp-oppdrag"))
            .components(Components().addSecuritySchemes(BEARER, bearerTokenSecurityScheme()))
            .addSecurityItem(SecurityRequirement().addList(BEARER, listOf("read", "write")))
    }

    private fun bearerTokenSecurityScheme(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .scheme(BEARER)
            .bearerFormat(BEARER)
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
    }
}
