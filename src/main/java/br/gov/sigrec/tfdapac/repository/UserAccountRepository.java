package br.gov.sigrec.tfdapac.repository;

import br.gov.sigrec.tfdapac.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    @Query("""
            select u
            from UserAccount u
            where lower(u.username) = lower(:login)
               or lower(u.codigoUsuario) = lower(:login)
            """)
    Optional<UserAccount> findByUsernameOrCodigoUsuario(@Param("login") String login);

    boolean existsByUsername(String username);
}
