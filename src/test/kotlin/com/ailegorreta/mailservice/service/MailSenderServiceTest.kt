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
 *  MailSenderServiceTest.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.service

import com.ailegorreta.mailservice.EnableTestContainers
import com.ailegorreta.mailservice.dto.MailDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableTestContainers
/* ^ This is a custom annotation to load the containers */
@ExtendWith(MockitoExtension::class)
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
/* ^ this is because: https://blog.mimacom.com/embeddedkafka-kafka-auto-configure-springboottest-bootstrapserversproperty/ */
@ActiveProfiles("integration-tests")
class MailSenderServiceTest {

    @MockBean
    private val streamBridge: StreamBridge? = null
    @MockBean
    private var reactiveJwtDecoder: ReactiveJwtDecoder? = null			// Mocked the security JWT

    /**
     * The only way that this test works is when we take out security from the 'param-service'
     * microservice because the web client used does not have any SCOPEs
     *
     * TODO See haw can we add scopes to the WebClient builder with SCOPEs . The only way known
     *      is to use WebFluxTest, but with this approach the block in block exception is thwrown.
     */
    @Test
    fun `Send an email from REST service`(@Autowired mailSenderService: MailSenderService) {
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
        val mailDTO = mailSenderService.processMail(mail)

        assertThat(mail).isEqualTo(mailDTO)
    }
}