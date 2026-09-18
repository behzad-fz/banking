package com.bank.exception;

import com.bank.integration.typesafe.TypeSafeClient;
import com.bank.modules.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "typesafe.api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MalformedRequestBodyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private TypeSafeClient typeSafeClient;

    @Test
    void typeSafeIsDisabledInTests() {
        assertFalse(typeSafeClient.isEnabled(), "tests must never call TypeSafe");
    }

    @Test
    @WithMockUser(authorities = "USER")
    void unparseableDateReturns400WithAcceptedFormats() throws Exception {
        String body = """
                {
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "email": "alice@example.com",
                  "phoneNumber": "+31-600000000",
                  "dateOfBirth": "sometime in 1990"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Accepted formats")));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":"))
                .andExpect(status().isBadRequest());
    }
}
