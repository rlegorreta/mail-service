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
 *  Template.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.dto

import java.time.LocalDate
import java.util.*

/**
 * Data class to get the Templates and its fields.
 *
 * @author rlh
 * @project : mail-service
 * @date October 2023
 */
data class Template(val id: UUID? = null,
                    var nombre: String,
                    var fileRepo: String?,
                    var destino: DestinoType?,
                    var json: String? = null,
                    var blockly: String? = null,
                    var autor: String,
                    var activo: Boolean = true,
                    var fechaCreacion: LocalDate? = null,
                    var fechaModificacion: LocalDate? = null)  {

    enum class DestinoType {
        Email, Reporte, SMS, Web, Otro, NoDefinido;

        companion object {
            // Avoid null values
            fun valueOfNull(value: String?): DestinoType? {
                return if (value != null) DestinoType.valueOf(value) else null
            }
        }
    }
}

data class TemplateField constructor(var id: UUID? = null,
                                     var nombre: String,
                                     var tipo: FieldType,
                                     var valorDefault: String?) {
    constructor() : this(id = null, nombre = "Nombre del campo",
        tipo = FieldType.Texto, valorDefault = null)

    enum class FieldType {
        Texto, Entero, Real, Monto, Fecha, Undefined;

        companion object {
            fun fromString(value: String): FieldType {
                values().forEach { if (it.toString() == value) return it }

                return Undefined
            }
        }

        fun toMxGraph() = when (this) {
            Texto -> "String"
            Entero -> "Integer"
            Real -> "Double"
            Monto -> "Double"
            Fecha -> "Date"
            Undefined -> "Undefined"
        }
    }
}

data class GraphqlResponseTemplate(val data: Data? = null,
                                   val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val templates: List<Template>)
}
