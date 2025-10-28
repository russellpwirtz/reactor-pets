package com.reactor.pets.query;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetStatusRepository extends JpaRepository<PetStatusView, String> {
  List<PetStatusView> findByIsAlive(boolean isAlive);
}
