package me.anjsk.bestoffer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // "application-test.yml" 설정을 사용
class BestOfferApplicationTests {

    @Test
    void contextLoads() {
    }

}
