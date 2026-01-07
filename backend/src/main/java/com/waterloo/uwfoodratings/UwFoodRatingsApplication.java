package com.waterloo.uwfoodratings;

import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UwFoodRatingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UwFoodRatingsApplication.class, args);
    }

    @Bean
    CommandLineRunner run(RatingRepository repository) {
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
    LocalDateTime.now()
);
                
                repository.save(testRating);
                System.out.println("✅ Added dummy data to database!");
            }
        };
    }
}