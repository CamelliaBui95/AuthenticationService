package fr.btn.resources;

import fr.btn.TestUtils;
import fr.btn.WireMockMailService;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
@QuarkusTest
@QuarkusTestResource(WireMockMailService.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class AuthResourceTest {
    private static final String ENDPOINT = "/auth/";
    @InjectMock
    UserRepository userRepository;
    private static String mockEncodedActivationCode;

    private static UserEntity mockUser1;

    private static UserEntity mockUser2;

    private static UserEntity mockUser3;

    @BeforeAll
    static void init() {
        String hashedPassword1 = Argon2.getHashedPassword("testPassword1");
        String hashedPassword2 = Argon2.getHashedPassword("testPassword2");
        String hashedPassword3 = Argon2.getHashedPassword("testPassword3");

        mockUser1 = UserEntity
                .builder()
                .id(99)
                .username("TestUser1")
                .password(hashedPassword1)
                .email("test1@mail.com")
                .role("USER")
                .status("ACTIVE")
                .pinCode(9999)
                .lastAccess(LocalDateTime.now())
                .build();

        mockUser2 = UserEntity
                .builder()
                .id(100)
                .username("TestUser2")
                .password(hashedPassword2)
                .email("test2@mail.com")
                .role("USER")
                .status("ACTIVE")
                .pinCode(9998)
                .lastAccess(LocalDateTime.now())
                .numFailAttempts(5)
                .build();

        mockUser3 = UserEntity
                .builder()
                .id(101)
                .username("TestUser3")
                .password(hashedPassword3)
                .email("test3@mail.com")
                .role("USER")
                .status("ACTIVE")
                .pinCode(9997)
                .lastAccess(LocalDateTime.now().plusMinutes(-15))
                .build();

    }

    @BeforeEach
    void setUp() {
        Mockito.when(userRepository.findUserByUsername("TestUser1")).thenReturn(mockUser1);
        Mockito.when(userRepository.findUserByUsername("TestUser2")).thenReturn(mockUser2);
        Mockito.when(userRepository.findUserByUsername("TestUser3")).thenReturn(mockUser3);
        Mockito.when(userRepository.findUserByEmail("test2@mail.com")).thenReturn(mockUser2);
        Mockito.when(userRepository.countByUsername("TestUser2")).thenReturn(1L);
    }

    @Test
    @Order(1)
    void testRegisterWithValidData() {
        String mockUsername = "NewUser";
        String mockPassword = "abcd1234";
        String mockEmail = "testNewUser@mail.com";

        given()
                .formParam("email", mockEmail)
                .formParam("username", mockUsername)
                .formParam("password", mockPassword)
                .when()
                .post(ENDPOINT + "register")
                .then()
                .contentType("text/plain")
                .statusCode(201)
                .body(is("Please activate your account by clicking on the link that has been sent to your email."));

        String hashedPassword = Argon2.getHashedPassword(mockPassword);

        List<String> userData = Arrays.asList(mockUsername, hashedPassword, mockEmail);
        mockEncodedActivationCode = TestUtils.generateEncodedStringWithUserData(userData, 0);
    }

    @Test
    @Order(2)
    void testActivateAccountWithValidStringCode() {
        given()
                .when()
                .get(ENDPOINT + "account_confirm?code=" + mockEncodedActivationCode)
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Account has been successfully activated."));
    }

    @Test
    @Order(3)
    void testLoginWithValidAccount() {
        given()
                .formParam("username", "TestUser1")
                .formParam("password", "testPassword1")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Please check your email and confirm your login."));
    }

    @Test
    @Order(4)
    void testConfirmLoginWithValidData() {
        Response res = given()
                            .formParam("username", mockUser1.getUsername())
                            .formParam("password", "testPassword1")
                            .formParam("pin_code", mockUser1.getPinCode())
                            .when()
                            .post(ENDPOINT + "login_confirm")
                            .then()
                            .contentType("text/plain")
                            .statusCode(200)
                            .body(is("Access Granted."))
                            .extract()
                            .response();

        String token = res.getHeader(HttpHeaders.AUTHORIZATION);

        assertNotNull(token);
        assertThat("Token is not empty", !token.isEmpty());
    }

    @Test
    @Order(5)
    void testConfirmResetPasswordWithValidData() {
        String newPassword = "updatedPassword";
        String hashedNewPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList(mockUser1.getUsername(), hashedNewPassword);
        String confirmCode = TestUtils.generateEncodedStringWithUserData(data, 0);

        given()
                .when()
                .put(ENDPOINT + "confirm_reset_password?code=" + confirmCode)
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Password has been reset successfully."));

        assertThat("New password is saved", Argon2.validate(newPassword, mockUser1.getPassword()));
    }

    @Test
    @Order(6)
    void testRegisterWithExistingEmail() {
        String mockUsername = "NewUser";
        String mockPassword = "abcd1234";
        String mockEmail = "test2@mail.com";

        given()
                .formParam("email", mockEmail)
                .formParam("username", mockUsername)
                .formParam("password", mockPassword)
                .when()
                .post(ENDPOINT + "register")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("This email is already registered."));

    }

    @Test
    @Order(7)
    void testRegisterWithExistingUsername() {
        String mockUsername = "TestUser2";
        String mockPassword = "abcd1234";
        String mockEmail = "newMail@mail.com";

        given()
                .formParam("email", mockEmail)
                .formParam("username", mockUsername)
                .formParam("password", mockPassword)
                .when()
                .post(ENDPOINT + "register")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid Username."));

    }

    @Test
    @Order(8)
    void testRegisterWithInvalidPassword() {
        String mockUsername = "NewUser";
        String mockPassword = "abcd";
        String mockEmail = "newMail@mail.com";

        given()
                .formParam("email", mockEmail)
                .formParam("username", mockUsername)
                .formParam("password", mockPassword)
                .when()
                .post(ENDPOINT + "register")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid password."));

    }

    @Test
    @Order(9)
    void testLoginWithInvalidUsername() {
        given()
                .formParam("username", "")
                .formParam("password", "testPassword1")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Username is required."));

        given()
                .formParam("password", "testPassword1")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Username is required."));

    }

    @Test
    @Order(10)
    void testLoginWithInvalidPassword() {
        given()
                .formParam("username", "TestUser1")
                .formParam("password", "")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Password is required."));

        given()
                .formParam("username", "TestUser1")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Password is required."));
    }

    @Test
    @Order(11)
    void testLoginWithNonExistentUser() {
        given()
                .formParam("username", "FalseUser")
                .formParam("password", "abcd1234")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(is("User does not exist."));

    }

    @Test
    @Order(12)
    void testLoginWithInvalidAccess() {
        String message = given()
                            .formParam("username", "TestUser2")
                            .formParam("password", "testPassword2")
                            .when()
                            .post(ENDPOINT + "login")
                            .then()
                            .contentType("text/plain")
                            .statusCode(HttpStatus.SC_FORBIDDEN)
                            .extract()
                            .body()
                            .asString();

        assertThat("Error message is correct.", message.startsWith("Your account is locked until "));
    }

    @Test
    @Order(13)
    void testLoginWithIncorrectPassword() {
        given()
                .formParam("username", "TestUser1")
                .formParam("password", "testPassword123")
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Incorrect Password."));

        assertEquals(1, mockUser1.getNumFailAttempts());
    }

    @Test
    @Order(14)
    void testActivateAccountWithExpiredStringCode() {
        String mockUsername = "NewUser";
        String mockPassword = "abcd1234";
        String mockEmail = "testNewUser@mail.com";

        String hashedPassword = Argon2.getHashedPassword(mockPassword);

        List<String> userData = Arrays.asList(mockUsername, hashedPassword, mockEmail);
        String mockExpiredActivationCode = TestUtils.generateEncodedStringWithUserData(userData, -15);

        given()
                .when()
                .get(ENDPOINT + "account_confirm?code=" + mockExpiredActivationCode)
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("This code is expired."));
    }

    @Test
    @Order(15)
    void testActivateAccountInvalidStringCode() {
        String mockUsername = "NewUser";
        String mockEmail = "testNewUser@mail.com";

        List<String> userData = Arrays.asList(mockUsername, mockEmail);
        String mockExpiredActivationCode = TestUtils.generateEncodedStringWithUserData(userData, 0);

        given()
                .when()
                .get(ENDPOINT + "account_confirm?code=" + mockExpiredActivationCode)
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Invalid code"));
    }

    @Test
    @Order(16)
    void testActivateAccountWithInvalidUsername() {
        // Use an existing username to register
        String mockUsername = "TestUser2";
        String mockPassword = "abcd1234";
        String mockEmail = "testNewUser@mail.com";

        String hashedPassword = Argon2.getHashedPassword(mockPassword);

        List<String> userData = Arrays.asList(mockUsername, hashedPassword, mockEmail);
        String mockExpiredActivationCode = TestUtils.generateEncodedStringWithUserData(userData, 0);

        given()
                .when()
                .get(ENDPOINT + "account_confirm?code=" + mockExpiredActivationCode)
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("This account is expired."));
    }

    @Test
    @Order(17)
    void testConfirmLoginWithInvalidData() {
        given()
                .formParam("username", mockUser1.getUsername())
                .formParam("password", "testPassword1")
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("Invalid data."));

        given()
                .formParam("username", "NewUser")
                .formParam("password", "testPassword1")
                .formParam("pin_code", 9999)
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(is("User does not exist."));

    }

    @Test
    @Order(18)
    void testConfirmLoginWithInvalidAccess() {
        String message = given()
                            .formParam("username", mockUser2.getUsername())
                            .formParam("password", mockUser2.getPassword())
                            .formParam("pin_code", mockUser2.getPinCode())
                            .when()
                            .post(ENDPOINT + "login_confirm")
                            .then()
                            .contentType("text/plain")
                            .statusCode(HttpStatus.SC_FORBIDDEN)
                            .extract()
                            .body().asString();

        assertThat("Error message is correct.", message.startsWith("Your account is locked until "));
    }

    @Test
    @Order(19)
    void testConfirmLoginWithIncorrectPassword() {
        given()
                .formParam("username", mockUser1.getUsername())
                .formParam("password", "incorrectPassword")
                .formParam("pin_code", 9999)
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Incorrect Password."));

        assertEquals(2, mockUser1.getNumFailAttempts());
    }
    @Test
    @Order(20)
    void testConfirmLoginWithIncorrectPinCode() {
        given()
                .formParam("username", mockUser3.getUsername())
                .formParam("password", "testPassword3")
                .formParam("pin_code", 1100) // incorrect pinCode
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Incorrect Pin Code."));

        assertEquals(1, mockUser3.getNumFailAttempts());
    }


    @Test
    @Order(21)
    void testConfirmLoginWithExpiredPinCode() {
        given()
                .formParam("username", mockUser1.getUsername())
                .formParam("password", "updatedPassword") // password was updated in the previous test, from "testPassword1" to "updatedPassword".
                .formParam("pin_code", 9999) // the above confirmLogin test already cleared pin code in db so this code is considered expired.
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Code is expired."));

        given()
                .formParam("username", mockUser3.getUsername())
                .formParam("password", "testPassword3")
                .formParam("pin_code", 9997) // correct but expired pinCode
                .when()
                .post(ENDPOINT + "login_confirm")
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Code is expired."));
    }

    @Test
    @Order(22)
    void testConfirmResetPasswordWithInvalidUsername() {
        String newPassword = "password";
        String hashedNewPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList("", hashedNewPassword);
        String confirmCode = TestUtils.generateEncodedStringWithUserData(data, 0);

        given()
                .when()
                .put(ENDPOINT + "confirm_reset_password?code=" + confirmCode)
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("Invalid code."));
    }

    @Test
    @Order(23)
    void testConfirmResetPasswordWithNonExistingUsername() {
        String newPassword = "password";
        String hashedNewPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList("FalseUser", hashedNewPassword);
        String confirmCode = TestUtils.generateEncodedStringWithUserData(data, 0);

        given()
                .when()
                .put(ENDPOINT + "confirm_reset_password?code=" + confirmCode)
                .then()
                .contentType("text/plain")
                .statusCode(HttpStatus.SC_NOT_ACCEPTABLE)
                .body(is("User does not exist."));
    }


}