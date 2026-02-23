# Spring Boot Testing Guide — MindVex Backend

> **Version:** Spring Boot 3.2.1 · Java 21 · JUnit 5 · Mockito  
> **Audience:** Engineering team contributing to `mindvex-backend`

---

## Table of Contents

1. [Testing Strategy Overview](#1-testing-strategy-overview)
2. [Dependencies Setup](#2-dependencies-setup)
3. [Test Types at a Glance](#3-test-types-at-a-glance)
4. [Unit Tests — Service Layer](#4-unit-tests--service-layer)
5. [Controller Slice Tests — `@WebMvcTest`](#5-controller-slice-tests--webmvctest)
6. [Repository Tests — `@DataJpaTest`](#6-repository-tests--datajpatest)
7. [Full Integration Tests — `@SpringBootTest`](#7-full-integration-tests--springboottest)
8. [Test Configuration & Profiles](#8-test-configuration--profiles)
9. [Best Practices](#9-best-practices)
10. [Project Directory Layout](#10-project-directory-layout)

---

## 1. Testing Strategy Overview

The testing pyramid guides how many tests of each type to write:

```
         ╱  E2E / Full Integration  ╲     ← fewest, slowest
        ╱  Controller Slice Tests    ╲
       ╱  Repository Slice Tests      ╲
      ╱  Unit Tests (Service Layer)    ╲  ← most, fastest
```

| Layer        | Scope                              | Speed    | Spring Context |
|--------------|-------------------------------------|----------|----------------|
| **Unit**     | Single class, mocked dependencies   | ⚡ Fast   | ❌ None         |
| **Slice**    | One layer (web, data) with partial context | 🟡 Medium | ⚠️ Partial     |
| **Integration** | Full application context          | 🐢 Slow  | ✅ Full         |

---

## 2. Dependencies Setup

The project's `pom.xml` already includes the required test dependencies:

```xml
<!-- Testing (includes JUnit 5, Mockito, AssertJ, Spring Test, JSONPath) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Security test support (@WithMockUser, SecurityMockMvc, etc.) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### What `spring-boot-starter-test` Provides

| Library           | Purpose                                              |
|-------------------|------------------------------------------------------|
| **JUnit 5**       | Test framework — `@Test`, `@BeforeEach`, `@DisplayName` |
| **Mockito**       | Mocking framework — `@Mock`, `@InjectMocks`, `when()`, `verify()` |
| **AssertJ**       | Fluent assertions — `assertThat(x).isEqualTo(y)`     |
| **Spring Test**   | Slice annotations — `@WebMvcTest`, `@DataJpaTest`    |
| **Hamcrest**      | Matcher library — `is()`, `hasSize()`                |
| **JSONPath**      | JSON response assertions — `$.name`, `$.id`          |
| **Mockito JUnit5**| `MockitoExtension` for JUnit 5 integration           |

### Additional Dependency for Repository Slice Tests (H2)

Add this to `pom.xml` if not already present to enable in-memory database testing:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 3. Test Types at a Glance

| Annotation             | What It Loads                      | Use Case                             | Needs DB? |
|------------------------|------------------------------------|--------------------------------------|-----------|
| `@ExtendWith(MockitoExtension.class)` | Nothing (plain JUnit) | Service / utility class unit tests   | No        |
| `@WebMvcTest(Controller.class)`       | Web layer only         | Controller endpoint testing          | No        |
| `@DataJpaTest`          | JPA layer + embedded DB           | Repository query verification        | Yes (H2)  |
| `@SpringBootTest`       | Full application context          | End-to-end / integration testing     | Yes       |

---

## 4. Unit Tests — Service Layer

Unit tests validate **business logic in isolation**. Dependencies are mocked with Mockito — no Spring context is loaded.

### 4.1 Example: `WorkspaceServiceTest`

```java
package ai.mindvex.backend.service;

import ai.mindvex.backend.dto.WorkspaceRequest;
import ai.mindvex.backend.dto.WorkspaceResponse;
import ai.mindvex.backend.entity.Workspace;
import ai.mindvex.backend.exception.ResourceNotFoundException;
import ai.mindvex.backend.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)           // ① Enables Mockito annotations
@DisplayName("WorkspaceService Unit Tests")
class WorkspaceServiceTest {

    @Mock                                      // ② Creates a mock instance
    private WorkspaceRepository workspaceRepository;

    @InjectMocks                               // ③ Injects mocks into the service
    private WorkspaceService workspaceService;

    private Workspace testWorkspace;
    private WorkspaceRequest testRequest;
    private final Long USER_ID = 1L;
    private final Long WORKSPACE_ID = 100L;

    @BeforeEach                                // ④ Runs before each test method
    void setUp() {
        testWorkspace = Workspace.builder()
                .id(WORKSPACE_ID)
                .userId(USER_ID)
                .name("My Workspace")
                .description("Test workspace")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = new WorkspaceRequest();
        testRequest.setName("My Workspace");
        testRequest.setDescription("Test workspace");
    }

    // ---- CREATE ----

    @Test
    @DisplayName("createWorkspace: when valid request, then returns saved workspace response")
    void whenValidRequest_thenCreateWorkspaceSuccessfully() {
        // Arrange
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        // Act
        WorkspaceResponse response = workspaceService.createWorkspace(testRequest, USER_ID);

        // Assert
        assertNotNull(response);
        assertEquals("My Workspace", response.getName());
        assertEquals(USER_ID, response.getUserId());
        verify(workspaceRepository, times(1)).save(any(Workspace.class));  // ⑤ Verify interaction
    }

    // ---- READ ----

    @Test
    @DisplayName("getAllUserWorkspaces: when user has workspaces, then returns list")
    void whenUserHasWorkspaces_thenReturnWorkspaceList() {
        when(workspaceRepository.findByUserId(USER_ID)).thenReturn(List.of(testWorkspace));

        List<WorkspaceResponse> workspaces = workspaceService.getAllUserWorkspaces(USER_ID);

        assertFalse(workspaces.isEmpty());
        assertEquals(1, workspaces.size());
        assertEquals("My Workspace", workspaces.get(0).getName());
    }

    @Test
    @DisplayName("getWorkspaceById: when workspace exists, then returns response")
    void whenWorkspaceExists_thenReturnWorkspace() {
        when(workspaceRepository.findByIdAndUserId(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of(testWorkspace));

        WorkspaceResponse response = workspaceService.getWorkspaceById(WORKSPACE_ID, USER_ID);

        assertNotNull(response);
        assertEquals(WORKSPACE_ID, response.getId());
    }

    @Test
    @DisplayName("getWorkspaceById: when workspace not found, then throws ResourceNotFoundException")
    void whenWorkspaceNotFound_thenThrowException() {
        when(workspaceRepository.findByIdAndUserId(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.getWorkspaceById(WORKSPACE_ID, USER_ID));
    }

    // ---- UPDATE ----

    @Test
    @DisplayName("updateWorkspace: when workspace exists, then updates and returns response")
    void whenWorkspaceExists_thenUpdateSuccessfully() {
        WorkspaceRequest updateRequest = new WorkspaceRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated description");

        when(workspaceRepository.findByIdAndUserId(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        WorkspaceResponse response = workspaceService.updateWorkspace(
                WORKSPACE_ID, updateRequest, USER_ID);

        assertNotNull(response);
        verify(workspaceRepository).save(any(Workspace.class));
    }

    // ---- DELETE ----

    @Test
    @DisplayName("deleteWorkspace: when workspace exists, then deletes successfully")
    void whenWorkspaceExists_thenDeleteSuccessfully() {
        when(workspaceRepository.findByIdAndUserId(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of(testWorkspace));
        doNothing().when(workspaceRepository).delete(testWorkspace);

        assertDoesNotThrow(() -> workspaceService.deleteWorkspace(WORKSPACE_ID, USER_ID));
        verify(workspaceRepository, times(1)).delete(testWorkspace);
    }

    @Test
    @DisplayName("deleteWorkspace: when workspace not found, then throws exception")
    void whenWorkspaceNotFound_thenDeleteThrowsException() {
        when(workspaceRepository.findByIdAndUserId(WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.deleteWorkspace(WORKSPACE_ID, USER_ID));
        verify(workspaceRepository, never()).delete(any());
    }
}
```

### Key Annotations Explained

| Annotation        | Purpose                                                |
|--------------------|-------------------------------------------------------|
| `@ExtendWith(MockitoExtension.class)` | Initializes `@Mock` and `@InjectMocks` without Spring context |
| `@Mock`            | Creates a mock (fake) implementation of a dependency   |
| `@InjectMocks`     | Creates the real object and injects all `@Mock` fields |
| `@BeforeEach`      | Runs common setup before every `@Test` method          |
| `@DisplayName`     | Provides a human-readable name in test reports         |

### Key Mockito Methods

| Method                            | Purpose                                    |
|-----------------------------------|--------------------------------------------|
| `when(mock.method()).thenReturn(x)` | Stubs a method to return a specific value |
| `doNothing().when(mock).method()`   | Stubs a void method                      |
| `verify(mock, times(n)).method()`   | Asserts a method was called `n` times    |
| `verify(mock, never()).method()`    | Asserts a method was never called         |
| `any(Class.class)`                  | Matches any argument of the given type   |
| `assertThrows(Exception.class, () -> ...)` | Asserts an exception is thrown    |

---

## 5. Controller Slice Tests — `@WebMvcTest`

Slice tests load **only the web layer** (controllers, filters, security config) without the full application context. Service dependencies are replaced with `@MockBean`.

### 5.1 Example: `WorkspaceControllerTest`

```java
package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.WorkspaceRequest;
import ai.mindvex.backend.dto.WorkspaceResponse;
import ai.mindvex.backend.entity.User;
import ai.mindvex.backend.repository.UserRepository;
import ai.mindvex.backend.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)            // ① Loads only this controller
@DisplayName("WorkspaceController Slice Tests")
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;                       // ② Simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper;             // ③ JSON serialization

    @MockBean                                      // ④ Replaces bean in Spring context
    private WorkspaceService workspaceService;

    @MockBean
    private UserRepository userRepository;

    private WorkspaceResponse testResponse;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Stub the user repository to resolve authentication
        User testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("test@example.com");
        when(userRepository.findByEmail("user")).thenReturn(Optional.of(testUser));

        testResponse = WorkspaceResponse.builder()
                .id(100L)
                .userId(USER_ID)
                .name("My Workspace")
                .description("Test workspace")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "user")               // ⑤ Simulates authenticated user
    @DisplayName("POST /api/workspaces: when valid request, then returns 201 CREATED")
    void whenValidRequest_thenCreateWorkspaceReturns201() throws Exception {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("My Workspace");
        request.setDescription("Test workspace");

        when(workspaceService.createWorkspace(any(WorkspaceRequest.class), eq(USER_ID)))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/workspaces")
                        .with(csrf())                  // ⑥ Include CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Workspace"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("GET /api/workspaces: when user has workspaces, then returns 200 with list")
    void whenUserHasWorkspaces_thenReturnsList() throws Exception {
        when(workspaceService.getAllUserWorkspaces(USER_ID))
                .thenReturn(List.of(testResponse));

        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Workspace"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("GET /api/workspaces/{id}: when workspace exists, then returns 200")
    void whenWorkspaceExists_thenReturnsWorkspace() throws Exception {
        when(workspaceService.getWorkspaceById(100L, USER_ID)).thenReturn(testResponse);

        mockMvc.perform(get("/api/workspaces/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("My Workspace"));
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("DELETE /api/workspaces/{id}: when workspace exists, then returns 204")
    void whenWorkspaceExists_thenDeleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/workspaces/100")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/workspaces: when unauthenticated, then returns 401")
    void whenUnauthenticated_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isUnauthorized());
    }
}
```

### `@MockBean` vs `@Mock`

| Feature        | `@Mock`                       | `@MockBean`                          |
|----------------|-------------------------------|--------------------------------------|
| Context        | No Spring context needed       | Requires Spring context              |
| Scope          | Plain JUnit + Mockito          | Spring Test slice (`@WebMvcTest`, etc.) |
| Injection      | Via `@InjectMocks`             | Replaces/adds bean in ApplicationContext |
| Use case       | Service unit tests             | Controller / integration tests       |

---

## 6. Repository Tests — `@DataJpaTest`

Repository slice tests load **only the JPA layer** with an embedded H2 database. They verify that custom queries, entity mappings, and constraints work correctly.

### 6.1 Test Profile Configuration

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false            # Disable Flyway for H2 tests
```

### 6.2 Example: `WorkspaceRepositoryTest`

```java
package ai.mindvex.backend.repository;

import ai.mindvex.backend.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest                                        // ① Loads only JPA components + H2
@ActiveProfiles("test")                             // ② Uses application-test.yml
@DisplayName("WorkspaceRepository Integration Tests")
class WorkspaceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;        // ③ JPA test helper (persist, flush, find)

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace1;
    private Workspace workspace2;

    @BeforeEach
    void setUp() {
        workspace1 = Workspace.builder()
                .userId(1L)
                .name("Project Alpha")
                .description("First workspace")
                .build();

        workspace2 = Workspace.builder()
                .userId(1L)
                .name("Project Beta")
                .description("Second workspace")
                .build();

        entityManager.persistAndFlush(workspace1);
        entityManager.persistAndFlush(workspace2);
    }

    @Test
    @DisplayName("findByUserId: when user has workspaces, then returns all matching")
    void whenUserHasWorkspaces_thenReturnAll() {
        List<Workspace> workspaces = workspaceRepository.findByUserId(1L);

        assertThat(workspaces)
                .hasSize(2)
                .extracting(Workspace::getName)
                .containsExactlyInAnyOrder("Project Alpha", "Project Beta");
    }

    @Test
    @DisplayName("findByUserId: when user has no workspaces, then returns empty list")
    void whenUserHasNoWorkspaces_thenReturnEmptyList() {
        List<Workspace> workspaces = workspaceRepository.findByUserId(999L);

        assertThat(workspaces).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndUserId: when workspace exists for user, then returns it")
    void whenWorkspaceExistsForUser_thenReturnWorkspace() {
        Optional<Workspace> found = workspaceRepository
                .findByIdAndUserId(workspace1.getId(), 1L);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Project Alpha");
    }

    @Test
    @DisplayName("findByIdAndUserId: when workspace belongs to different user, then returns empty")
    void whenWorkspaceBelongsToDifferentUser_thenReturnEmpty() {
        Optional<Workspace> found = workspaceRepository
                .findByIdAndUserId(workspace1.getId(), 999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("save: when valid workspace, then persists with generated ID")
    void whenValidWorkspace_thenPersistsWithId() {
        Workspace newWorkspace = Workspace.builder()
                .userId(2L)
                .name("New Project")
                .description("Brand new workspace")
                .build();

        Workspace saved = workspaceRepository.save(newWorkspace);

        assertThat(saved.getId()).isNotNull();
        assertThat(entityManager.find(Workspace.class, saved.getId())).isNotNull();
    }
}
```

### Key Annotations

| Annotation            | Purpose                                           |
|------------------------|--------------------------------------------------|
| `@DataJpaTest`         | Auto-configures JPA, embedded DB, and rolls back after each test |
| `@ActiveProfiles("test")` | Loads `application-test.yml` for H2 config    |
| `TestEntityManager`    | Provides `persist`, `flush`, `find` for test data setup |

> [!IMPORTANT]
> `@DataJpaTest` automatically rolls back transactions after each test, ensuring test isolation without manual cleanup.

---

## 7. Full Integration Tests — `@SpringBootTest`

Full integration tests load the **entire application context** and can test multiple layers together. Use sparingly; they are the slowest test type.

### 7.1 Example: `WorkspaceIntegrationTest`

```java
package ai.mindvex.backend;

import ai.mindvex.backend.dto.WorkspaceRequest;
import ai.mindvex.backend.dto.WorkspaceResponse;
import ai.mindvex.backend.service.WorkspaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest                                     // ① Full context
@ActiveProfiles("test")                             // ② H2 database
@Transactional                                      // ③ Rollback after each test
@DisplayName("Workspace Integration Tests")
class WorkspaceIntegrationTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("Full CRUD flow: create → read → update → delete")
    void whenFullCrudFlow_thenAllOperationsSucceed() {
        // Create
        WorkspaceRequest createRequest = new WorkspaceRequest();
        createRequest.setName("Integration Workspace");
        createRequest.setDescription("Created during integration test");

        WorkspaceResponse created = workspaceService.createWorkspace(createRequest, 1L);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Integration Workspace");

        // Read
        List<WorkspaceResponse> allWorkspaces = workspaceService.getAllUserWorkspaces(1L);
        assertThat(allWorkspaces).hasSizeGreaterThanOrEqualTo(1);

        // Update
        WorkspaceRequest updateRequest = new WorkspaceRequest();
        updateRequest.setName("Updated Workspace");
        updateRequest.setDescription("Updated during integration test");

        WorkspaceResponse updated = workspaceService.updateWorkspace(
                created.getId(), updateRequest, 1L);
        assertThat(updated.getName()).isEqualTo("Updated Workspace");

        // Delete
        workspaceService.deleteWorkspace(created.getId(), 1L);
        List<WorkspaceResponse> afterDelete = workspaceService.getAllUserWorkspaces(1L);
        assertThat(afterDelete).noneMatch(w -> w.getId().equals(created.getId()));
    }
}
```

---

## 8. Test Configuration & Profiles

### 8.1 Profile-Based Isolation

Use `@ActiveProfiles("test")` to load test-specific configuration, keeping tests isolated from your production PostgreSQL database.

```
src/
├── main/resources/
│   └── application.yml               ← Production (PostgreSQL)
└── test/resources/
    └── application-test.yml           ← Test profile (H2 in-memory)
```

### 8.2 Disabling Unnecessary Auto-Configuration

For slimmer test contexts, exclude configurations you don't need:

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost"
})
class LightweightIntegrationTest {
    // ...
}
```

---

## 9. Best Practices

### 9.1 General Rules

| Rule | Description |
|------|-------------|
| **One behavior per test** | Each `@Test` method should verify exactly one behavior or scenario |
| **Descriptive naming** | Use `whenCondition_thenExpectedResult` pattern for method names |
| **Arrange-Act-Assert** | Structure every test into three clear sections |
| **Fast and isolated** | Tests must not depend on each other or on external services |
| **No production DB** | Always use `@ActiveProfiles("test")` with H2 for data layer tests |

### 9.2 Choosing the Right Test Type

```
                           ┌──────────────────────────┐
                           │  What are you testing?    │
                           └────────────┬─────────────┘
                  ┌─────────────────────┼─────────────────────┐
                  ▼                     ▼                     ▼
          Business logic         HTTP endpoints        JPA queries
          (Service layer)        (Controllers)         (Repositories)
                  │                     │                     │
                  ▼                     ▼                     ▼
        @ExtendWith(Mockito)     @WebMvcTest           @DataJpaTest
        + @Mock / @InjectMocks   + MockMvc             + TestEntityManager
                                 + @MockBean           + H2
```

### 9.3 Common Pitfalls to Avoid

> [!CAUTION]
> **Anti-pattern:** Using `@SpringBootTest` for every test.  
> This loads the full context each time and makes your test suite extremely slow.  
> Use `@ExtendWith(MockitoExtension.class)` for service tests and `@WebMvcTest` / `@DataJpaTest` for slice tests.

> [!WARNING]
> **Anti-pattern:** Testing implementation details instead of behavior.  
> Don't verify every internal method call. Focus on **inputs → outputs** and **side effects** (like verifying `save()` was called).

> [!TIP]
> **Use `@BeforeEach` for shared setup**, but keep test-specific data inside the test method to maintain readability and prevent hidden dependencies between tests.

### 9.4 Naming Convention Examples

```java
// ✅ Good — clearly describes the scenario and expectation
void whenWorkspaceNotFound_thenThrowResourceNotFoundException()
void whenValidRequest_thenCreateWorkspaceReturns201()
void whenUnauthenticated_thenReturns401()

// ❌ Bad — vague, doesn't describe what's being tested
void test1()
void testWorkspace()
void createWorkspaceTest()
```

---

## 10. Project Directory Layout

Tests must **mirror** the main source package structure under `src/test/java`:

```
src/
├── main/java/ai/mindvex/backend/
│   ├── controller/
│   │   └── WorkspaceController.java
│   ├── service/
│   │   └── WorkspaceService.java
│   ├── repository/
│   │   └── WorkspaceRepository.java
│   └── entity/
│       └── Workspace.java
│
├── test/java/ai/mindvex/backend/
│   ├── controller/
│   │   └── WorkspaceControllerTest.java        ← @WebMvcTest
│   ├── service/
│   │   └── WorkspaceServiceTest.java           ← @ExtendWith(MockitoExtension)
│   ├── repository/
│   │   └── WorkspaceRepositoryTest.java        ← @DataJpaTest
│   └── WorkspaceIntegrationTest.java           ← @SpringBootTest
│
└── test/resources/
    └── application-test.yml                     ← H2 config for test profile
```

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=WorkspaceServiceTest

# Run a specific test method
./mvnw test -Dtest="WorkspaceServiceTest#whenValidRequest_thenCreateWorkspaceSuccessfully"

# Run tests with verbose output
./mvnw test -Dsurefire.useFile=false

# Run only unit tests (by naming convention)
./mvnw test -Dtest="**/*Test"

# Generate test report
./mvnw surefire-report:report
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT TESTING CHEAT SHEET                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UNIT TEST (fastest)              CONTROLLER SLICE                      │
│  ─────────────────                ────────────────                      │
│  @ExtendWith(MockitoExtension)    @WebMvcTest(Controller.class)         │
│  @Mock + @InjectMocks             @MockBean + MockMvc                   │
│  when(...).thenReturn(...)        mockMvc.perform(get/post/put/delete)   │
│  verify(mock, times(n))          .andExpect(status().isOk())            │
│  assertEquals / assertThrows     .andExpect(jsonPath("$.key").value(x)) │
│                                                                         │
│  REPOSITORY SLICE                 FULL INTEGRATION                      │
│  ────────────────                 ────────────────                      │
│  @DataJpaTest                     @SpringBootTest                       │
│  @ActiveProfiles("test")          @ActiveProfiles("test")               │
│  TestEntityManager                @Transactional                        │
│  assertThat(...).hasSize(n)       Real beans, real flow                  │
│                                                                         │
│  SECURITY                                                               │
│  ────────                                                               │
│  @WithMockUser(username = "x")    .with(csrf())                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```
