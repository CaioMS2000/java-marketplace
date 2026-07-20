package com.caioms.java_marketplace.http.openapi;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

@Component
public class SwaggerUiDarkThemeTransformer extends SwaggerIndexPageTransformer {

	private static final String DARK_CSS_LINK = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/swagger-dark.css\" />";

	public SwaggerUiDarkThemeTransformer(SwaggerUiConfigProperties swaggerUiConfig,
	        SwaggerUiOAuthProperties swaggerUiOAuthProperties,
	        SwaggerWelcomeCommon swaggerWelcomeCommon, ObjectMapperProvider objectMapperProvider) {
		super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon,
		        objectMapperProvider);
	}

	@Override
	public Resource transform(HttpServletRequest request, Resource resource,
	        ResourceTransformerChain transformerChain) throws IOException {
		Resource transformed = super.transform(request, resource, transformerChain);

		String filename = resource.getFilename();
		if (filename != null && filename.equals("index.html")) {
			String html = new String(transformed.getInputStream().readAllBytes(),
			        StandardCharsets.UTF_8);
			html = html.replace("</head>", DARK_CSS_LINK + "</head>");
			return new TransformedResource(resource, html.getBytes(StandardCharsets.UTF_8));
		}
		return transformed;
	}
}
