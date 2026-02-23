package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.HotspotResponse;
import ai.mindvex.backend.entity.IndexJob;
import ai.mindvex.backend.repository.FileChurnStatRepository;
import ai.mindvex.backend.repository.IndexJobRepository;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.service.ChurnCalculationEngine;
import ai.mindvex.backend.service.JGitMiningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsController Unit Tests")
class AnalyticsControllerTest {

    @Mock
    private JGitMiningService miningService;

    @Mock
    private ChurnCalculationEngine churnEngine;

    @Mock
    private FileChurnStatRepository churnStatRepository;

    @Mock
    private IndexJobRepository indexJobRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsController analyticsController;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("POST /api/analytics/mine: triggers mining")
    void whenTriggerMining_thenReturnsAccepted() {
        when(indexJobRepository.save(any(IndexJob.class))).thenAnswer(invocation -> {
            IndexJob j = invocation.getArgument(0);
            j.setId(100L);
            return j;
        });

        ResponseEntity<Map<String, Object>> response = analyticsController.triggerMining("http://github.com/test", 90,
                jwt);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(100L, response.getBody().get("jobId"));
    }

    @Test
    @DisplayName("GET /api/analytics/hotspots: returns hotspots data")
    void whenGetHotspots_thenReturnsData() {
        when(churnStatRepository.findHotspots(eq(1L), eq("http://github.com/test"), any(), eq(25.0)))
                .thenReturn(List.of());

        ResponseEntity<List<HotspotResponse>> response = analyticsController.getHotspots("http://github.com/test", 12,
                25.0, jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
