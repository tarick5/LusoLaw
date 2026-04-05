package com.lusolaw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lusolaw.model.User;
import com.lusolaw.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.seed.enabled=false",
        "app.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.security.rate-limit.login.max-attempts=100",
        "app.security.rate-limit.register.max-attempts=100",
        "stripe.api.key="
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void unauthenticatedUsersCannotCreateService() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Consulta de visto",
                                  "description": "Acompanhamento completo",
                                  "price": 350.00
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clientCannotPublishService() throws Exception {
        AuthSession client = registerUser("CLIENT");

        mockMvc.perform(post("/api/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Servico bloqueado",
                                  "description": "Cliente nao pode criar",
                                  "price": 220.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void lawyerCanPublishServiceAndPasswordIsNeverReturned() throws Exception {
        AuthSession lawyer = registerUser("LAWYER");

        assertThat(lawyer.user().has("password")).isFalse();

        mockMvc.perform(post("/api/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lawyer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Defesa de processo migratorio",
                                  "description": "Planeamento e execucao juridica",
                                  "price": 990.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lawyer.id").value(lawyer.userId()))
                .andExpect(jsonPath("$.name").value("Defesa de processo migratorio"));
    }

    @Test
    void lawyerCannotRespondToBookingOwnedByAnotherLawyer() throws Exception {
        AuthSession lawyerA = registerUser("LAWYER");
        AuthSession lawyerB = registerUser("LAWYER");
        AuthSession client = registerUser("CLIENT");

        long serviceId = createService(lawyerA.token(), "Pedido de residencia premium");
        long bookingId = createBooking(client.token(), serviceId);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/respond")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lawyerB.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "ACCEPT"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void publicRegisterCannotCreateAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MockMultipartFile identification = new MockMultipartFile(
                "identificationDocument",
                "id.pdf",
                "application/pdf",
                "fake-identification".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/register")
                        .file(identification)
                        .param("name", "Admin " + suffix)
                        .param("email", "admin." + suffix + "@test.local")
                        .param("password", "StrongPass!123")
                        .param("role", "ADMIN")
                        .param("identificationNumber", "DOC-" + suffix))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void lawyerCannotLoginBeforeAdminApproval() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "lawyer.pending." + suffix + "@test.local";
        String password = "StrongPass!123";

        MockMultipartFile identification = new MockMultipartFile(
                "identificationDocument",
                "id-" + suffix + ".pdf",
                "application/pdf",
                ("identification-" + suffix).getBytes()
        );
        MockMultipartFile lawyerCredential = new MockMultipartFile(
                "lawyerCredentialDocument",
                "oa-" + suffix + ".pdf",
                "application/pdf",
                ("credential-" + suffix).getBytes()
        );

        mockMvc.perform(multipart("/api/auth/register")
                        .file(identification)
                        .file(lawyerCredential)
                        .param("name", "Lawyer Pending " + suffix)
                        .param("email", email)
                        .param("password", password)
                        .param("role", "LAWYER")
                        .param("identificationNumber", "DOC-" + suffix)
                        .param("specialization", "Direito de Imigracao")
                        .param("pricePerHour", "120.00")
                        .param("lawyerRegistrationNumber", "OA-" + suffix))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private AuthSession registerUser(String role) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MockMultipartFile identification = new MockMultipartFile(
                "identificationDocument",
                "id-" + suffix + ".pdf",
                "application/pdf",
                ("identification-" + suffix).getBytes()
        );

        MockMultipartHttpServletRequestBuilder registerBuilder = multipart("/api/auth/register");
        registerBuilder.file(identification);
        registerBuilder.param("name", role + " " + suffix);
        registerBuilder.param("email", role.toLowerCase() + "." + suffix + "@test.local");
        registerBuilder.param("password", "StrongPass!123");
        registerBuilder.param("role", role);
        registerBuilder.param("identificationNumber", "DOC-" + suffix);

        if ("LAWYER".equals(role)) {
            MockMultipartFile lawyerCredential = new MockMultipartFile(
                    "lawyerCredentialDocument",
                    "oa-" + suffix + ".pdf",
                    "application/pdf",
                    ("credential-" + suffix).getBytes()
            );
            registerBuilder.file(lawyerCredential);
            registerBuilder.param("specialization", "Direito de Imigracao");
            registerBuilder.param("pricePerHour", "120.00");
            registerBuilder.param("lawyerRegistrationNumber", "OA-" + suffix);
        }

        int expectedStatus = "LAWYER".equals(role) ? 202 : 201;
        MvcResult result = mockMvc.perform(registerBuilder)
                .andExpect(status().is(expectedStatus))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        if ("LAWYER".equals(role)) {
            String email = role.toLowerCase() + "." + suffix + "@test.local";
            User lawyer = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            lawyer.setAccountStatus(User.AccountStatus.ACTIVE);
            userRepository.save(lawyer);
            return login(email, "StrongPass!123");
        }

        return new AuthSession(
                body.path("token").asText(),
                body.path("user").path("id").asLong(),
                body.path("user")
        );
    }

    private AuthSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthSession(
                body.path("token").asText(),
                body.path("user").path("id").asLong(),
                body.path("user")
        );
    }

    private long createService(String token, String serviceName) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", serviceName);
        payload.put("description", "Apoio juridico completo");
        payload.put("price", 450.00);

        MvcResult result = mockMvc.perform(post("/api/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private long createBooking(String token, long serviceId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("serviceId", serviceId);
        payload.put("situation", "Preciso regularizar a minha residencia");
        payload.put("details", "Tenho urgencia e preciso de acompanhamento no processo completo.");

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private record AuthSession(String token, long userId, JsonNode user) {
    }
}
