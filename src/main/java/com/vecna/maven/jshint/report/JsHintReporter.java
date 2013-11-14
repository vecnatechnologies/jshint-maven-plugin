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

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.Multimap;
import com.vecna.maven.jshint.mojo.JsHintError;

/**
 * API for writing JsHint reports.
 * @author ogolberg@vecna.com
 */
public interface JsHintReporter {
  /**
   * Write an error report to a stream
   * @param errors errors
   * @param output output stream
   * @throws MojoExecutionException if the report cannot be generated/written
   */
  public void report(Multimap<String, JsHintError> errors, OutputStream output) throws MojoExecutionException;
}
