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
 *  ParamService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.service

import com.ailegorreta.commons.event.EventGraphqlError
import com.ailegorreta.commons.event.EventType
import com.ailegorreta.mailservice.config.ServiceConfig
import com.ailegorreta.mailservice.dto.GraphqlRequestBody
import com.ailegorreta.mailservice.dto.GraphqlResponseTemplate
import com.ailegorreta.mailservice.dto.Template
import com.ailegorreta.mailservice.util.GraphqlSchemaReaderUtil
import com.ailegorreta.resourceserver.utils.HasLogger
import com.ailegorreta.resourceserver.utils.UserContext
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * ParamService to communicate to the param server repo microservice
 * - Templates
 *
 * @author rlh
 * @project mail-service
 * @date October 2023
 */
@Service
class ParamService(@Qualifier("client_credentials") val webClient: WebClient,
                    // ^ could use @Qualifier("client_credentials_load_balanced) for load balanced calls
                   private val eventService: EventService,
                   private val serviceConfig: ServiceConfig): HasLogger {

    fun uri() = UriComponentsBuilder.fromUriString(serviceConfig.getParamProvider())

    /**
     * Private method that read a template from param microservice.
     */
    fun getTemplate(name: String): Template? {
        val graphQLRequestBody = GraphqlRequestBody(
            GraphqlSchemaReaderUtil.getSchemaFromFileName("getTemplate"),
                                                            mutableMapOf("input" to name))

        val res = webClient.post()
                            .uri(uri().path("/param/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(UserContext.CORRELATION_ID, "mail-service correlation ID")
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(serviceConfig.clientId + "-client-credentials"))
                            .retrieve()
                            .toEntity(GraphqlResponseTemplate::class.java)
                            .block()
                            ?.body

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer el template:" + (res?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                  /* headers = res?.headers,*/ userName = "mail",
                                   eventName = "ERROR:LEER TEMPLATE",
                                   value = EventGraphqlError(res?.errors, mutableMapOf("template" to name))
            )
            return null
        }

        return if (res.data!!.templates.isEmpty())
                    null
                else
                    res.data.templates.first()

    }

}
