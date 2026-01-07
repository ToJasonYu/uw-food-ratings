package com.waterloo.uwfoodratings;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    private String restaurantName;
    private String dishName;
    private int stars;
    
    @Column(length = 1000)
    private String comment;

    private int upvotes = 0;
    private LocalDateTime timestamp;
}