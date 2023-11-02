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
 *  EventService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.event.EventDTO
import com.ailegorreta.commons.event.EventType
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.config.ServiceConfig
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

/**
 * EventService that receives the events and sends the email if it has a valid format:
 * 'to', 'subject' and 'body' structure.
 *
 *  @author rlh
 *  @project : mail-service
 *  @date October 2023
 */
@Service
class EventService(private val serviceConfig: ServiceConfig,
                   private val streamBridge: StreamBridge,
                   private val mapper: ObjectMapper): HasLogger {

    private val coreName = "mail-service"

     /**
    * Send the event directly to a Kafka microservice using the EventConfig class or the .yml file if it is a
    * producer only.
    * These events are just for error situation.
    */
    fun sendEvent(headers: HttpHeaders? = null,
                  userName: String = "mail",
                  eventName: String,
                  value: Any,
                  eventType: EventType = EventType.DB_STORE,
                  producer:String = "producer-out-0"): EventDTO {
        val eventBody = mapper.readTree(mapper.writeValueAsString(value))
        val parentNode = mapper.createObjectNode()
        val correlationId = "No gateway, so no correlation id found"

        // Add the permit where notification will be sent
        parentNode.put("notificaFacultad", serviceConfig.getNotificaFacultad())
        parentNode.set<JsonNode>("datos", eventBody!!)

        val event = EventDTO(correlationId = correlationId,
            eventType = eventType,
            username = userName,
            eventName = eventName,
            applicationName = serviceConfig.appName!!,
            coreName = coreName,
            eventBody = parentNode)

         logger.debug("Will send use stream bridge to producer:$producer")
         val res = streamBridge.send(producer,event)
         logger.debug("Result for sending the message via streamBridge:$res")

        return event
    }
}
