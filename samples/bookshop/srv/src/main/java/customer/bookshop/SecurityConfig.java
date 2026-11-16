/*
 * © 2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package customer.bookshop;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
public class SecurityConfig {

	@Bean
	public HttpFirewall allowSemicolonHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowSemicolon(true);
		return firewall;
	}

	@Bean
	public ServletRegistrationBean<JakartaWebServlet> h2ServletRegistration() {
		ServletRegistrationBean<JakartaWebServlet> registration =
				new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
		registration.addInitParameter("-webAllowOthers", "");
		return registration;
	}

}
