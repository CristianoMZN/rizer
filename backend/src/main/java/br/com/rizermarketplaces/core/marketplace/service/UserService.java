package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.model.User;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Serviço responsável por operações relacionadas a usuários.
// @Service registra esta classe como bean do Spring.
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Cria ou atualiza o usuário a partir dos dados retornados pelo OAuth2 (ex: Google).
     * Chamado após login bem-sucedido.
     * @Transactional: operação que escreve no banco dentro de uma transação.
     */
    @Transactional
    public User syncFromOAuth2(OAuth2User principal, String provider) {
        // O atributo 'sub' vem do padrão OpenID Connect; fallback para principal.getName()
        String providerId = principal.getAttribute("sub") != null
                ? principal.getAttribute("sub")
                : principal.getName();

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existing -> {
                    // Atualiza usuário existente com informações do provedor
                    existing.setName(principal.getAttribute("name"));
                    existing.setEmail(principal.getAttribute("email"));
                    existing.setAvatarUrl(principal.getAttribute("picture"));
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    // Cria novo usuário se não existir
                    User user = new User();
                    user.setProvider(provider);
                    user.setProviderId(providerId);
                    user.setName(principal.getAttribute("name"));
                    user.setEmail(principal.getAttribute("email"));
                    user.setAvatarUrl(principal.getAttribute("picture"));
                    return userRepository.save(user);
                });
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
