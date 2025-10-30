package com.reactor.pets.query;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalTimeRepository extends JpaRepository<GlobalTimeView, String> {
}
