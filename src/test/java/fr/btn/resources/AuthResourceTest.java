package fr.btn.resources;

import fr.btn.WireMockMailService;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import fr.btn.utils.Utils;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.auth.User;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
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

    private static String mockUsername = "TestUsername";
    private static String mockPassword = "abcd1234";
    private static String mockEmail = "test@mail.com";
    private static String mockEncodedActivationCode;

    @BeforeAll
    static void init() {

    }

    @BeforeEach
    void setUp() {

        //Mockito.when(userRepository.findUserByEmail(""))
    }

    @Test
    @Order(1)
    void testRegisterWithValidData() {
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
        mockEncodedActivationCode = Utils.generateEncodedStringWithUserData(userData);
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
                .formParam("username", mockUsername)
                .formParam("password", mockPassword)
                .when()
                .post(ENDPOINT + "login")
                .then()
                .contentType("text/plain")
                .statusCode(200)
                .body(is("Please check your email and confirm your login."));
    }



    @Test
    void confirmLogin() {
    }

    @Test
    void confirmResetPassword() {
    }
}