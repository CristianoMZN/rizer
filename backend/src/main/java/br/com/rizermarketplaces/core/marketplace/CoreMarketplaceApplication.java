/*
 * Arquivo principal da aplicação Spring Boot.
 *
 * Este arquivo contém a classe de inicialização que sobe o contexto do
 * Spring e inicia o servidor embutido (Tomcat/Jetty) conforme configuração.
 * Comentários em português explicam anotações e chamadas do Spring Boot.
 */
package br.com.rizermarketplaces.core.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreMarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreMarketplaceApplication.class, args);
	}

}
