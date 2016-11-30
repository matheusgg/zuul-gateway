package br.com.gateway;

import br.com.gateway.properties.ZuulGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * A diferenca entre as anotacoes @EnableZuulProxy e @EnableZuulServer é que a primeira habilita a auto configuracao do Zuul
 * e adiciona os filtros responsaveis pela descoberta dos servicos e pelo roteamento. Já a segunda, apenas configura o Zuul server,
 * ou seja, ela nao adiciona os filtros default. Neste caso, a requisicao é transferida para a cadeia de filtros do Zuul e o roteamento
 * deve ser feito de forma manual.
 */
@SpringCloudApplication
@EnableZuulProxy
//@EnableZuulServer
@EnableConfigurationProperties(ZuulGatewayProperties.class)
public class ZuulGatewayApplication {

	public static void main(final String[] args) {
		SpringApplication.run(ZuulGatewayApplication.class, args);
	}
}
