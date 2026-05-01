package me.anjsk.bestoffer;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BestOfferApplicationTests {

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
    }
}
