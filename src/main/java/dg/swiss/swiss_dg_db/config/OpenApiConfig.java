package dg.swiss.swiss_dg_db.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info =
                @Info(
                        title = "API Documentation for Swisstour Disc Golf",
                        contact = @Contact(name = "Christopher Walker"),
                        description =
                                "Open API documentation for application to manage Swisstour."),
        servers = {
            @Server(description = "Local ENV", url = "http://localhost:8080"),
            @Server(description = "Production ENV", url = "https://swiss-dg-db.onrender.com")
        })
@SecurityScheme(
        type = SecuritySchemeType.HTTP,
        name = "bearerAuth",
        description = "Login using JWT token",
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {}
