package com.frank.demo.entity;

import java.math.BigDecimal;


public class SalesData {
 
  private Long id;
  private String agencyCode;

  public String getAgencyCode() {
	return agencyCode;
}

public void setAgencyCode(String agencyCode) {
	this.agencyCode = agencyCode;
}

private BigDecimal amount;

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

 

  public BigDecimal getBalance() {
    return this.amount;
  }

  public void setBalance(BigDecimal balance) {
    this.amount = balance;
  }

}
