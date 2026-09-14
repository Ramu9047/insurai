package com.insurai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "jwt.secret=InsurAI_Super_Secret_Jwt_Key_2026_Must_Be_At_Least_32_Bytes_Long_Test!"
})
class InsuraiBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
