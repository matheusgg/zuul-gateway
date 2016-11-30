package br.com.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstanceInfo {
	private String host;
	private int port;
}
