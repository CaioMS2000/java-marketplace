package com.caioms.java_marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.caioms")
public class JavaMarketplaceApplication {

  public static void main(String[] args) {
    SpringApplication.run(JavaMarketplaceApplication.class, args);
  }
}
