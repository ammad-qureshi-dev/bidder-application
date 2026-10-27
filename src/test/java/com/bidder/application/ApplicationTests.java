package com.bidder.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@Import(TestcontainersConfiguration.class)
class ApplicationTests {

  @Test
  void contextLoads() {}
}
