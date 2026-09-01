package com.backend.travelervpn.test

import com.backend.travelervpn.controller.VpnController
import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.generated.api.schema.Client
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.service.VpnLinkExtractorService
import com.backend.travelervpn.service.xui.XUIManagerService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant
import java.util.UUID

@WebMvcTest(VpnController::class)
class VpnControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val vpnUserRepository = mockk<VpnUserRepository>()
    private val xuiManagerService = mockk<XUIManagerService>()
    private val vpnLinkExtractorService = mockk<VpnLinkExtractorService>()
    private val messagingTemplate = mockk<SimpMessagingTemplate>()

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerKotlinModule()

        // Mock VpnUserRepository
        val vpnUser = VpnUser(
            userId = UUID.randomUUID(),
            username = "testuser",
            connectionLinks = setOf("link1", "link2"),
            expiryAt = Instant.now().plusSeconds(3600),
            trafficLeft = 26843545600L
        )
        coEvery { vpnUserRepository.findById(any<String>()) } returns mockk(relaxed = true)
        coEvery { vpnUserRepository.save(any<VpnUser>()) } returns mockk(relaxed = true)

        // Mock XUIManagerService
        coEvery { xuiManagerService.getAllInbounds() } returns mockk(relaxed = true)
        coEvery {
            xuiManagerService.createClient(
                userId = any(),
                expiryTime = any(),
                totalGB = any(),
                inbounds = any()
            )
        } returns mockk(relaxed = true)
        coEvery { xuiManagerService.getClientByEmail(any()) } returns mockk(relaxed = true)
        coEvery { xuiManagerService.getClientTraffic(any()) } returns mockk(relaxed = true)
        coEvery { xuiManagerService.testNode(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @Test
    fun `test subscription with existing user`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/user/subscription?userId=user1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                {
                    "status": "success",
                    "connectionLinks": ["link1", "link2"],
                    "message": "Created new user"
                }
            """
                )
            )
    }

    @Test
    fun `test subscription with new user`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/user/subscription")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                {
                    "status": "success",
                    "connectionLinks": ["link1", "link2"],
                    "message": "Created new user"
                }
            """
                )
            )
    }

    @Test
    fun `test user endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/user?userId=user1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                {
                    "status": "success",
                    "client": {
                        "id": "user1",
                        "email": "user1@secret.com"
                    }
                }
            """
                )
            )
    }

    @Test
    fun `test update user endpoint`() {
        val client = Client(
            id = "user1",
            email = "user1@secret.com",
            comment = "Test user",
            enable = true,
            expiryTime = Instant.now().plusSeconds(3600).toEpochMilli(),
            limitIp = 0,
            reset = 0,
            security = "auto",
            subId = "sub123",
            tgId = 0,
            totalGB = 26843545600L
        )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/user/update?userId=user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(client))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }

    @Test
    fun `test user traffic endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/user/traffic?userId=user1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                {
                    "status": "success",
                    "response": {
                        "total": 0
                    }
                }
            """
                )
            )
    }

    @Test
    fun `test test node endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/node/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"address": "127.0.0.1", "port": 8080, "apiToken": "testToken"}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }

    @Test
    fun `test detach inbounds endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/user/inbounds/detach?userId=user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""[1]""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }

    @Test
    fun `test attach inbounds endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/node/inbounds/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"user1", "inbounds":[1]}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }

    @Test
    fun `test ws endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/ws")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }

    @Test
    fun `test ws logout endpoint`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/ws/logout")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("""{"status":"success"}"""))
    }
}