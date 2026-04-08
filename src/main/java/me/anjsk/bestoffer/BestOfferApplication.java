package me.anjsk.bestoffer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BestOfferApplication {

    public static void main(String[] args) {
        SpringApplication.run(BestOfferApplication.class, args);
    }

}
