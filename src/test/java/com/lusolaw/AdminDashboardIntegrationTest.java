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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanReadDashboard() throws Exception {
        String token = createAdminAndLogin();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowDays").value(30))
                .andExpect(jsonPath("$.kpis.totalUsers").isNumber())
                .andExpect(jsonPath("$.kpis.totalLawyers").isNumber())
                .andExpect(jsonPath("$.kpis.totalRevenue").exists())
                .andExpect(jsonPath("$.revenueByMonth").isArray());
    }

    @Test
    void clientCannotReadDashboard() throws Exception {
        String token = registerClientAndLogin();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private String createAdminAndLogin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "admin." + suffix + "@test.local";
        String password = "StrongPass!12345";

        User admin = new User();
        admin.setName("Admin " + suffix);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("email", email);
        payload.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("token").asText();
    }

    private String registerClientAndLogin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MockMultipartFile identification = new MockMultipartFile(
                "identificationDocument",
                "id-" + suffix + ".pdf",
                "application/pdf",
                ("identification-" + suffix).getBytes()
        );

        MvcResult registerResult = mockMvc.perform(multipart("/api/auth/register")
                        .file(identification)
                        .param("name", "Client " + suffix)
                        .param("email", "client." + suffix + "@test.local")
                        .param("password", "StrongPass!123")
                        .param("role", "CLIENT")
                        .param("identificationNumber", "DOC-" + suffix))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return body.path("token").asText();
    }
}
