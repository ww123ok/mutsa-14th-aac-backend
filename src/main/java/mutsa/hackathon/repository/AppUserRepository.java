package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser>
    findByProviderAndProviderId(
            String provider,
            String providerId
    );

    Optional<AppUser>
    findByRefreshToken(
            String refreshToken
    );

    boolean existsByProviderAndProviderId(
            String provider,
            String providerId
    );
}