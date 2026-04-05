package com.lusolaw.repository;

import com.lusolaw.model.Service;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    @EntityGraph(attributePaths = {"lawyer"})
    List<Service> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"lawyer"})
    List<Service> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);

    @Override
    @EntityGraph(attributePaths = {"lawyer"})
    Optional<Service> findById(Long id);
}
