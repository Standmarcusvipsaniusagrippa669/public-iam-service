package com.valiantech.core.iam.auth.repository;

import com.valiantech.core.iam.auth.model.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for persisting and querying UserLogin entities.
 */
public interface UserLoginRepository extends JpaRepository<UserLogin, UUID> {

    /**
     * Finds all login events for a given user ordered by login timestamp descending.
     *
     * @param userId user UUID to filter logins
     * @return list of UserLogin records
     */
    List<UserLogin> findByUserIdOrderByLoginAtDesc(UUID userId);
}
