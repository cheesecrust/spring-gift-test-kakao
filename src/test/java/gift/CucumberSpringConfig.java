package gift;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {

    private static final String POSTGRES_SERVICE = "postgres-1";
    private static final int POSTGRES_PORT = 5432;

    static ComposeContainer composeContainer = new ComposeContainer(
            new File("docker-compose.yml")
    )
    .withLocalCompose(true)
    .withExposedService(POSTGRES_SERVICE, POSTGRES_PORT,
            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    static {
        composeContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = composeContainer.getServiceHost(POSTGRES_SERVICE, POSTGRES_PORT);
        Integer port = composeContainer.getServicePort(POSTGRES_SERVICE, POSTGRES_PORT);
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://" + host + ":" + port + "/testdb");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }
}
