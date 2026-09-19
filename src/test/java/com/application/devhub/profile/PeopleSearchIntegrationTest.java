package com.application.devhub.profile;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeopleSearchIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers users;

    private String bearer;

    @BeforeEach
    void seed() throws Exception {
        users.verified("me_searching", "Ada Searcher");
        users.verified("ada", "Ada Lovelace");
        users.verified("adam_k", "Adam King");
        users.verified("rustacean", "Ada Rust");
        users.verified("jose", "José Martínez");
        users.verified("ken_go", "Ken Thompson");
        users.unverified("ada_ghost");
        bearer = users.bearer("me_searching");
    }

    @Test
    void anExactUsernameComesFirstThenPrefixesThenNameMatches() throws Exception {
        search("ada")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].username", contains("ada", "adam_k", "rustacean")));
    }

    @Test
    void aTypoStillFindsTheDeveloper() throws Exception {
        search("adq").andExpect(jsonPath("$.data[*].username", hasItem("ada")));
    }

    @Test
    void displayNamesMatchByAnyWordIgnoringAccents() throws Exception {
        search("martinez").andExpect(jsonPath("$.data[*].username", contains("jose")));
        search("thompson").andExpect(jsonPath("$.data[*].username", contains("ken_go")));
    }

    @Test
    void aLeadingAtSignIsIgnored() throws Exception {
        search("@ken").andExpect(jsonPath("$.data[0].username").value("ken_go"));
    }

    @Test
    void neverIncludesMeOrUnverifiedAccounts() throws Exception {
        search("ada")
                .andExpect(jsonPath("$.data[*].username", not(hasItem("me_searching"))))
                .andExpect(jsonPath("$.data[*].username", not(hasItem("ada_ghost"))));
    }

    @Test
    void blankQueriesReturnNothingAndTheLimitIsRespected() throws Exception {
        search("   ").andExpect(jsonPath("$.data", empty()));
        mockMvc.perform(get("/users/search").param("q", "ada").param("limit", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/users/search").param("q", query).header(HttpHeaders.AUTHORIZATION, bearer));
    }
}
