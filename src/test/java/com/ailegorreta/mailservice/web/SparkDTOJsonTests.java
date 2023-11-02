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
 *  SparkDTOJsonTests.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.mailservice.web;

import com.ailegorreta.mailservice.dto.SparkDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example for a JsonTest for some DTOs, many others can be added
 *
 * In this case we check that SparkDTO is serializable correctly
 *
 * @project mail-service
 * @author rlh
 * @date October 2023
 */
@JsonTest
@ContextConfiguration(classes = SparkDTOJsonTests.class)
@ActiveProfiles("integration-tests")
public class SparkDTOJsonTests {
    @Autowired
    public JacksonTester<SparkDTO> json;

    @Test
    void testSerialize() throws Exception {
        var sparkDTO = new SparkDTO("rlh@legosoft.com.mx", "staff@.legosoft.com.mx",
                "Template de prueba", "Test",
                "Prueba");
        var jsonContent = json.write(sparkDTO);

        assertThat(jsonContent).extractingJsonPathStringValue("@.to")
                .isEqualTo(sparkDTO.getTo());
        assertThat(jsonContent).extractingJsonPathStringValue("@.from")
                .isEqualTo(sparkDTO.getFrom());
        assertThat(jsonContent).extractingJsonPathStringValue("@.template")
                .isEqualTo(sparkDTO.getTemplate());
        assertThat(jsonContent).extractingJsonPathStringValue("@.subject")
                .isEqualTo(sparkDTO.getSubject());
        assertThat(jsonContent).extractingJsonPathStringValue("@.body")
                .isEqualTo(sparkDTO.getBody());
    }

    @Test
    void testDeserialize() throws Exception {
        var sparkDTO = new SparkDTO("rlh@legosoft.com.mx", "staff@.legosoft.com.mx",
                                    "Template de prueba", "Test",
                                    "Prueba");
        var content = """
                {
                    "to": 
                    """ + "\"" + sparkDTO.getTo() + "\"," + """
                    "from": "staff@.legosoft.com.mx",
                    "template": 
                    """ + "\"" + sparkDTO.getTemplate() + "\"" + "," + """
                    "subject" : 
                    """ + "\"" + sparkDTO.getSubject() + "\"" + "," + """
                    "body" :\s
                    """ + "\"" + sparkDTO.getBody() + "\"" + """
                }
                """;
        assertThat(json.parse(content))
                        .usingRecursiveComparison()
                        .isEqualTo(sparkDTO);
    }
}

