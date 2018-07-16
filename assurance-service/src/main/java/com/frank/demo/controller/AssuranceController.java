package com.frank.demo.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.frank.demo.entity.AssuranceData;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
public class AssuranceController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AssuranceController.class);

	@HystrixCommand(fallbackMethod = "findByIdFallback")
	@GetMapping("/{agencyCode}")
	public AssuranceData findById(@PathVariable String agencyCode) {
		LOGGER.info("find AssuranceData by agencyCode : "+agencyCode);
		// return this.restTemplate.getForObject("http://salesdata-service/" + id,
		// SalesData.class);

		AssuranceData findOne = null;// this.agencyRepository.findOne(id);

		if ("99999901".equals(agencyCode)) {
			findOne = new AssuranceData();
			findOne.setId(1L);
			findOne.setAmount(BigDecimal.valueOf(10000.0));
			findOne.setAgencyCode("99999901");

		} else if ("99999902".equals(agencyCode)) {
			findOne = new AssuranceData();
			findOne.setId(2L);
			findOne.setAmount(BigDecimal.valueOf(20000.0));
			findOne.setAgencyCode("99999902");
		} else {
			findOne = new AssuranceData();
			findOne.setId(-1L);
			findOne.setAmount(BigDecimal.valueOf(0.0));
			findOne.setAgencyCode("NA");
		}

		return findOne;
	}
}
