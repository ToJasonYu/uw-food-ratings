package com.waterloo.uwfoodratings;

import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedInitialRating(RatingRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Rating testRating = new Rating(
                        null,
                        null,
                        "Lazeez",
                        "Large Chicken on the Rocks",
                        5,
                        "The spice level was perfect.",
                        0,
                        LocalDateTime.now());

                repository.save(testRating);
                System.out.println("Added dummy data to database!");
            }
        };
    }
}
