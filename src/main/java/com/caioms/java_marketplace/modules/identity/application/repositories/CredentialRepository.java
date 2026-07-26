package com.caioms.java_marketplace.modules.identity.application.repositories;

import com.caioms.java_marketplace.modules.identity.application.models.Credential;
import com.caioms.java_marketplace.modules.identity.application.models.CredentialType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
	List<Credential> findByUserId(UUID userId);
	Optional<Credential> findByUserIdAndType(UUID userId, CredentialType type);
	boolean existsByUserIdAndType(UUID userId, CredentialType type);
}
