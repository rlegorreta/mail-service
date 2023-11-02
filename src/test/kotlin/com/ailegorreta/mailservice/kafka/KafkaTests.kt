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
 *  KafkaTests.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.kafka

import com.ailegorreta.mailservice.EnableTestContainers
import com.ailegorreta.mailservice.config.EventConfig
import com.ailegorreta.mailservice.config.ServiceConfig
import com.ailegorreta.mailservice.service.EventService
import com.ailegorreta.commons.event.EventDTO
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.dto.MailDTO
import com.ailegorreta.mailservice.dto.SparkDTO
import com.ailegorreta.mailservice.service.EventServiceTest
import com.ailegorreta.mailservice.service.MailSenderService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.*
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.TimeUnit

/**
 * For a good test slices for testing @SpringBootTest, see:
 * https://reflectoring.io/spring-boot-test/
 * https://www.diffblue.com/blog/java/software%20development/testing/spring-boot-test-slices-overview-and-usage/
 *
 * This class test all context with @SpringBootTest annotation and checks that everything is loaded correctly.
 * Also creates the classes needed for all slices in @TestConfiguration annotation
 *
 * Testcontainers:
 *
 * Use for test containers Redis & Kafka following the next's ticks:
 *
 * - As little overhead as possible:
 * - Containers are started only once for all tests
 * - Containers are started in parallel
 * - No requirements for test inheritance
 * - Declarative usage.
 *
 * see article: https://maciejwalkowiak.com/blog/testcontainers-spring-boot-setup/
 *
 * For GraphQL tester see:
 * https://piotrminkowski.com/2023/01/18/an-advanced-graphql-with-spring-boot/
 *
 * Also for a problem with bootstrapServerProperty
 * see: https://blog.mimacom.com/embeddedkafka-kafka-auto-configure-springboottest-bootstrapserversproperty/
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableTestContainers
/* ^ This is a custom annotation to load the containers */
@ExtendWith(MockitoExtension::class)
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
/* ^ this is because: https://blog.mimacom.com/embeddedkafka-kafka-auto-configure-springboottest-bootstrapserversproperty/ */
@Testcontainers			/* Activates automatic startup and cleanup of test container */
@Import(ServiceConfig::class)
@ActiveProfiles("integration-tests")
@DirtiesContext				/* will make sure this context is cleaned and reset between different tests */
class KafkaTests: HasLogger {
    /* StreamBridge instance is used by EventService but in @Test mode it is not instanced, so we need to mock it:
       see: https://stackoverflow.com/questions/67276613/streambridge-final-cannot-be-mocked
       StreamBridge is a final class, With Mockito2 we can mock the final class, but by default this feature is disabled
       and that need to enable with below steps:

       1. Create a directory ‘mockito-extensions’ in src/test/resources/ folder.
       2. Create a file ‘org.mockito.plugins.MockMaker’ in ‘src/test/resources/mockito-extensions/’ directory.
       3. Write the content 'mock-maker-inline' in org.mockito.plugins.MockMaker file.

        At test class level use ‘@ExtendWith(MockitoExtension.class)’
        Then StreamBridge will be mocked successfully.

        note: Instead of mocking the final class (which is possible with the latest versions of mockito using the
        mock-maker-inline extension), you can wrap StreamBridge into your class and use it in your business logic.
        This way, you can mock and test it any way you need.

        This is a common practice for writing unit tests for code where some dependencies are final or static classes
     */
    @MockBean
    private var reactiveJwtDecoder: ReactiveJwtDecoder? = null			// Mocked the security JWT

    /**
     * Do not use @MockBean because it initialises an empty ReactiveClientRegistration
     * Repository, so it crashed when create a WebClient.
     */
    @Autowired
    lateinit var reactiveClientRegistrationRepository: ReactiveClientRegistrationRepository

    @MockBean
    var authorizedClientRepository: OAuth2AuthorizedClientRepository?= null

    /**
     * For Kafka all are consumer events, the producer we use Kafka template instance
     */
    @Autowired
    var consumer: MailSenderService? = null

    @Autowired
    var template: KafkaTemplate<String, EventDTO>? = null

    @Autowired
    var producer: EventService? = null

    @Autowired
    var sparkProducer: EventServiceTest? = null

    @Autowired
    var mapper: ObjectMapper? = null

    @Autowired
    var serviceConfig: ServiceConfig? = null

    @Autowired
    var eventConfig: EventConfig? = null

    /**
     * This test send an event 'sendMail' in order to send an email from any microservice.
     *
     * Testing with Spring Cloud stream, i.e., used the configuration defines in the application.yml file and not
     * in the Kafka test container, so we not use the Kafka template.
     */
    @Test
    fun `Test to send en event "Send mail"`() {
        val mail = MailDTO(template = "carta_bienvenida",
                           to = "user.test@ailegorreta",
                           subject = "TEST",
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
        producer!!.sendEvent(eventName = "sendMail", value = mail, producer = "producerMailTest-out-0")

        val messageConsumed = consumer!!.latch.await(30, TimeUnit.SECONDS)
        logger.debug("After message consumed $messageConsumed")

        assertTrue(messageConsumed)
    }

    @Test
    fun `Test to send en spark "Send mail"`() {
        val mail = SparkDTO(template = "carta_bienvenida",
                            from = "admin.test@ailegorreta",
                            to = "user.test@ailegorreta",
                            subject = "TEST",
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
        sparkProducer!!.sendSparkEvent( value = mail, producer = "producerSparkMailTest-out-0")

        val messageConsumed = consumer!!.latch.await(10, TimeUnit.SECONDS)
        logger.debug("After message consumed $messageConsumed")

        assertTrue(messageConsumed)
    }

}
