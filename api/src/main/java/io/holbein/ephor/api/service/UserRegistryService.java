package io.holbein.ephor.api.service;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistryService {

    private final KnownUserRepository knownUserRepository;

    @Transactional
    public void registerOrUpdate(UserContext userContext) {
        String groupsCsv = userContext.groups() != null
                ? String.join(",", userContext.groups())
                : "";
        knownUserRepository.upsert(
                userContext.username(),
                userContext.email(),
                userContext.getEffectiveDisplayName(),
                groupsCsv
        );
    }
}
