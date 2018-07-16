package com.frank.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frank.demo.entity.Agency;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, Long> {
}
