/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  ControllerWebFluxTests.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.web

import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.EnableTestContainers
import com.ailegorreta.mailservice.config.ResourceServerConfig
import com.ailegorreta.mailservice.config.ServiceConfig
import com.ailegorreta.mailservice.controller.MailController
import com.ailegorreta.mailservice.dto.MailDTO
import com.ailegorreta.mailservice.dto.Template
import com.ailegorreta.mailservice.service.ParamService
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.util.*

/**
 * This class tests the REST calls received, we do not have the cache in Redis database the controller
 * via repository will call cache-service in order to fill the Redis database
 *
 * note: This class must be in Kotlin because we use the Coroutines in the repositories
 *
 * note: The Mockito needs a testing library to work properly in Kotlin. For more information see:
 * https://www.baeldung.com/kotlin/mockk#:~:text=In%20Kotlin%2C%20all%20classes%20and,that%20we%20want%20to%20mock.
 *
 * @proyect: cache-service
 * @author: rlh
 * @date: September 2023
 */
@WebFluxTest(MailController::class)
@EnableTestContainers
@ExtendWith(MockitoExtension::class)
@Import(ServiceConfig::class, ResourceServerConfig::class, MailController::class)
@ActiveProfiles("integration-tests-webflux")
internal class ControllerWebFluxTests : HasLogger {
    @MockBean
    private val streamBridge: StreamBridge? = null
    @MockBean
    private var reactiveJwtDecoder: ReactiveJwtDecoder? = null			// Mocked the security JWT

    @Autowired
    var applicationContext: ApplicationContext? = null

    // @Autowired
    private var webTestClient: WebTestClient? = null

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext!!)
                                      // ^ add Spring Security test Support
                                    .apply(SecurityMockServerConfigurers.springSecurity())
                                    .configureClient()
                                    .responseTimeout(Duration.ofMillis(30000))
                                    .build()
    }

    /**
     * Test for sending an email.
     * note: We cannot use directly to read the template using ParamService class because the Webflux
     *      block inside a block; that is the MailController is called with /mail/send uri and is the
     *      first block, and second block is when the ParamService calls the param-service microservice
     *      with uri /get/template.
     *
     *      So we mock the ParamService in order to mke this test work.
     */
    @Test
    fun `Send an email as a REST call`(@Autowired paramService: ParamService) {
        val uriMail = UriComponentsBuilder.fromUriString("/mail/send")
        val mail = MailDTO( template = "carta_bienvenida",
                            to = "user.test@ailegorreta",
                            subject = " TEST",
                            body = """
                            {
                                "nombre": "Prueba",
                                "apellido": "Apellido Prueba",
                                "monto": 9999.00,
                                "días": 99,
                                "dependientes": {
                                    "nombre": "Dependiente hijo",
                                    "parentesco": "hijo",
                                    "direccion": {
                                        "calle": "Prado Sur 240",
                                        "colonia": "Lomas Reforma"
                                    }
                                }
                            }                                
                            """.trimIndent())

        logger.debug("Will get the template")
        val template = Template(nombre = "carta_bienvenida", fileRepo = "2e8e826d-b9e0-49be-8451-172c0da028da",
                                json = """
                            {
                                "nombre": "Ricardo",
                                "apellido": "Legorreta",
                                "monto": 134450.2,
                                "días": 14,
                                "dependientes": {
                                    "nombre": "Isabel García Cano",
                                    "parentesco": "hija",
                                    "direccion": {
                                        "calle": "1430 Post Oak",
                                        "colonia": "Houston, Tx"
                                    }
                                }
                            }                     
                            
                            """.trimIndent(),
                            autor = "TEST", activo = true, fechaCreacion = LocalDate.now(), fechaModificacion = LocalDate.now(),
                            blockly = """
                                <xml xmlns=""https://developers.google.com/blockly/xml"">
                                </xml>
                            """.trimIndent(),
                            destino = Template.DestinoType.Email)

        mockkObject(paramService)
        every { runBlocking { paramService.getTemplate(any()) }} returns template

        logger.debug("Will send mail")

        webTestClient!!.mutateWith(
                SecurityMockServerConfigurers.mockJwt().authorities(listOf<GrantedAuthority>(
                        SimpleGrantedAuthority("SCOPE_iam.facultad"),
                        SimpleGrantedAuthority("SCOPE_acme.facultad")
                )))
                .post()
                .uri(uriMail.build().toUri())
                .contentType(MediaType.APPLICATION_JSON)

                .body(Mono.just(mail), MailDTO::class.java)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MailDTO::class.java)
                .consumeWith { result ->
                    val responseMail: MailDTO = result.responseBody!!

                    assertThat(responseMail.subject).isEqualTo(mail.subject)
                }
            //.returnResult()
            //.responseBody
    }

}
