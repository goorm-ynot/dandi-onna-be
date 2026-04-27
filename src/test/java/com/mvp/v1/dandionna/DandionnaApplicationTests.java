package com.mvp.v1.dandionna;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DANDI_RUN_INFRA_TESTS", matches = "true")
class DandionnaApplicationTests {

	@Test
	void contextLoads() {
	}

}
