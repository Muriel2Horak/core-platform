package cz.muriel.core.user.boundary;

import cz.muriel.core.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pro přístup k entitám {@link UserAccount}.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
}
