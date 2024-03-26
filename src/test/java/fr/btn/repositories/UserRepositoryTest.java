package fr.btn.repositories;

import fr.btn.entities.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.auth.User;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class UserRepositoryTest {
    @Inject
    UserRepository userRepository;

    private static UserEntity testUser;

    @BeforeAll
    static void init() {
        testUser = UserEntity
                .builder()
                .username("TEST USER")
                .password("TEST PASSWORD")
                .email("test@mail.com")
                .role("USER")
                .status("ACTIVE")
                .build();
    }

    @Test
    @Order(1)
    void persist() {
        userRepository.persist(testUser);

        assertTrue(userRepository.isPersistent(testUser));
    }

    @Test
    @Order(2)
    void findUserByUsername() {
        UserEntity found = userRepository.findUserByUsername(testUser.getUsername());

        assertNotNull(found);
        assertEquals(testUser.getUsername(), found.getUsername());
    }

    @Test
    @Order(3)
    void findUserByEmail() {
        UserEntity found = userRepository.findUserByEmail(testUser.getEmail());

        assertNotNull(found);
        assertEquals(testUser.getEmail(), found.getEmail());
    }

    @Test
    @Order(4)
    void countByUsername() {
        long count = userRepository.countByUsername(testUser.getUsername());

        assertEquals(count, 1);
    }

    @Test
    @Order(5)
    void deleteById() {
        userRepository.deleteById(testUser.getId());

        UserEntity deleted = userRepository.findById(testUser.getId());

        assertNull(deleted);
    }
}