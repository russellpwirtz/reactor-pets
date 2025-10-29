package com.reactor.pets.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerProgressionRepository extends JpaRepository<PlayerProgressionView, String> {
}
