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
 *  MailController.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.controller

import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.dto.MailDTO
import com.ailegorreta.mailservice.service.MailSenderService
import com.ailegorreta.mailservice.service.ParamService
import org.springframework.web.bind.annotation.*


/**
 * This is the API REST that can receive the Mail controller. The REST call is to send an email using the REST
 * channel and not the event Kafka channel.
 *
 * @project: mail-service
 * @author: rlh
 * @date: October 2023
 */
@CrossOrigin
@RestController
@RequestMapping("/mail")
class MailController(val mailSenderService: MailSenderService,
                     val paramService: ParamService): HasLogger {
    @PostMapping("/send")
    fun sendMail(@RequestBody mailDTO: MailDTO) = mailSenderService.processMail(mailDTO)

}
