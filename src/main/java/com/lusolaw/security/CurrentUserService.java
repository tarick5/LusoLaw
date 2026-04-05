package com.lusolaw.security;

import com.lusolaw.model.User;
import com.lusolaw.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(UNAUTHORIZED, "Autenticacao obrigatoria");
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Utilizador nao encontrado"));

        if (user.getRole() == User.Role.LAWYER && user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(FORBIDDEN, "Conta de advogado ainda nao esta ativa");
        }

        return user;
    }

    public User requireRole(User.Role role) {
        User user = requireAuthenticatedUser();
        if (user.getRole() != role) {
            throw new ResponseStatusException(FORBIDDEN, "Permissao insuficiente");
        }
        return user;
    }
}
