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

package com.vecna.maven.jshint.mojo;

import org.mozilla.javascript.NativeObject;

/**
 * A POJO representation of a JSHint error.
 * @author ogolberg@vecna.com
 */
public class JsHintError {
  private static String toString(CharSequence seq) {
    return seq == null ? null : seq.toString();
  }

  private final String m_source;
  private final int m_line;
  private final int m_character;
  private final String m_evidence;
  private final String m_reason;

  /**
   * Create a new JSHint error
   * @param source name of the source file
   * @param line line number
   * @param character column number
   * @param evidence JS snippet that violated a rule
   * @param reason explanation of the violated rule
   */
  public JsHintError(String source, int line, int character, String evidence, String reason) {
    m_source = source;
    m_line = line;
    m_character = character;
    m_evidence = evidence;
    m_reason = reason;
  }

  /**
   * Create a {@link JsHintError} from a Javascript JsHint error object.
   * @param source source file
   * @param jsObject JS object representing a JsHint error
   */
  public JsHintError (String source, NativeObject jsObject) {
    this(source, ((Number) jsObject.get("line")).intValue(),
                 ((Number) jsObject.get("character")).intValue(),
                 toString((CharSequence) jsObject.get("evidence")), toString((CharSequence) jsObject.get("reason")));
  }


  /**
   * @return name of the source file
   */
  public String getSource() {
    return m_source;
  }

  /**
   * @return column number
   */
  public int getCharacter() {
    return m_character;
  }

  /**
   * @return JS snippet that violated a rule
   */
  public String getEvidence() {
    return m_evidence;
  }

  /**
   * @return line number
   */
  public int getLine() {
    return m_line;
  }

  /**
   * @return explanation of the violated rule
   */
  public String getReason() {
    return m_reason;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return m_source + "[" + m_line + "," + m_character + "]: " + m_reason + " (" + m_evidence + ")";
  }
}
