package br.com.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayForwardController {

	@RequestMapping(path = "/internal/**")
	public String internal() {
		return "Internal Request";
	}

}
