package com.caioms.java_marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JavaMarketplaceApplicationTests {

  @Test
  void contextLoads() {}
}
