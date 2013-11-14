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

package com.vecna.maven.jshint.rhino;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

/**
 * A convenience wrapper around the Rhino JS engine.
 * @author ogolberg@vecna.com
 */
public class JsEngine {
  private final Context m_context;
  private final Scriptable m_scope;

  /**
   * Initialize a new engine
   */
  public JsEngine() {
    m_context = Context.enter();
    // temporary - until https://github.com/jshint/jshint/issues/1333 is fixed
    m_context.setOptimizationLevel(-1);
    m_scope = m_context.initStandardObjects();
  }

  /**
   * Evaluate js code
   * @param code the code to evaluate
   * @return the result
   */
  public Object eval(String code) {
    return m_context.evaluateString(m_scope, code, null, 0, null);
  }

  /**
   * Evaluate js code from an input stream
   * @param is input stream
   * @return this
   * @throws IOException if the stream cannot be read
   */
  public JsEngine eval(InputStream is) throws IOException {
    Reader reader = new InputStreamReader(is);
    try {
      m_context.evaluateReader(m_scope, reader, null, 0, null);
    } finally {
      reader.close();
    }
    return this;
  }

  /**
   * Parse JSON into a JS object
   * @param json a JSON string
   * @return a JS object
   */
  public NativeObject parseJSON(String json) {
    NativeJSON jsJSON = (NativeJSON) get("JSON");
    Function parseJSON = (Function) jsJSON.get("parse");
    return (NativeObject) parseJSON.call(m_context, m_scope, m_scope, new Object[] {json});
  }

  /**
   * Add a fake browser environment to the global scope.
   * @return <code>this</code>
   */
  public JsEngine browserEnv() {
    m_scope.put("window", m_scope, new NativeObject());
    return this;
  }

  /**
   * Get a simple property of a JS object
   * @param obj JS object (complex object/function/etc)
   * @param property property
   * @return value
   */
  public Object get(Scriptable obj, String property) {
    return obj.get(property, m_scope);
  }

  /**
   * Retrieve an object from the global scope
   * @param name name of the object
   * @return the object bound to the name
   */
  public Object get(String name) {
    return get(m_scope, name);
  }

  /**
   * Call a function
   * @param fun the function to call
   * @param args the arguments to pass to the function
   * @return the result
   */
  public Object call(Function fun, Object ... args) {
    return fun.call(m_context, m_scope, m_scope, args);
  }

  /**
   * Call a function by name
   * @param functionName the name of the function in the global scope
   * @param args the arguments to pass to the function
   * @return the result
   */
  public Object call(String functionName, Object ... args) {
    Function fun = (Function) get(functionName);
    return call(fun, args);
  }
}
