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
 *  ProcessHtmlException.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.exceptions

/**
 * Different exception types for HtmlProcess.
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 *
 */
open class ProcessHtmlException: Exception {
    constructor(message: String): super(message)
}

class BadJsonException : ProcessHtmlException {
    constructor(message: String): super("Error en el procesamiento de HTML:$message")
}

class VariableNotFoundException : ProcessHtmlException {
    constructor(message: String): super("Error en el procesamiento de HTML:$message")
}

class VariableFormatException : ProcessHtmlException {
    constructor(message: String): super("Error en el procesamiento de HTML:$message")
}

class TemplateNotFoundException : Exception {
    constructor(message: String): super("Error en la lectura del template:$message")
}

class TemplateNotActiveException : Exception {
    constructor(message: String): super("El template se encuentra:$message")
}


