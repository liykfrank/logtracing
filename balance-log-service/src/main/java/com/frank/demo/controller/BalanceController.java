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
import com.frank.demo.entity.AssuranceData;
import com.frank.demo.entity.SalesData;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
public class BalanceController {
	private static final Logger LOGGER = LoggerFactory.getLogger(BalanceController.class);
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private LoadBalancerClient loadBalancerClient;

	@GetMapping("/{agencyCode}")
	public Agency findById(@PathVariable String agencyCode) {
		LOGGER.info("find Balance by agencyCode : "+agencyCode);
		AssuranceData ad = this.findAssuranceById(agencyCode);
		SalesData sd = this.findSalesById(agencyCode);
		BigDecimal bal = ad.getAmount().subtract(sd.getAmount());
		
		Agency ag = new Agency();
		ag.setCode(agencyCode);
		ag.setBalance(bal);
		ag.setGuaranteeAmount(ad.getAmount());
		ag.setSalesAmount(sd.getAmount());
		
		return ag;
		
//		return this.restTemplate.getForObject("http://salesdata-service/" + id, SalesData.class);
	}

	@HystrixCommand(fallbackMethod = "findAssuranceByIdFallback")
	public AssuranceData findAssuranceById(String id) {
		LOGGER.info("find Assurance by agencyCode : "+id);
		return this.restTemplate.getForObject("http://assurance-service/" + id, AssuranceData.class);
	}

	public AssuranceData findAssuranceByIdFallback(Long id) {
		AssuranceData data = new AssuranceData();
		data.setId(-1L);
		data.setAgencyCode("Can not connect to assurance-service");
		return data;
	}

	@HystrixCommand(fallbackMethod = "findSalesByIdFallback")
	public SalesData findSalesById(String id) {
		LOGGER.info("find Sales by agencyCode : "+id);
		return this.restTemplate.getForObject("http://salesdata-service/" + id, SalesData.class);
	}

	public SalesData findSalesByIdFallback(Long id) {
		SalesData data = new SalesData();
		data.setId(-1L);
		data.setAgencyCode("Can not connect to salesdata-service");
		return data;
	}

	// assurance-service:
	// ribbon:
	// listOfServers: localhost:8092
	// salesdata-service:

}
