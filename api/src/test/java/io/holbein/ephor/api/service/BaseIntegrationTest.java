package io.holbein.ephor.api.service;

import io.holbein.ephor.api.BaseTestcontainersTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest extends BaseTestcontainersTest {
}
