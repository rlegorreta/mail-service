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
 *  MailSenderService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.event.EventDTO
import com.ailegorreta.commons.event.EventType
import com.ailegorreta.commons.event.EventGraphqlError
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.config.ServiceConfig
import com.ailegorreta.mailservice.dto.MailDTO
import com.ailegorreta.mailservice.dto.SparkDTO
import com.ailegorreta.mailservice.exceptions.TemplateNotActiveException
import com.ailegorreta.mailservice.exceptions.TemplateNotFoundException
import com.ailegorreta.mailservice.util.ProcessHtmlUtil
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.mail.javamail.MimeMessagePreparator
import org.springframework.stereotype.Service
import java.lang.Boolean
import jakarta.mail.internet.MimeMessage
import java.util.concurrent.CountDownLatch
import kotlin.Any
import kotlin.Exception
import kotlin.String
import kotlin.to


/**
 * ParamService retrieves all system parameters:
 *  - Variables
 *  - Dates
 *  - Document types
 *
 * note: before call param microservice, it checks existence in Redis memory database
 *
 *  @author rlh
 *  @project : mail-service
 *  @date October 2023
 */
@Service
class MailSenderService(private val serviceConfig: ServiceConfig,
                        private val paramService: ParamService,
                        private val documentService: DocumentService,
                        private val eventService: EventService,
                        private val javaMailSender: JavaMailSender,
                        private val mapper: ObjectMapper) : HasLogger {
    var templateContent: String = ""
    var template: String = "_"

    var latch = CountDownLatch(1)

    /**
     * This function send an email came from a Kafka event channel.
     *
     */
    /* note: The @KafkaListener annotation must be uncommented just for the Kafka test (i.e., KakfkaTests.kt class)
     *       without the use of Spring cloud stream configuration
     */
    // @KafkaListener(topics = ["any-service"], groupId = "group-mail-service")
    fun processMail(eventDTO: EventDTO): EventDTO {
        logger.debug("Receive an event to send an email {}", eventDTO)
        if (eventDTO.eventName == "sendMail") {

            try {
                val eventBody = eventDTO.eventBody as JsonNode  // as HashMap<*, *>
                val data = mapper.readTree(eventBody["datos"].toString())     // as String
                val template = data["template"] as TextNode
                val to = data["to"] as TextNode
                val subject = data["subject"] as TextNode
                val body = data["subject"]

                logger.debug("Send an email to $to with the subject:$subject and template:$template")
                sendMail(template.textValue(), to.textValue(), subject.textValue(), body.toString())
            } catch (e : Exception) {
                logger.error("Bad e mail format: ${e.message}")
                eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                       eventName = "ERROR:ENVIO DE UN MAIL",
                                       value = EventGraphqlError(listOf(mapOf("error" to e.message!!)), mutableMapOf("body" to eventDTO)))
            }
        } else {
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    eventName = "ERROR:ENVIO DE UN MAIL",
                                    value = EventGraphqlError(listOf(mapOf("error" to "se esperó un evento 'sendMail'")), mutableMapOf("body" to eventDTO)))
            logger.error("Error bad mail format")
        }
        latch.countDown()       // just for testing purpose

        return eventDTO
    }

    /**
     *  This function send an email came from a REST service as a JSON mail
     */
    fun processMail(mailDTO: MailDTO) : MailDTO {
        logger.debug("Receive a REST call to send an email: {}", mailDTO)
        try {
            val template = mailDTO.template
            val to = mailDTO.to
            val subject = mailDTO.subject
            val body = mailDTO.body

            logger.debug("Send an email to $to with the subject:$subject and template:$template")
            sendMail(template, to, subject, body)
        } catch (e : Exception) {
            logger.error("Bad mail format: ${e.message}")
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                eventName = "ERROR:ENVIO DE UN MAIL",
                value = EventGraphqlError(listOf(mapOf("error" to e.message!!)), mutableMapOf("mailDTO" to mailDTO)))
        }

        return mailDTO
    }

    fun processSparkMail(sparkDTO: SparkDTO): SparkDTO {
        logger.debug("Receive a Spark event to send an email: {}", sparkDTO)
        try {
            sendMail(template = sparkDTO.template!!,
                     to = sparkDTO.to!!,
                     subject = sparkDTO.subject!!,
                     jsonBody = sparkDTO.body!!,
                     from = sparkDTO.from!!)
        } catch (e : Exception) {
            logger.error("Bad mail format: ${e.message}")
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                        eventName = "ERROR:ENVIO DE UN MAIL",
                        value = EventGraphqlError(listOf(mapOf("error" to e.message!!)), mutableMapOf("mailDTO" to sparkDTO)))
        }
        latch.countDown()       // just for testing purpose

        return sparkDTO
    }

    private fun mergeDataWithHtml(body: String): String {
        val processHtmlUtil = ProcessHtmlUtil(templateContent, emptyList(), body, mapper)

        return processHtmlUtil.parseHTML(true)
    }

    /**
     * Process the json data with the template and the sends the mail using the mailserver defined in
     * the configuration: ${mail.host}.
     */
    private fun sendMail(template: String,
                                 to: String,
                                 subject: String,
                                 jsonBody: String,
                                 from: String = serviceConfig.fromEmail){
        try {

            if (template != this.template) {
                logger.debug("IN SEND MAIL $template")
                val templateDTO =  paramService.getTemplate(template)

                logger.debug("Found template {}", templateDTO)
                if (templateDTO != null) {
                    if (!templateDTO.activo)
                        throw TemplateNotActiveException("*no activo*")
                    val document = DocumentService.Document(documentService, templateDTO.fileRepo!!)

                    document.documentById()       // read content
                    this.templateContent = document.fileData.bufferedReader().use { it.readText() }
                    this.template = template
                } else
                    throw TemplateNotFoundException("Template no existe $template")
            }
            val bodyMail = mergeDataWithHtml(jsonBody)

            logger.debug("Will send mail:${bodyMail}")

            val msg = MimeMessagePreparator { mimeMessage: MimeMessage ->
                                                val messageHelper = MimeMessageHelper(mimeMessage)

                                                messageHelper.setFrom(from)
                                                messageHelper.setTo(to)
                                                messageHelper.setSubject(subject)
                                                messageHelper.setText(bodyMail, Boolean.TRUE)
                                            }
            javaMailSender.send(msg)
            logger.info("Mail sent to $to")
        } catch (e: Exception) {
            logger.error("No se pudo enviar el mail:  ${e.message} ")
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    eventName = "ERROR:ENVIO DE UN MAIL",
                                    value = EventGraphqlError(listOf(mapOf("error" to e.message!!)),
                                    mutableMapOf("template" to template,
                                                 "to" to to,
                                                 "subject" to subject,
                                                 "body" to jsonBody)))
        }
    }

    fun resetLatch() {
        latch = CountDownLatch(1)
    }

}
