package com.frank.demo.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.frank.demo.entity.Agency;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
public class RiskController {
	private static final Logger LOGGER = LoggerFactory.getLogger(RiskController.class);
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private LoadBalancerClient loadBalancerClient;

	// @HystrixCommand(fallbackMethod = "findByIdFallback")
	// @GetMapping("/sales/{id}")
	// public SalesData findById(@PathVariable Long id) {
	// return this.restTemplate.getForObject("http://salesdata-service/" + id,
	// SalesData.class);
	// }
	//
	// public SalesData findByIdFallback(Long id) {
	// SalesData SalesData = new SalesData();
	// SalesData.setId(-1L);
	// SalesData.setAgencyCode("Can not connect to
	// microservice-provider-SalesData");
	// return SalesData;
	// }
	@GetMapping("/{agencyCode}")
	public Agency findById(@PathVariable String agencyCode) {
		LOGGER.info("find Agency by agencyCode : " + agencyCode);
		Agency ad = this.findAgencyById(agencyCode);
		if (ad.getId() < 0) {
			ad.setName("agent doesn't exist");
			return ad;
		}
		Agency sd = this.findBalanceById(agencyCode);
		sd.setName(ad.getName());
		sd.setId(ad.getId());
		if (sd.getBalance().compareTo(BigDecimal.valueOf(1000L)) > 0) {
			sd.setStatus("active");
		} else {
			sd.setStatus("locked");
		}

		return sd;

		// return this.restTemplate.getForObject("http://salesdata-service/" + id,
		// SalesData.class);
	}

	@HystrixCommand(fallbackMethod = "findBalanceByIdFallback")
	public Agency findBalanceById(String id) {
		LOGGER.info("find Balance by agencyCode : " + id);
		return this.restTemplate.getForObject("http://balance-service/" + id, Agency.class);
	}

	public Agency findBalanceByIdFallback(Long id) {
		Agency data = new Agency();
		data.setId(-1L);
		data.setCode("Can not connect to assurance-service");
		return data;
	}

	@HystrixCommand(fallbackMethod = "findAgencyByIdFallback")
	public Agency findAgencyById(String id) {
		LOGGER.info("find Agency by agencyCode : " + id);
		return this.restTemplate.getForObject("http://agency-service/" + id, Agency.class);
	}

	public Agency findAgencyByIdFallback(Long id) {
		Agency data = new Agency();
		data.setId(-1L);
		data.setCode("Can not connect to salesdata-service");
		return data;
	}

}
