package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.audit.AuditService;
import br.com.rizermarketplaces.core.marketplace.auth.AuthService;
import br.com.rizermarketplaces.core.marketplace.auth.JwtTokenProvider;
import br.com.rizermarketplaces.core.marketplace.dto.MediaPresignResponse;
import br.com.rizermarketplaces.core.marketplace.dto.MediaUploadResponse;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3StorageService storageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TenantRepository tenantRepository;

    @MockitoBean
    private TenantUserRepository tenantUserRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void uploadImage_returns200WithResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.webp", "image/webp", "image-content".getBytes()
        );
        MediaUploadResponse response = new MediaUploadResponse(
            "https://rizer-pic.br-se1.magaluobjects.com/uploads/abc123.webp",
            "uploads/abc123.webp",
            "rizer-pic",
            "image/webp",
            13L
        );
        when(storageService.uploadPublicImage(any(), eq("announce-gallery"))).thenReturn(response);

        mockMvc.perform(multipart("/media/upload/image")
                .file(file)
                .param("context", "announce-gallery"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("https://rizer-pic.br-se1.magaluobjects.com/uploads/abc123.webp"))
            .andExpect(jsonPath("$.key").value("uploads/abc123.webp"))
            .andExpect(jsonPath("$.bucket").value("rizer-pic"))
            .andExpect(jsonPath("$.contentType").value("image/webp"))
            .andExpect(jsonPath("$.sizeBytes").value(13));

        verify(storageService).uploadPublicImage(any(), eq("announce-gallery"));
    }

    @Test
    void uploadImage_ioException_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "content".getBytes()
        );
        when(storageService.uploadPublicImage(any(), eq("announce-gallery")))
            .thenThrow(new IOException("Disk full"));

        mockMvc.perform(multipart("/media/upload/image")
                .file(file)
                .param("context", "announce-gallery"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImage_sdkException_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "content".getBytes()
        );
        when(storageService.uploadPublicImage(any(), eq("announce-gallery")))
            .thenThrow(SdkClientException.create("S3 unavailable"));

        mockMvc.perform(multipart("/media/upload/image")
                .file(file)
                .param("context", "announce-gallery"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void uploadImage_emptyFile_handled() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "empty.png", "image/png", new byte[0]
        );
        MediaUploadResponse response = new MediaUploadResponse(
            "https://rizer-pic.br-se1.magaluobjects.com/uploads/empty.png",
            "uploads/empty.png",
            "rizer-pic",
            "image/png",
            0L
        );
        when(storageService.uploadPublicImage(any(), eq("announce-gallery"))).thenReturn(response);

        mockMvc.perform(multipart("/media/upload/image")
                .file(file)
                .param("context", "announce-gallery"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sizeBytes").value(0));
    }

    @Test
    void uploadDocument_returns200WithPresignedUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "contract.pdf", "application/pdf", "pdf-content".getBytes()
        );
        MediaUploadResponse response = new MediaUploadResponse(
            "https://rizer-storage.br-se1.magaluobjects.com/docs/contract.pdf?X-Amz-Algorithm=...",
            "docs/contract.pdf",
            "rizer-storage",
            "application/pdf",
            12L
        );
        when(storageService.uploadPrivateDocument(any(), eq("document"))).thenReturn(response);

        mockMvc.perform(multipart("/media/upload/document")
                .file(file)
                .param("context", "document"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("docs/contract.pdf"))
            .andExpect(jsonPath("$.bucket").value("rizer-storage"));
    }

    @Test
    void deleteFile_returns204() throws Exception {
        mockMvc.perform(delete("/media")
                .param("key", "uploads/abc123.webp")
                .param("bucket", "rizer-pic"))
            .andExpect(status().isNoContent());

        verify(storageService).deleteFile("uploads/abc123.webp", "rizer-pic");
    }

    @Test
    void getPresignedUrl_returns200WithUrl() throws Exception {
        Instant expiresAt = Instant.parse("2026-06-14T12:00:00Z");
        MediaPresignResponse response = new MediaPresignResponse(
            "https://rizer-storage.br-se1.magaluobjects.com/docs/contract.pdf?X-Amz-Algorithm=...",
            expiresAt
        );
        when(storageService.getPresignedUrl("docs/contract.pdf")).thenReturn(response);

        mockMvc.perform(get("/media/presign")
                .param("key", "docs/contract.pdf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.presignedUrl").value("https://rizer-storage.br-se1.magaluobjects.com/docs/contract.pdf?X-Amz-Algorithm=..."))
            .andExpect(jsonPath("$.expiresAt").value("2026-06-14T12:00:00Z"));
    }
}
