package com.lusolaw.repository;

import com.lusolaw.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    long countByRole(User.Role role);
    List<User> findByRoleAndAccountStatusOrderByCreatedAtDesc(User.Role role, User.AccountStatus accountStatus);
    List<User> findByIdentificationDocumentDataIsNotNullOrderByCreatedAtDesc(Pageable pageable);
}
