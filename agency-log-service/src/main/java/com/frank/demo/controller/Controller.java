package com.frank.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.frank.demo.entity.Agency;
import com.frank.demo.repository.AgencyRepository;

@RestController
public class Controller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	@Autowired
	private AgencyRepository agencyRepository;

	@GetMapping("/{code}")
	public Agency findByCode(@PathVariable String code) {
		LOGGER.info("find agency by agencyCode : "+code);
		Agency findOne = null;// this.agencyRepository.findOne(id);

		if ("99999901".equals(code)) {
			findOne = new Agency();
			findOne.setId(1L);
			findOne.setName("agent1");
			findOne.setCode("99999901");

		} else if ("99999902".equals(code)) {
			findOne = new Agency();
			findOne.setId(2L);
			findOne.setName("agent2");
			findOne.setCode("99999902");
		} else {
			findOne = new Agency();
			findOne.setId(-1L);
			findOne.setName("NA");
			findOne.setCode("NA");
		}

		return findOne;
	}
}
