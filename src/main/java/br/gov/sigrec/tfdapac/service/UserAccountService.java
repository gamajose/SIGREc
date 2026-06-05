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
import java.util.Map;

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
        return create(Map.of(
                "username", value(username),
                "email", value(email),
                "senha", value(rawPassword),
                "role", role.name(),
                "unidade_id", unidadeId == null ? "" : unidadeId.toString()
        ), actorId);
    }

    @Transactional
    public UserAccount create(Map<String, String> form, Long actorId) {
        String username = form.get("username");
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Usuario ja existe.");
        }

        UserRole role = UserRole.valueOf(form.getOrDefault("role", "SOLICITANTE"));
        String senha = form.get("senha");

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setNome(defaultValue(form.get("nome"), username));
        user.setEmail(form.get("email"));
        user.setEndereco(form.get("unidade_endereco"));
        user.setPasswordHash(passwordEncoder.encode(senha));
        user.setRole(role);
        user.setUnidadeId(nullableLong(form.get("unidade_id")));

        UserAccount saved = repository.save(user);
        atualizarPerfilCompleto(saved.getId(), form, false);
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
    public void update(Long id, Map<String, String> form, boolean ativo, Long actorId) {
        UserRole role = UserRole.valueOf(form.getOrDefault("role", "SOLICITANTE"));
        UserAccount user = find(id);
        user.setEmail(form.get("email"));
        user.setNome(defaultValue(form.get("nome"), user.getUsername()));
        user.setEndereco(form.get("unidade_endereco"));
        if (StringUtils.hasText(form.get("senha"))) {
            user.setPasswordHash(passwordEncoder.encode(form.get("senha")));
        }
        user.setRole(role);
        user.setUnidadeId(nullableLong(form.get("unidade_id")));
        user.setAtivo(ativo);
        repository.save(user);
        atualizarPerfilCompleto(id, form, true);
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

    private void atualizarPerfilCompleto(Long id, Map<String, String> form, boolean perfilCompleto) {
        jdbcTemplate.update("""
                update regulacao_tfd.usuarios
                set codigo_usuario = ?,
                    telefone = ?,
                    unidade_telefone = ?,
                    unidade_endereco = ?,
                    unidade_cep = ?,
                    unidade_numero = ?,
                    unidade_bairro = ?,
                    unidade_complemento = ?,
                    perfil_completo = ?,
                    primeiro_login = case when ? then false else true end,
                    data_perfil_atualizado = case when ? then current_timestamp else data_perfil_atualizado end
                where id = ?
                """,
                blankToNull(form.get("codigo_usuario")),
                digits(form.get("telefone")),
                digits(form.get("unidade_telefone")),
                form.get("unidade_endereco"),
                digits(form.get("unidade_cep")),
                form.get("unidade_numero"),
                form.get("unidade_bairro"),
                form.get("unidade_complemento"),
                perfilCompleto,
                perfilCompleto,
                perfilCompleto,
                id);
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

    private Long nullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
