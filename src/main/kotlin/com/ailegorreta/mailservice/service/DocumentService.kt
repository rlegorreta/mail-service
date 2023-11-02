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
 *  DocumentService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.service

import com.ailegorreta.commons.cmis.CMISService
import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import com.ailegorreta.commons.cmis.data.AbstractCmisStreamObject
import com.ailegorreta.commons.cmis.util.HasLogger
import org.springframework.stereotype.Service
import java.io.InputStream

/**
 * This service reads documents in Alfresco repository: Is utilize ailegorreta-kit-commons-cmis-library
 *
 * @project mail-service
 * @autor rlh
 * @date October 2023
 */
@Service
class DocumentService(override val serviceConfig: ServiceConfigAlfresco,
                      private val eventService: EventService): CMISService(serviceConfig), HasLogger {

    class Document: AbstractCmisStreamObject {

        companion object {
            fun emptyHtmlDocument(documentService: DocumentService, fileName: String, author: String) =
                Document(documentService = documentService, fileName = fileName,
                    fileData = "<h1>Vacío</h1>".byteInputStream(), contentLength = 14L,
                    mimeType = "text/html", author = author)
        }

        constructor(documentService: DocumentService, objectId: String): super(documentService, objectId)

        constructor(documentService: DocumentService, fileName: String,
                    fileData: InputStream, contentLength: Long, mimeType: String, author: String)  :
                super(documentService, fileName, fileData, contentLength, mimeType, author)

    }

}
