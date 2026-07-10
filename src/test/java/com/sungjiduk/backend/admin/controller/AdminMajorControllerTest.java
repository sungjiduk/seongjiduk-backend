package com.sungjiduk.backend.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sungjiduk.backend.admin.dto.request.AdminMajorContentSpotRequest;
import com.sungjiduk.backend.admin.dto.request.AdminMajorUserRateRequest;
import com.sungjiduk.backend.admin.dto.request.AdminSpotUpsertRequest;
import com.sungjiduk.backend.admin.dto.response.AdminCommandResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorContentSpotResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorRateResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorUserResponse;
import com.sungjiduk.backend.admin.exception.ContentNotFoundException;
import com.sungjiduk.backend.admin.service.AdminContentService;
import com.sungjiduk.backend.admin.service.AdminMajorService;
import com.sungjiduk.backend.admin.service.AdminSpotService;
import com.sungjiduk.backend.common.config.SecurityConfig;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.user.service.UserService;

@DisplayName("AdminMajorController")
@Import(SecurityConfig.class)
@WebMvcTest(AdminMajorController.class)
public class AdminMajorControllerTest {
    @MockitoBean
    AdminMajorService adminMajorService;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("user는")
    class user {
        String jsonRequest = """
                {
                    "duration" : "week",
                    "date" : "2025-01-07"
                }
                """;

        @BeforeEach
        void givenEach() {
            given(adminMajorService.user(any(AdminMajorUserRateRequest.class)))
                .willReturn(new AdminMajorUserResponse(
                    List.of(), List.of()
                ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN 유저가 접근 시 HTTP OK를 반환해야 한다")
        void success() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER 유저가 접근 시 HTTP 403을 반환해야 한다")
        void failed_no_auth() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("비로그인 유저가 접근 시 HTTP 403를 반환해야 한다")
        void failed_no_login() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("content는")
    class content {
        String jsonRequest = """
                {
                    "duration" : "week",
                    "date" : "2025-01-07"
                }
                """;

        @BeforeEach
        void givenEach() {
            given(adminMajorService.content(any(AdminMajorContentSpotRequest.class)))
                .willReturn(new AdminMajorContentSpotResponse(
                    List.of(), List.of()
                ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN 유저가 접근 시 HTTP OK를 반환해야 한다")
        void success() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER 유저가 접근 시 HTTP 403을 반환해야 한다")
        void failed_no_auth() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("비로그인 유저가 접근 시 HTTP 403를 반환해야 한다")
        void failed_no_login() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("spot은")
    class spot {
        String jsonRequest = """
                {
                    "duration" : "week",
                    "date" : "2025-01-07"
                }
                """;

        @BeforeEach
        void givenEach() {
            given(adminMajorService.content(any(AdminMajorContentSpotRequest.class)))
                .willReturn(new AdminMajorContentSpotResponse(
                    List.of(), List.of()
                ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN 유저가 접근 시 HTTP OK를 반환해야 한다")
        void success() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/spot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER 유저가 접근 시 HTTP 403을 반환해야 한다")
        void failed_no_auth() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/spot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("비로그인 유저가 접근 시 HTTP 403를 반환해야 한다")
        void failed_no_login() throws Exception {
            // given
            // void givenEach()

            // when
            mockMvc.perform(get("/api/admin/major/spot")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
                    .with(csrf()))

                // then
                .andExpect(status().isForbidden());
        }
    }
}
