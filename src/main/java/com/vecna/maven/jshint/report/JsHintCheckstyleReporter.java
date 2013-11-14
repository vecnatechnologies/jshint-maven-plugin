/**
 * Copyright 2013 Vecna Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
*/

package com.vecna.maven.jshint.report;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map.Entry;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.Multimap;
import com.vecna.maven.jshint.mojo.JsHintError;

/**
 * Creates a Checkstyle-compatible report of JsHint violations.
 * @author ogolberg@vecna.com
 */
public class JsHintCheckstyleReporter implements JsHintReporter {
  /**
   * {@inheritDoc}
   */
  @Override
  public void report(Multimap<String, JsHintError> errors, OutputStream output) throws MojoExecutionException {
    XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    try {
      XMLStreamWriter w = new IndentingXMLStreamWriter(outFactory.createXMLStreamWriter(output));

      w.writeStartDocument();
      w.writeStartElement("checkstyle");
      w.writeAttribute("version", "4.3");

      for (Entry<String, Collection<JsHintError>> entry : errors.asMap().entrySet()) {
        w.writeStartElement("file");
        w.writeAttribute("name", entry.getKey());

        for (JsHintError error : entry.getValue()) {
          w.writeStartElement("error");

          w.writeAttribute("line", String.valueOf(error.getLine()));
          w.writeAttribute("column", String.valueOf(error.getCharacter()));
          w.writeAttribute("severity", "error");
          w.writeAttribute("message", error.getReason());

          w.writeEndElement();
        }

        w.writeEndElement();
      }
      w.writeEndElement();
      w.writeEndDocument();
      w.close();
    } catch (XMLStreamException e) {
      throw new MojoExecutionException("failed to write the violation report", e);
    }
  }
}
