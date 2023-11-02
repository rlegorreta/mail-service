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
 *  MailServiceApplication.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice

import com.ailegorreta.mailservice.config.ServiceConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

/**
 * Mail service.
 *
 * Microservice that sends emails. This server must be connected to a mail server, for now as a demo we used the
 * Apache James mail server http://james.apache.org or the LegoSoft internal server
 *
 * To ways are to send emails:
 * - Receive a request from a REST service.
 * - Listen to a kafka event with topic 'mail' and send the email.
 *
 * Both methods need to include a 'to' email sender, 'subject' of a mai and the html 'body'. The from email is
 * defined in the application properties file.
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = ["com.ailegorreta.mailservice", "com.ailegorreta.resourceserver.utils"])
// this package must be included in order to instantiate de UserContext

class MailServiceApplication {
	companion object {

		@Bean
		fun kotlinPropertyConfigurer(): PropertySourcesPlaceholderConfigurer {
			val propertyConfigurer = PropertySourcesPlaceholderConfigurer()

			propertyConfigurer.setPlaceholderPrefix("@{")
			propertyConfigurer.setPlaceholderSuffix("}")
			propertyConfigurer.setIgnoreUnresolvablePlaceholders(true)

			return propertyConfigurer
		}

		@Bean
		fun defaultPropertyConfigurer() = PropertySourcesPlaceholderConfigurer()
	}

	@Bean
	fun mapperConfigurer() = Jackson2ObjectMapperBuilder().apply {
		serializationInclusion(JsonInclude.Include.NON_NULL)
		failOnUnknownProperties(true)
		featuresToDisable(*arrayOf(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
		indentOutput(true)
		modules(listOf(KotlinModule.Builder().build(), JavaTimeModule(), Jdk8Module()))
	}

	@Bean
	fun getJavaMailSender(serviceConfig: ServiceConfig): JavaMailSender {
		val mailSender = JavaMailSenderImpl()

		mailSender.host = serviceConfig.mailHost
		mailSender.port = serviceConfig.mailPort
		mailSender.username = serviceConfig.mailUsername
		mailSender.password = serviceConfig.mailPassword

		val props = mailSender.javaMailProperties

		props["mail.transport.protocol"] = "smtp"
		props["mail.smtp.auth"] = "true"
		props["mail.smtp.starttls.enable"] = "true"
		props["mail.debug"] = "true"

		return mailSender
	}
}

fun main(args: Array<String>) {
	runApplication<MailServiceApplication>(*args)
}
