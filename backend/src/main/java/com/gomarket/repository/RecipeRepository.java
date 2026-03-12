package com.gomarket.repository;

import com.gomarket.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByWeatherContext(String weatherContext);
}
