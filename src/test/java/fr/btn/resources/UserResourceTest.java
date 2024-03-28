package fr.btn.resources;

import fr.btn.WireMockMailService;
import fr.btn.dtos.UserDataForm;
import fr.btn.dtos.UserDto;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import fr.btn.securityUtils.TokenUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.auth.User;
import jakarta.transaction.Transactional;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(WireMockMailService.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class UserResourceTest {
    private static final String ENDPOINT = "/users/";
    private static String userToken;
    private static String adminToken;
    private static UserEntity mockUser1;
    private static UserEntity mockUser2;
    private static UserEntity mockAdmin;
    @InjectMock
    UserRepository userRepository;

    @BeforeAll
    static void init() {
        userToken = TokenUtil.generateJwt("TestUser", "USER");
        adminToken = TokenUtil.generateJwt("TestAdmin", "ADMIN");
    }

    @BeforeEach
    void setUp() {
        String hashedPassword = Argon2.getHashedPassword("testPassword");

        mockUser1 = UserEntity
                .builder()
                .id(1)
                .username("TestUser")
                .password(hashedPassword)
                .email("testUser1@mail.com")
                .role("USER")
                .status("ACTIVE")
                .build();

        mockUser2 = UserEntity
                .builder()
                .id(2)
                .username("TestUser2")
                .password(hashedPassword)
                .email("testUser2@mail.com")
                .role("USER")
                .status("ACTIVE")
                .numFailAttempts(3)
                .lastAccess(LocalDateTime.now())
                .build();

        mockAdmin = UserEntity
                .builder()
                .id(3)
                .username("TestAdmin")
                .password(hashedPassword)
                .email("testAdmin@mail.com")
                .role("ADMIN")
                .status("ACTIVE")
                .build();

        Mockito.when(userRepository.listAll()).thenReturn(Arrays.asList(mockUser1, mockAdmin));
        Mockito.when(userRepository.findUserByUsername("TestUser")).thenReturn(mockUser1);
        Mockito.when(userRepository.findUserByUsername("TestUser2")).thenReturn(mockUser2);
        Mockito.when(userRepository.findUserByEmail(mockUser1.getEmail())).thenReturn(mockUser1);
    }
    @Test
    @Order(1)
    void testGetAllWithAdminToken() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(ENDPOINT + "all_users")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("size()", is(2));
    }

    @Test
    @Order(2)
    void testGetAllWithUserToken() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(ENDPOINT + "all_users")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    void testGetAllWithNoToken() {
        given()
                .when()
                .get(ENDPOINT + "all_users")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(4)
    void promoteUserWithAdminTokenAndValidUsername() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .put(ENDPOINT + "promote?username=TestUser")
                .then()
                .statusCode(200);

        assertEquals("ADMIN", mockUser1.getRole());
    }

    @Test
    @Order(5)
    void promoteUserWithUserTokenAndValidUsername() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .put(ENDPOINT + "promote?username=TestUser")
                .then()
                .statusCode(403);

        assertEquals("USER", mockUser1.getRole());
    }

    @Test
    @Order(6)
    void promoteInvalidUsername() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .put(ENDPOINT + "promote?username=.Test")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .put(ENDPOINT + "promote?username=FalseUser")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @Order(7)
    void testRequestResetPasswordWithValidData() {
        given()
                .formParam("new_password", "newPassword")
                .when()
                .post(ENDPOINT + "TestUser/forgot_password")
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Please confirm your request to reset password by clicking on the link that has been sent to your email."));
    }

    @Test
    @Order(8)
    void testRequestResetPasswordForUserWithInvalidAccess() {
        String message = given()
                            .formParam("new_password", "newPassword")
                            .when()
                            .post(ENDPOINT + "TestUser2/forgot_password")
                            .then()
                            .contentType("text/plain")
                            .statusCode(HttpStatus.SC_FORBIDDEN)
                            .extract()
                            .body()
                            .asString();

        assertThat("Error message is correct.", message.startsWith("Your account is locked until "));
    }

    @Test
    @Order(9)
    void testRequestResetPasswordWithInvalidNewPassword() {
        given()
                .formParam("new_password", "abc") // password is too short
                .when()
                .post(ENDPOINT + "TestUser/forgot_password")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Password is invalid."));
    }

    @Test
    @Order(10)
    void testRequestResetPasswordWithNonExistentUsername() {
        given()
                .formParam("new_password", "newPassword") // password is too short
                .when()
                .post(ENDPOINT + "FalseUser/forgot_password")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(is("User does not exist."));
    }

    @Test
    @Order(11)
    void testRequestUsernameWithValidEmail() {
        given()
                .formParam("email", mockUser1.getEmail())
                .when()
                .post(ENDPOINT + "forgot_username")
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Please check your email to retrieve your username."));
    }

    @Test
    @Order(12)
    void testRequestUsernameWithInValidEmail() {
        given()
                .formParam("email", "invalidMail")
                .when()
                .post(ENDPOINT + "forgot_username")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("A valid email is required."));

        given()
                .formParam("email", "falseMail@mail.com")
                .when()
                .post(ENDPOINT + "forgot_username")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(is("User not found."));
    }

    @Test
    @Order(13)
    void testModifyPasswordWithValidData() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .formParam("password", "testPassword")
                .formParam("new_password", "newPassword")
                .when()
                .post(ENDPOINT + "TestUser/modify_password")
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Please confirm your request to reset password by clicking on the link that has been sent to your email."));
    }

    @Test
    @Order(14)
    void testModifyPasswordWithInvalidUsername() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .formParam("password", "testPassword")
                .formParam("new_password", "newPassword")
                .when()
                .post(ENDPOINT + "Test User/modify_password")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid Username."));
    }

    @Test
    @Order(15)
    void testModifyPasswordWithInvalidNewPassword() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .formParam("password", "testPassword")
                .formParam("new_password", "123") // password is too short
                .when()
                .post(ENDPOINT + "TestUser/modify_password")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("New password is invalid."));
    }

    @Test
    @Order(16)
    void testModifyPasswordForNonExistentUser() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .formParam("password", "testPassword")
                .formParam("new_password", "newPassword")
                .when()
                .post(ENDPOINT + "FalseUser/modify_password")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(is("User not found."));
    }

    @Test
    @Order(17)
    void testModifyPasswordForUserWithInvalidAccess() {
        String message = given()
                            .header("Authorization", "Bearer " + userToken)
                            .formParam("password", "testPassword")
                            .formParam("new_password", "newPassword")
                            .when()
                            .post(ENDPOINT + "TestUser2/modify_password") // this mock user has invalid access
                            .then()
                            .contentType("text/plain")
                            .statusCode(HttpStatus.SC_FORBIDDEN)
                            .extract()
                            .body()
                            .asString();

        assertThat("Error message is correct.", message.startsWith("Your account is locked until "));
    }

    @Test
    @Order(18)
    void testModifyPasswordForUserWithIncorrectPassword() {
            given()
                .header("Authorization", "Bearer " + userToken)
                .formParam("password", "jkhdfkjsh") // incorrect password
                .formParam("new_password", "newPassword")
                .when()
                .post(ENDPOINT + "TestUser/modify_password") // this mock user has invalid access
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(is("Incorrect Password."));

            assertEquals(1, mockUser1.getNumFailAttempts());
    }

    @Test
    @Order(19)
    void testModifyUserDataWithValidData() {
        UserDataForm dataForm = UserDataForm
                .builder()
                .firstName("New First Name")
                .lastName("New Last Name")
                .birthdate(LocalDate.parse("1995-10-11"))
                .build();

        UserDto updatedUser = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body(dataForm)
                .when()
                .put(ENDPOINT + "TestUser/modify_data")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .body()
                .as(UserDto.class);

        assertEquals(dataForm.getFirstName(), updatedUser.getFirstName());
        assertEquals(dataForm.getLastName(), updatedUser.getLastName());
        assertEquals(dataForm.getBirthdate(), updatedUser.getBirthdate());

        assertEquals(dataForm.getFirstName(), mockUser1.getFirstName());
        assertEquals(dataForm.getLastName(), mockUser1.getLastName());
        assertEquals(dataForm.getBirthdate(), mockUser1.getBirthdate());
    }

    @Test
    @Order(20)
    void testModifyUserDataWithInvalidUsername() {
        UserDataForm dataForm = UserDataForm
                .builder()
                .firstName("New First Name")
                .lastName("New Last Name")
                .birthdate(LocalDate.parse("1995-10-11"))
                .build();

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .body(dataForm)
                .when()
                .put(ENDPOINT + ".DFS/modify_data") // Invalid Username
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid Username."));
    }

    @Test
    @Order(21)
    void testModifyUserDataWithNoDataForm() {

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType("application/json")
                .when()
                .put(ENDPOINT + "TestUser/modify_data") // Invalid Username
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid data."));
    }

    @Test
    @Order(22)
    void testModifyDataForNonExistentUser() {
        String mockToken = TokenUtil.generateJwt("FalseUser", "USER");

        UserDataForm dataForm = UserDataForm
                .builder()
                .firstName("New First Name")
                .lastName("New Last Name")
                .birthdate(LocalDate.parse("1995-10-11"))
                .build();

        given()
                .header("Authorization", "Bearer " + mockToken)
                .contentType("application/json")
                .body(dataForm)
                .when()
                .put(ENDPOINT + "FalseUser/modify_data") // Invalid Username
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @Order(23)
    void testModifyUserDataWithInvalidAccess() {
        String user2Token = TokenUtil.generateJwt("TestUser2", "USER");

        UserDataForm dataForm = UserDataForm
                .builder()
                .firstName("New First Name")
                .lastName("New Last Name")
                .birthdate(LocalDate.parse("1995-10-11"))
                .build();

        String message = given()
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType("application/json")
                            .body(dataForm)
                            .when()
                            .put(ENDPOINT + "TestUser2/modify_data") // Invalid Username
                            .then()
                            .contentType("text/plain")
                            .statusCode(HttpStatus.SC_FORBIDDEN)
                            .extract()
                            .body()
                            .asString();

        assertThat("Error message is correct.", message.startsWith("Your account is locked until "));
    }
}