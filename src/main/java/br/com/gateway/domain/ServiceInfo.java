package br.com.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ServiceInfo {
	private String id;
	private List<InstanceInfo> instances;
}
