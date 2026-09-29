package com.imfundokahle.repository;

import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** Repositorio de usuarios. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    List<User> findByRole(Role role);
    List<User> findTop5ByOrderByCreatedAtDesc();
}
