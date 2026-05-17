package br.gov.sigrec.tfdapac.service;

import br.gov.sigrec.tfdapac.domain.UserAccount;
import br.gov.sigrec.tfdapac.domain.UserRole;
import br.gov.sigrec.tfdapac.repository.UserAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserAccount> findAll() {
        return repository.findAll();
    }

    public UserAccount find(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public UserAccount create(String username, String email, String rawPassword, UserRole role, Long unidadeId, Long actorId) {
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Usuario ja existe.");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setUnidadeId(unidadeId);
        UserAccount saved = repository.save(user);
        syncRole(saved);
        audit(saved.getId(), actorId, "CRIAR_USUARIO", "Usuario criado com perfil " + role);
        return saved;
    }

    @Transactional
    public void update(Long id, String email, String rawPassword, UserRole role, Long unidadeId, boolean ativo, Long actorId) {
        UserAccount user = find(id);
        user.setEmail(email);
        if (StringUtils.hasText(rawPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setUnidadeId(unidadeId);
        user.setAtivo(ativo);
        repository.save(user);
        syncRole(user);
        audit(id, actorId, "EDITAR_USUARIO", "Perfil atualizado para " + role);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        UserAccount user = find(id);
        user.setAtivo(false);
        repository.save(user);
        audit(id, actorId, "DELETAR_USUARIO", "Usuario desativado para preservar historico");
    }

    private void syncRole(UserAccount user) {
        jdbcTemplate.update("delete from regulacao_tfd.user_roles where usuario_id = ?", user.getId());
        String roleName = "ROLE_" + user.getRole().name();
        Long roleId = jdbcTemplate.queryForObject("select id from regulacao_tfd.roles where nome = ?", Long.class, roleName);
        jdbcTemplate.update("insert into regulacao_tfd.user_roles (usuario_id, role_id) values (?, ?)", user.getId(), roleId);
    }

    private void audit(Long targetId, Long actorId, String action, String details) {
        jdbcTemplate.update("""
                insert into regulacao_tfd.usuario_auditoria (usuario_alvo_id, usuario_acao_id, acao, detalhes)
                values (?, ?, ?, ?)
                """, targetId, actorId, action, details);
    }
}
