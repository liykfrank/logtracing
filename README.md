# faulttolerence
//    docker run -d -it --name rabbit --hostname rabbit -p 30000:5672 -p 30001:15672 rabbitmq:management
//
//      998  docker run -d -it --name es -p 9200:9200 -p 9300:9300 elasticsearch
//  999  docker run -d -it --name kibana --link es:elasticsearch -p 5601:5601 kibana

//docker inspect --format '{{ .NetworkSettings.IPAddress }}' 8570b38e5877
 //  docker inspect --format '{{ .NetworkSettings.IPAddress }}'  36ba0255c836
//    docker run -d -it --name logstash logstash -e 'input { rabbitmq { host => "172.17.0.2" port => 5672 queue => "helloq"  durable => true } }  output { elasticsearch { hosts => ["172.17.0.3"] } }'
*For other versions of OpenShift, follow the instructions in the corresponding branch e.g. ocp-3.9, ocp-3.7, etc

# Fault tolerence - Hystrix and Turbine with RabbitMQ 

Hystrix integration with Turbine via RabbitMQ

## Sections

* [Introduction](#introduction)
* [Prerequisites](#prerequisites)

* [Agency Service](#agency-service)
* [Salesdata Service](#salesdata-service)
* [Balance Service](#balance-service)
* [Risk Service](#risk-service)
* [Turbine Service](#turbine-service)
* [Hystrix Service](#hystrix-service)



## Introduction

This project provides an implementation of circuit breaker with hystrix. Further more it will leverage turbine to gather the distributted hystrix stream informations. This allows you to monitor all of the hystrix status easily. Each application instance pushes the metrics from Hystrix commands to Turbine through a central RabbitMQ broker.

The demo includes 6 applications, 4 fucntion services and 2 infrastructure services:

![](images/riskseq.png?raw=true)


![](images/trace-id.png?raw=true)

Below diagram explains the dependencies and communications of above services  

![](images/log1.png?raw=true)

![](images/log2.png?raw=true)


![](images/log3.png?raw=true)


![](images/log4.png?raw=true)


![](images/log5.png?raw=true)


The following diagram shows the communication model of Spring Cloud Turbine AMQP:

![](images/turbineamqp.png?raw=true)

The application used in this pipeline is a JAX-RS application which is available on GitHub and is imported into Gogs during the setup process:
[https://github.com/OpenShiftDemos/openshift-tasks](https://github.com/OpenShiftDemos/openshift-tasks/tree/eap-7)

## Prerequisites
* As RabbitMQ is used as the broker of histrix metrics collection. you need to install RabbitMQ first. here I use docker to create a RabbitMQ instance with management plugin.

~~~~

  docker run -d -it --name rabbit --hostname rabbit -p 30000:5672 -p 30001:15672 rabbitmq:management

~~~~
  
  

* The web console could be accessed with http://localhost:30001 with guest/guest

## Agency Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-amqp</artifactId>
	</dependency>
	<dependency>
		<groupId>net.logstash.logback</groupId>
		<artifactId>logstash-logback-encoder</artifactId>
		<version>4.9</version>
	</dependency>
	<dependency>
		<groupId>ch.qos.logback</groupId>
		<artifactId>logback-classic</artifactId>
		<version>1.2.3</version>
	</dependency>
	<dependency>
		<groupId>ch.qos.logback</groupId>
		<artifactId>logback-core</artifactId>
		<version>1.2.3</version>
	</dependency>
```

To enable /logback-spring.xml like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<include resource="org/springframework/boot/logging/logback/defaults.xml"/>
	​
	<springProperty scope="context" name="springAppName" source="spring.application.name"/>
	<!-- Example for logging into the build folder of your project -->
	<property name="LOG_FILE" value="${BUILD_FOLDER:-build}/${springAppName}"/>​

	<!-- You can override this to have a custom pattern -->
	<property name="CONSOLE_LOG_PATTERN"
			  value="%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>

	<!-- Appender to log to console -->
	<appender name="console" class="ch.qos.logback.core.ConsoleAppender">
		<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
			<!-- Minimum logging level to be presented in the console logs-->
			<level>DEBUG</level>
		</filter>
		<encoder>
			<pattern>${CONSOLE_LOG_PATTERN}</pattern>
			<charset>utf8</charset>
		</encoder>
	</appender>

	<!-- Appender to log to file -->​
	<appender name="flatfile" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<file>${LOG_FILE}</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.gz</fileNamePattern>
			<maxHistory>7</maxHistory>
		</rollingPolicy>
		<encoder>
			<pattern>${CONSOLE_LOG_PATTERN}</pattern>
			<charset>utf8</charset>
		</encoder>
	</appender>
	​
	<!-- Appender to log to file in a JSON format -->
	<appender name="logstash" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<file>${LOG_FILE}.json</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>${LOG_FILE}.json.%d{yyyy-MM-dd}.gz</fileNamePattern>
			<maxHistory>7</maxHistory>
		</rollingPolicy>
		<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
			<providers>
				<timestamp>
					<timeZone>UTC</timeZone>
				</timestamp>
				<pattern>
					<pattern>
						{
						"severity": "%level",
						"service": "${springAppName:-}",
						"trace": "%X{X-B3-TraceId:-}",
						"span": "%X{X-B3-SpanId:-}",
						"parent": "%X{X-B3-ParentSpanId:-}",
						"exportable": "%X{X-Span-Export:-}",
						"pid": "${PID:-}",
						"thread": "%thread",
						"class": "%logger{40}",
						"rest": "%message"
						}
					</pattern>
				</pattern>
			</providers>
		</encoder>
	</appender>
	
	<appender name="AMQP"
		class="org.springframework.amqp.rabbit.logback.AmqpAppender">
		<layout>
			<pattern>
				{
				"severity": "%level",
				"service": "${springAppName:-}",
				"trace": "%X{X-B3-TraceId:-}",
				"span": "%X{X-B3-SpanId:-}",
				"parent": "%X{X-B3-ParentSpanId:-}",
				"exportable": "%X{X-Span-Export:-}",
				"pid": "${PID:-}",
				"thread": "%thread",
				"class": "%logger{40}",
				"rest": "%message"
				}
			</pattern>
		</layout>

		<!-- RabbitMQ connection -->
		<host>localhost</host>
		<port>30000</port>
		<username>guest</username>
		<password>guest</password>

		<applicationId>agency-service</applicationId>
		<routingKeyPattern>agency-service</routingKeyPattern>
		<declareExchange>true</declareExchange>
		<exchangeType>direct</exchangeType>
		<exchangeName>ex_logstash</exchangeName>
		<!-- <queue>hello</queue> -->

		<virtualHost>/</virtualHost>
		<generateId>true</generateId>
		<charset>UTF-8</charset>
		<durable>true</durable>
		<deliveryMode>PERSISTENT</deliveryMode>
	</appender>
	​
	<root level="INFO">
		<appender-ref ref="console"/>
 	<appender-ref ref="AMQP"/> 
		<!-- uncomment this to have also JSON logs -->
		<!--<appender-ref ref="logstash"/>-->
		<!--<appender-ref ref="flatfile"/>-->
	</root>
</configuration>
```
A bug: the service name will appear in the logs instead of bootstrap.if you set the spring.application.name in bootstrap.yaml then the name is present in the logs without doing the change.
Add /bootstrap.yml in /src/main/resources/

```
spring:
  application:
    name: agency-service
```
you can find the bug here:
https://github.com/spring-cloud/spring-cloud-sleuth/issues/330

You could build and run this application follow below  steps:
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/agency-service-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8091/1

And you will get something like this:

![](images/agency.png?raw=true)

## Salesdata Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

To enable loading of the `DiscoveryClient`, add `@EnableDiscoveryClient` to the according configuration or application class like this:

```java
@RestController
public class Controller {
  @Autowired
  private SalesDataRepository userRepository;

  @GetMapping("/{id}")
  public SalesData findById(@PathVariable Long id) {
    SalesData findOne = this.userRepository.findOne(id);
    return findOne;
  }
}
```
Here is the configuration in `application
.properties`:

```
server:
  port: 8092
spring:
  application:
    name: salesdata-service
  jpa:
    generate-ddl: false
    show-sql: true
    hibernate:
      ddl-auto: none
  datasource:                           
    platform: h2                        
    schema: classpath:schema.sql        
    data: classpath:data.sql
```
You could build and run this application follow below  steps:
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/salesdata-service-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8092/1

And you will get something like this:

![](images/sales.png?raw=true)

## Balance Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-ribbon</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-hystrix</artifactId>
    </dependency>
     <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-netflix-hystrix-stream</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
    </dependency>
  </dependencies>
```

To enable loading of the `DiscoveryClient`, add `@EnableDiscoveryClient` to the according configuration or application class like this:

```java
@SpringBootApplication
@EnableCircuitBreaker
public class BalanceApplication {
  @Bean
  @LoadBalanced
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public static void main(String[] args) {
    SpringApplication.run(BalanceApplication.class, args);
  }
}
```

```java
@RestController
public class Controller {
  @HystrixCommand(fallbackMethod = "findByIdFallback")
  @GetMapping("/agency/{id}")
  public Agency findById(@PathVariable Long id) {
    return this.restTemplate.getForObject("http://agency-service/" + id, Agency.class);
  }
  
  public Agency findByIdFallback(Long id) {
    Agency agency = new Agency();
    agency.setId(-1L);
    agency.setName("Can not connect to agency-service");
    return agency;
  }
}
```
Here is the configuration in `application
.properties`:

```
server:
  port: 8081
spring:
  application:
    name: balance-service
  rabbitmq:
    host: localhost
    port: 30000
    username: guest
    password: guest
agency-service:
  ribbon:
    listOfServers: localhost:8091
```
You could build and run this application follow below steps:
As this service depends on Agency service, you may need to run Agency service first.
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/balance-service-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8081/agency/1

And you will get something like this:

![](images/balance-success.png?raw=true)

In case of Agency service is out of service, you wiill have something like this:
![](images/balance-fallback.png?raw=true)


And this proves that the fallback of Hystrix is working well.

## Risk Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-ribbon</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-hystrix</artifactId>
    </dependency>
     <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-netflix-hystrix-stream</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
    </dependency>
  </dependencies>
```

To enable loading of the `DiscoveryClient`, add `@EnableDiscoveryClient` to the according configuration or application class like this:

```java
@SpringBootApplication
@EnableCircuitBreaker
public class RiskApplication {
  @Bean
  @LoadBalanced
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public static void main(String[] args) {
    SpringApplication.run(RiskApplication.class, args);
  }
}
```

```java
  @HystrixCommand(fallbackMethod = "findByIdFallback")
  @GetMapping("/sales/{id}")
  public SalesData findById(@PathVariable Long id) {
    return this.restTemplate.getForObject("http://salesdata-service/" + id, SalesData.class);
  }
  
  public SalesData findByIdFallback(Long id) {
    SalesData SalesData = new SalesData();
    SalesData.setId(-1L);
    SalesData.setAgencyCode("Can not connect to microservice-provider-SalesData");
    return SalesData;
  }
```
Here is the configuration in `application
.properties`:

```
server:
  port: 8082
spring:
  application:
    name: risk-service
  rabbitmq:
    host: localhost
    port: 30000
    username: guest
    password: guest
salesdata-service:
  ribbon:
    listOfServers: localhost:8092
```
You could build and run this application follow below steps:
As this service depends on Agency service, you may need to run Agency service first.
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/risk-service-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8082/sales/1

And you will get something like this:

![](images/risk.png?raw=true)

In case of Agency service is out of service, you wiill have something like this:
![](images/riskfallback.png?raw=true)


And this proves that the fallback of Hystrix is working well.


## Turbine Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
   <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-turbine-stream</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
    </dependency>
  </dependencies>
```

To enable loading of the `DiscoveryClient`, add `@EnableDiscoveryClient` to the according configuration or application class like this:

```java
@SpringBootApplication
@EnableTurbineStream
public class TurbineApplication {
  public static void main(String[] args) {
    SpringApplication.run(TurbineApplication.class, args);
  }
}
```

Here is the configuration in `application
.properties`:

```
server:
  port: 8031
spring:
  application:
    name: hystrix-turbine-mq
  rabbitmq:
    host: localhost
    port: 30000
    username: guest
    password: guest
```
You could build and run this application follow below steps:
As this service depends on Agency service, you may need to run Agency service first.
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/hystrix-turbine-mq-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8031/turbine.stream

And you will get something like this:

![](images/balance-success.png?raw=true)

In case of Agency service is out of service, you wiill have something like this:
![](images/balance-fallback.png?raw=true)


And this proves that the fallback of Hystrix is working well.



## Hystrix Service

This service is nothing more than an ordinary spring boot application:
For the data persistence a in-memory H2 database was used.     
The following is the dependencies used in this project:

```xml
    <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-hystrix-dashboard</artifactId>
    </dependency>
  </dependencies>
```

To enable loading of the `DiscoveryClient`, add `@EnableDiscoveryClient` to the according configuration or application class like this:

```java
@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboardApplication {
  public static void main(String[] args) {
    SpringApplication.run(HystrixDashboardApplication.class, args);
  }
}
```

Here is the configuration in `application
.properties`:

```
server:
  port: 8030
```
You could build and run this application follow below steps:
As this service depends on Agency service, you may need to run Agency service first.
- Go to agency-service directory
```
cd agency-service
```
- Compile with maven
```
mvn clean package
```
- Run this service
```
java -jar target/hystrix-dashboard-0.0.1-SNAPSHOT.jar
```

- Then you could test this service with below link:
http://localhost:8030/hystrix

And you will get something like this:
![](images/hystrix0.png?raw=true)

Next type in your turbine url "http://localhost:8031/turbine.stream" and click on monitor Stream

![](images/hystrixdashboard.png?raw=true)

In case of Agency service is out of service, you wiill have something like this:
![](images/balance-fallback.png?raw=true)


And this proves that the fallback of Hystrix is working well.

![](images/zipkin0.png?raw=true)

![](images/zipkin1.png?raw=true)

![](images/zipkin2.png?raw=true)

![](images/kibana01.png?raw=true)
