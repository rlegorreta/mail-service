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
 *  ProcessHtmlUtil.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.utils.FormattingUtils
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.mailservice.dto.TemplateField
import com.ailegorreta.mailservice.exceptions.BadJsonException
import com.ailegorreta.mailservice.exceptions.VariableFormatException
import com.ailegorreta.mailservice.exceptions.VariableNotFoundException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.util.Assert
import java.math.BigDecimal
import java.time.LocalDate

/**
 * This class is to process a document formatted in html with extra tags defined in the templateFields are
 * processes and replaced with the needed values produced the final result of the html.
 *
 * The input data could be received from a list of TemplateFields or as a json data defined in the constructor.
 *
 * The original content is kept in memory in order to do batch processing. That is it can be used for batch processing
 * just re-defining and re-processing the input data keeping the original content.
 *
 * If an error exists the process is halted and an error is sent. The json received from the mail event can not have
 * any missing field defined in the template.
 *
 * @project mail-service
 * @author rlh
 * @date October 2023
 */
class ProcessHtmlUtil: HasLogger {

    companion object {
        const val ATTR_VAR = "varname"
        const val MARKER_YELLOW = "marker-yellow"
        const val SPAN_TAG = "span"
        const val ATTR_HTML = "mark"            // attribute that starts the replacement
        const val VALUE = "valor"               // tha value for array in a json format variable
        const val ATTR_PAGE_BREAK = "style"     // this is to avoid page breaks in html table
        const val ATTR_VALIGN = "valign"        // vertical alignment for tr html elements


        private val staticVariables = hashMapOf("hoy" to LocalDate.now().toString() )

        fun addStaticVariables(name: String, value: String) {
            staticVariables[name] = value
        }
    }

    private val vars = HashMap<String, Var<*>>()
    private val content: String
    var document : Document? = null   // HTML document parsed


    constructor(content: String,
                variables: Collection<TemplateField> = emptyList(),
                json: String? = "", mapper: ObjectMapper) {
        this.content = content
        if (variables.isEmpty())  //vars are defined using the json string
            try {
                val jsonElements: Map<String, Any> = mapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
                jsonElements.entries.forEach { initVars(it.key, it.value)}
            } catch (e : Exception) {
                throw BadJsonException("Error en el JSON. No se encuentra bien formado $json, no se hizo la lectura de variables")
            }
        else // vars are defined using the variables collection of TemplateFields
            variables.forEach { vars[it.nombre] = Var.fromTemplateField(it) }
    }

    private fun initVars(name: String,  value: Any) {
        if (value is HashMap<*,*>) {
            value.forEach { k, v -> initVars("$name.$k", v!!) }
            // ^ use dot notations
        } else
            vars[name] = Var.fromTemplateJson(name, value)
    }

    fun setVaribales(json: JsonNode): Boolean {
        vars.keys.forEach { name ->
            val variable = json.get(name)

            if (variable != null) {
                if (variable.isArray) {  // create a unique string separate by ';'
                    val value = StringBuffer()

                    variable.forEach {
                        val v = it.get(VALUE)

                        if (v == null) {
                            logger.error("No se encontró en el json a la variable $name como array con el valor $VALUE")
                            throw VariableNotFoundException("No se encontró en el json a la variable $name como array con el valor $VALUE")
                        }
                        value.append(';')
                        value.append(v.textValue())
                    }
                    if (!vars[name]!!.fromStringValue(value.toString()))  {
                        logger.error(("Error en formato de la variable ${value.toString()}"))
                        throw VariableFormatException("No se encontró en el json a la variable $name como array con el valor $VALUE")
                    }
                } else
                    if (!vars[name]!!.fromStringValue(variable.asText())) {
                        logger.error("Error en formato de la variable ${variable.asText()}")
                        throw VariableFormatException("Error en formato de la variable ${variable.asText()}")
                    }
            } else {
                logger.error("No se encontró en el json a la variable $name se utiliza el valor de 'default'")
                throw VariableNotFoundException("No se encontró en el json a la variable $name se utiliza el valor de 'default'")
            }
        }

        return true
    }

    fun setVariablesAndProcess(json: JsonNode): String? {
        if (setVaribales(json))
            return parseHTML()

        return null
    }

    fun parseHTML(validate: Boolean = false): String {
        document = Jsoup.parse(content)

        for (tag in document!!.select(ATTR_HTML)) {
            // This is data, add the placeHolder in order not to be editable by the user
            process(tag, validate)
        }
        for (b in document!!.select("b"))  // This is for MS-Word compatibility
            if (hasDelimiters(b.text(), "[", "]")) {
                // This is data, add the placeHolder in order not to be editable by the user
                process(b, validate)
            }

        return document!!.outerHtml()
    }

    private fun hasDelimiters(token: String, prefix: String, suffix: String): Boolean {
        return token.startsWith(prefix) && token.endsWith(suffix)
    }

    private fun deleteDelimiters(token: String, index: Int): String {
        return token.substring(index, token.length - index)
    }

    private fun process(element : Element, validate: Boolean) {
        var elementContent = element.text()
        val e = if (validate)
                        element
                else {
                    val parent = element.parent()

                    element.remove()
                    parent!!.appendElement("span")
                }

        e.removeClass(MARKER_YELLOW)
        e.tagName(SPAN_TAG)           // change the mark tagName for SPAN to do nothing
        if (elementContent == null) {
            e.text("Nombre de la variable NULL")
            e.attr(ATTR_VAR, "null")
        } else {
            val eContent: String = if (hasDelimiters(elementContent, "[", "]"))
                                        deleteDelimiters(elementContent, 1)
                                    else
                                        elementContent

            if (eContent.startsWith("@")) {  // is is a static variable
                val value = staticVariables[eContent.substring(1)]

                e.text(value ?: "Error: variable estática $eContent no definida")
                e.attr(ATTR_VAR, elementContent)
            } else {
                val variable = vars[eContent]

                if (variable != null) {
                    if (variable.isArray()) {
                        // Process a HTML table
                        expandToTable(e, variable)
                    } else {            // simple array
                        e.text(variable.value())
                        e.attr(ATTR_VAR, elementContent)
                    }
                } else {
                    e.text("Error: variable no definida '$eContent'")
                    e.attr(ATTR_VAR, elementContent)
                }
            }
        }
    }

    private fun expandToTable(e: Element, vs: Var<*>) {
        e.text("")
        val tableElement = e.appendElement("table")
        var trElement: Element? = null

        // Avoid the table element to have page breaks
        tableElement.attr(ATTR_PAGE_BREAK, "page-break-inside: avoid;")
        // The table html element always try to put everything in a
        // two column table
        for(tdIndex in 0 until vs.values.size) {
            if ((tdIndex % 2) == 0) {
                trElement = tableElement.appendElement("tr")
                trElement.attr(ATTR_VALIGN, "top")
            }
            val tdElement = trElement!!.appendElement("td")
            val pElement = tdElement.appendElement("p")
            val scanElement = pElement.appendElement("scan")

            scanElement.text(vs.value(tdIndex))
            scanElement.attr(ATTR_VAR, vs.name)
        }
    }

    /*
     * Variable internal class.
     */
    data class Var<T> constructor(val name: String,
                                  var values: List<T>) {

        companion object {
            /*
             * It uses the Template Var records or  aJson structure
             */
            fun fromTemplateField(templateField: TemplateField): Var<*> {
                try {
                    when (templateField.tipo) {
                        TemplateField.FieldType.Texto -> {
                            val values = if (templateField.valorDefault == null)
                                listOf("no definido")
                            else
                                templateField.valorDefault!!.split(';')
                            return Var(name = templateField.nombre, values = values)
                        }
                        TemplateField.FieldType.Entero -> {
                            val values = if (templateField.valorDefault == null)
                                listOf(-1L)
                            else
                                templateField.valorDefault!!.split(';').map { it.toLong() }
                            return Var(name = templateField.nombre, values = values)
                        }
                        TemplateField.FieldType.Real -> {
                            val values = if (templateField.valorDefault == null)
                                listOf(-1.0)
                            else
                                templateField.valorDefault!!.split(';').map { it.toDouble() }
                            return Var(name = templateField.nombre, values = values)
                        }
                        TemplateField.FieldType.Monto -> {
                            val values = if (templateField.valorDefault == null)
                                listOf(BigDecimal.ZERO)
                            else
                                templateField.valorDefault!!.split(';').map { it.toBigDecimal() }
                            return Var(name = templateField.nombre, values = values)
                        }
                        TemplateField.FieldType.Fecha -> {
                            val values = if (templateField.valorDefault == null)
                                listOf(LocalDate.now())
                            else
                                templateField.valorDefault!!.split(';').map { LocalDate.parse(it) }
                            return Var(name = templateField.nombre, values = values)
                        }
                        TemplateField.FieldType.Undefined -> {
                            return Var(name = templateField.nombre, values = listOf("Tipo no definido"))
                        }
                    }
                } catch (e: Exception) {
                    return Var(
                        name = templateField.nombre,
                        values = listOf("ERROR en el formato '${templateField.nombre}' del valor de 'default' definido en el template")
                    )
                }
            }

            /*
             * It utilizes as input a Json string
             */
            fun fromTemplateJson(name: String, variable: Any): Var<*> {
                return when (variable) {
                    is String -> Var(name = name, variable.split(';'))
                    is Int -> Var(name = name, listOf(variable.toString().toLong()))
                    is Long -> Var(name = name, listOf(variable.toString().toLong()))
                    is Double -> Var(name = name, listOf(variable.toString().toBigDecimal()))
                    else ->
                        Var(
                            name = name,
                            values = listOf("ERROR en el formato '${name}' del valor de 'default' definido en el template")
                        )

                }

            }
        }

        fun value(value: T? = null): String {
            val v = value ?: values.first()

            if (v is String) return v
            if (v is Long || v is Int) return "%,d".format(v)
            if (v is Double) return "%,.2f".format(v)
            if (v is BigDecimal) return "$%,.2f".format(v)
            if (v is LocalDate) return FormattingUtils.FULL_DATE_FORMATTER.format(v)

            return v.toString()
        }

        fun value(index: Int): String {
            Assert.isTrue(index < values.size, "Variable no existente con el index: $index")
            return value(values[index])
        }

        fun fromStringValue(newValues: String): Boolean{
            val v = values.first()

            return try {
                when (v) {
                    (v is String) -> values = newValues.split(';') as List<T>
                    (v is Long),
                    (v is Int) -> values = newValues.split(';').map { it.toLong() } as List<T>
                    (v is Double) -> values = newValues.split(';').map { it.toDouble() } as List<T>
                    (v is BigDecimal) -> values = newValues.split(';').map { it.toBigDecimal() } as List<T>
                    (v is LocalDate) -> values = newValues.split(';').map { LocalDate.parse(it) } as List<T>
                    else -> Assert.isTrue(true, "Error undefined type ${v!!::class.java.canonicalName}")
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        fun isArray() = values.size > 1
    }
}
