package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.GraphResponse;
import ai.mindvex.backend.dto.ReferenceResult;
import ai.mindvex.backend.entity.IndexJob;
import ai.mindvex.backend.repository.IndexJobRepository;
import ai.mindvex.backend.service.DependencyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GraphController Unit Tests")
class GraphControllerTest {

        @Mock
        private IndexJobRepository indexJobRepository;

        @Mock
        private DependencyEngine dependencyEngine;

        @Mock
        private JdbcTemplate jdbc;

        @InjectMocks
        private GraphController graphController;

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
        @DisplayName("POST /api/graph/build: enqueues dependency build graph metadata")
        void whenBuildGraph_thenReturnsAccepted() {
                when(indexJobRepository.save(any(IndexJob.class))).thenAnswer(invocation -> {
                        IndexJob j = invocation.getArgument(0);
                        j.setId(200L);
                        return j;
                });

                ResponseEntity<Map<String, Object>> response = graphController.buildGraph("http://github.com/myrepo",
                                jwt);

                assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
                assertEquals(200L, response.getBody().get("jobId"));
        }

        @Test
        @DisplayName("GET /api/graph/dependencies: returns cytoscape formatted graph")
        void whenGetDependencies_thenReturnsData() {
                when(dependencyEngine.getAllEdgesRaw(eq(1L), eq("http://github.com/myrepo")))
                                .thenReturn(List.of());

                ResponseEntity<GraphResponse> response = graphController.getDependencies("http://github.com/myrepo",
                                null, 20, jwt);

                assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("GET /api/graph/references: returns references")
        @SuppressWarnings("unchecked")
        void whenGetReferences_thenReturnsData() {
                when(jdbc.query(anyString(), any(RowMapper.class), eq(1L), eq("http://github.com/myrepo"),
                                eq("mySymbol")))
                                .thenReturn(List.of());

                ResponseEntity<List<ReferenceResult>> response = graphController
                                .getReferences("http://github.com/myrepo", "mySymbol", jwt);

                assertEquals(HttpStatus.OK, response.getStatusCode());
        }
}
