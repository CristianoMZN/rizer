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

// @SpringBootApplication: Anotação de conveniência que combina:
//  - @Configuration: indica que esta classe pode conter beans/configurações
//  - @EnableAutoConfiguration: habilita configurações automáticas do Spring Boot
//  - @ComponentScan: faz scan automático de componentes no pacote atual
// Ela configura a aplicação Spring Boot com comportamento padrão.
@SpringBootApplication
public class CoreMarketplaceApplication {

	public static void main(String[] args) {
		// Ponto de entrada da aplicação Java.
		// SpringApplication.run(...) inicializa o contexto do Spring,
		// cria e injeta beans, configura servidores e executa a aplicação.
		SpringApplication.run(CoreMarketplaceApplication.class, args);
	}

}
