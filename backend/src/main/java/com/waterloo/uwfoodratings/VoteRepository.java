package com.waterloo.uwfoodratings;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserIdAndRatingId(Long userId, Long ratingId);
}