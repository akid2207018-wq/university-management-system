package com.example.university.integration;

import com.example.university.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void roleBasedAccessControlIsEnforcedAcrossResources() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());

        String teacher = login("teacher", "ChangeMe123!");
        expect(teacher, "/api/teachers", 200);
        expect(teacher, "/api/admins", 403);
        expect(teacher, "/api/students", 403);

        String student = login("student", "ChangeMe123!");
        expect(student, "/api/students", 200);
        expect(student, "/api/teachers", 403);
        expect(student, "/api/admins", 403);

        String admin = login("admin", "ChangeMe123!");
        expect(admin, "/api/admins", 200);
        expect(admin, "/api/teachers", 200);
        expect(admin, "/api/students", 200);
    }

    private void expect(String token, String path, int expectedStatus) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
