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
 *  ServiceConfig.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.config

import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import com.ailegorreta.resourceserver.security.config.SecurityServiceConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.HashMap

/**
 * Service configuration stored in the properties .yml file.
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 *
 */
@Component
@Configuration
class ServiceConfig: SecurityServiceConfig, ServiceConfigAlfresco {

    @Value("\${spring.application.name}")
    val appName: String? = null

    @Value("\${mail-service.test}")
    val testing = false

    @Value("\${security.clientId}")
    val clientId = "false"

    @Value("\${management.mail.host}")
    var mailHost = ""

    @Value("\${management.mail.port}")
    var mailPort = 587

    @Value("\${management.mail.username}")
    var mailUsername = ""

    @Value("\${management.mail.password}")
    var mailPassword = ""

    @Value("\${mail-service.from}")
    var fromEmail = ""

    @Value("\${alfresco.url}")
    private val alfrescoServer: String = "Client rest not defined"
    override fun getAlfrescoServer() = alfrescoServer

    @Value("\${alfresco.username}")
    private val alfrescoUsername: String = "Alfresco username dose not exists"
    override fun getAlfrescoUsername() = alfrescoUsername

    @Value("\${alfresco.password}")
    private val alfrescoPassword: String = "Alfresco password does not exists"
    override fun getAlfrescoPassword() = alfrescoPassword

    @Value("\${alfresco.company}")
    private val alfrescoCompany: String = "Alfresco root directory"
    override fun getAlfrescoCompany() = alfrescoCompany

    @Value("\${microservice.param.provider-uri}")
    private val paramProviderUri: String = "Issuer uri not defined"
    fun getParamProvider() =  paramProviderUri

    override fun getSecurityIAMProvider() = "not needed for this micro.service"

    override fun getSecurityClientId(): HashMap<String, String> {
        return hashMapOf("param-service" to clientId
                        /* other microservice provers can be added here */
                        )
    }

    override fun getSecurityDefaultClientId() = clientId

    @Value("\${server.port}")
    private val serverPort: Int = 0
    override fun getServerPort() = serverPort


    fun getNotificaFacultad() = "NOTIFICA_MAIL"

}
