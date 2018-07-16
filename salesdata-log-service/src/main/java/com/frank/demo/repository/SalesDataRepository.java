package com.frank.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.frank.demo.entity.SalesData;

@Repository
public interface SalesDataRepository extends JpaRepository<SalesData, Long> {
}
