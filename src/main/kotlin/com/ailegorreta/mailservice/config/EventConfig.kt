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
 *  EventConfig.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.event.EventDTO
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.ApplicationContextProvider
import com.ailegorreta.mailservice.dto.SparkDTO
import com.ailegorreta.mailservice.service.MailSenderService
import org.apache.kafka.common.errors.SerializationException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * Spring cloud stream kafka configuration.
 *
 * This class configure Kafka to listen to the events that come from any microservice that utilizes the mail
 * server in order to send an email. If the mail is not valid an error event is sent to the auditory microservice.
 *
 * To see an example : https://refactorfirst.com/spring-cloud-stream-with-kafka-communication.html
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 *
 */
@Component
@Configuration
class EventConfig: HasLogger {

    /**
     * This is the case when we receive the events from the Param and the events are NOT forwarded to another
     * microservice listener. They are just invalidate the Redis database.
     *
     * The application.yml is the following:
     *
    function:
        definition: consumerParam
            kafka:
        bindings:
            consumerMail-in-0:
        consumer:
        configuration:
            value.serializer: com.lmass.auditserverrepo.service.event.dto.EventDTOSerializer
        bindings:
    consumerMail-in-0:
        destination: param
        consumer:
            use-native-decoding: true     # Enables using the custom deserializer
     */
    @Bean
    fun consumerMail(mailSenderService: MailSenderService): Consumer<EventDTO> = Consumer {
            event: EventDTO -> mailSenderService.processMail(event)
    }

    @Bean
    fun consumerSparkMail(mailSenderService: MailSenderService): Consumer<String> = Consumer {
        event: String -> run {
            val objectMapper = ApplicationContextProvider.getBean(ObjectMapper::class.java)

            try {
                val sparkDTO = objectMapper.readValue(event, SparkDTO::class.java)

                mailSenderService.processSparkMail(sparkDTO)
            } catch (e: JsonProcessingException) {
                throw SerializationException(e)
            }
        }
    }

}
