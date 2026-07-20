package com.caioms.java_marketplace.modules.identity.application.repositories;

import com.caioms.java_marketplace.modules.identity.application.models.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);
}
