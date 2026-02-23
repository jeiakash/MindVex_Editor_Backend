package ai.mindvex.backend.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import ai.mindvex.backend.security.CustomOAuth2UserService;
import ai.mindvex.backend.security.HttpCookieOAuth2AuthorizationRequestRepository;
import ai.mindvex.backend.security.OAuth2AuthenticationFailureHandler;
import ai.mindvex.backend.security.OAuth2AuthenticationSuccessHandler;

/**
 * Test configuration that provides mock beans needed for controller slice
 * tests.
 * Mocks JPA metamodel (for @EnableJpaAuditing) and all OAuth2 security beans
 * (so that SecurityConfig can be constructed without real OAuth2 providers).
 */
@TestConfiguration
@MockBean({
        JpaMetamodelMappingContext.class,
        CustomOAuth2UserService.class,
        OAuth2AuthenticationSuccessHandler.class,
        OAuth2AuthenticationFailureHandler.class,
        HttpCookieOAuth2AuthorizationRequestRepository.class
})
public class TestJpaConfig {
}
