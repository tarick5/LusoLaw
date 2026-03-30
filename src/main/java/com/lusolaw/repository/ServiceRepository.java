package com.lusolaw.repository;

import com.lusolaw.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByLawyerId(Long lawyerId);
}