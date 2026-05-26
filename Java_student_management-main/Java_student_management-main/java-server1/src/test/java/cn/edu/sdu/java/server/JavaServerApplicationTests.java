package cn.edu.sdu.java.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Packaging should not depend on a local database or H2-compatible schema.")
@SpringBootTest
class JavaServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
