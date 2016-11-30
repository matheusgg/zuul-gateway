package br.com.gateway.controller;

import br.com.gateway.domain.InstanceInfo;
import br.com.gateway.domain.ServiceInfo;
import com.netflix.loadbalancer.ILoadBalancer;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(method = GET)
public class ServiceController {

	private final SpringClientFactory clientFactory;
	private final DiscoveryClient discoveryClient;

	public ServiceController(final DiscoveryClient discoveryClient, final SpringClientFactory clientFactory) {
		this.discoveryClient = discoveryClient;
		this.clientFactory = clientFactory;
	}

	@RequestMapping(path = "/instances")
	public List<ServiceInfo> allServicesInstances() {
		return this.discoveryClient.getServices()
				.stream()
				.map(serviceId -> new ServiceInfo(serviceId, this.getInstanceInfos(serviceId)))
				.collect(toList());
	}

	@RequestMapping(path = "/instances/{serviceId}")
	public List<InstanceInfo> serviceInstances(@PathVariable final String serviceId) {
		return this.getInstanceInfos(serviceId);
	}

	private List<InstanceInfo> getInstanceInfos(final String serviceId) {
		final ILoadBalancer loadBalancer = this.clientFactory.getLoadBalancer(serviceId);
		return loadBalancer.getAllServers()
				.stream()
				.map(server -> new InstanceInfo(server.getHost(), server.getPort()))
				.collect(toList());
	}

}
