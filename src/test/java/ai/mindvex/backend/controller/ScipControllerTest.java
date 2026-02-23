package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.HoverResponse;
import ai.mindvex.backend.dto.IndexJobResponse;
import ai.mindvex.backend.entity.IndexJob;
import ai.mindvex.backend.entity.User;
import ai.mindvex.backend.repository.IndexJobRepository;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.service.ScipQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScipController Unit Tests")
class ScipControllerTest {

        @Mock
        private ScipQueryService scipQueryService;

        @Mock
        private UserRepository userRepository;

        @Mock
        private IndexJobRepository indexJobRepository;

        @InjectMocks
        private ScipController scipController;

        private UserDetails userDetails;
        private final Long USER_ID = 1L;

        @BeforeEach
        void setUp() {
                User testUser = User.builder().id(USER_ID).email("user").build();
                when(userRepository.findByEmail("user")).thenReturn(Optional.of(testUser));

                userDetails = mock(UserDetails.class);
                when(userDetails.getUsername()).thenReturn("user");
        }

        @Test
        @DisplayName("POST /api/scip/upload: returns jobId")
        void whenUploadScip_thenReturnsJobId() throws IOException {
                MockMultipartFile file = new MockMultipartFile("file", "index.scip", "application/octet-stream",
                                "dummy_content".getBytes());

                IndexJob mockJob = new IndexJob();
                mockJob.setId(100L);
                when(indexJobRepository.save(any(IndexJob.class))).thenReturn(mockJob);

                ResponseEntity<Map<String, Object>> response = scipController.upload(userDetails,
                                "http://github.com/test", file);

                assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
                assertEquals(100L, response.getBody().get("jobId"));
        }

        @Test
        @DisplayName("GET /api/scip/hover: returns content")
        void whenHover_thenReturnsHoverData() {
                HoverResponse hResponse = HoverResponse.builder()
                                .symbol("List")
                                .displayName("java.util.List")
                                .documentation("Interface for lists")
                                .build();
                when(scipQueryService.getHover(USER_ID, "http://repo", "File.java", 10, 5))
                                .thenReturn(Optional.of(hResponse));

                ResponseEntity<HoverResponse> response = scipController.hover(userDetails, "http://repo", "File.java",
                                10, 5);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("List", response.getBody().getSymbol());
        }

        @Test
        @DisplayName("GET /api/scip/jobs/{id}: returns job status")
        void whenGetJobStatus_thenReturnsStatus() {
                IndexJob mockJob = new IndexJob();
                mockJob.setId(100L);
                mockJob.setUserId(USER_ID);
                mockJob.setStatus("completed");

                when(indexJobRepository.findById(100L)).thenReturn(Optional.of(mockJob));

                ResponseEntity<IndexJobResponse> response = scipController.getJobStatus(userDetails, 100L);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("completed", response.getBody().getStatus());
        }
}
