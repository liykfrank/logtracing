package com.frank.demo.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.frank.demo.entity.SalesData;
import com.frank.demo.repository.SalesDataRepository;

@RestController
public class Controller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	@Autowired
	private SalesDataRepository userRepository;

	@GetMapping("/{agencyCode}")
	public SalesData findById(@PathVariable String agencyCode) {

		LOGGER.info("find SalesData by agencyCode : " + agencyCode);

		SalesData findOne = null;// this.agencyRepository.findOne(id);

		if ("99999901".equals(agencyCode)) {
			findOne = new SalesData();
			findOne.setId(1L);
			findOne.setAmount(BigDecimal.valueOf(5000.0));
			findOne.setAgencyCode("99999901");

		} else if ("99999902".equals(agencyCode)) {
			findOne = new SalesData();
			findOne.setId(2L);
			findOne.setAmount(BigDecimal.valueOf(20000.0));
			findOne.setAgencyCode("99999902");
		} else {
			findOne = new SalesData();
			findOne.setId(-1L);
			findOne.setAmount(BigDecimal.valueOf(0.0));
			findOne.setAgencyCode("NA");
		}

		return findOne;
	}
}
