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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vecna.maven.jshint.report.JsHintCheckstyleReporter;
import com.vecna.maven.jshint.report.JsHintReporter;
import com.vecna.maven.jshint.rhino.JsEngine;

/**
 * JSHint plugin.
 * @author ogolberg@vecna.com
 */
@Mojo(name = "check",
      defaultPhase = LifecyclePhase.PROCESS_SOURCES,
      threadSafe = true)
public class JsHintMojo extends AbstractMojo {
  /**
   * Location of the JSHint source on the classpath. Only needs to be set if a custom version of JSHint is needed.
   */
  @Parameter(defaultValue = "jshint.js")
  private String jsHintJS;

  /**
   * Directory with the javascript files to be checked.
   */
  @Parameter(defaultValue = "${basedir}/src/main/javascript")
  private File srcDirectory;

  /**
   * List of file name patterns to include.
   */
  @Parameter
  private String[] includes;

  /**
   * List of file name patterns to exclude.
   */
  @Parameter
  private String[] excludes;

  /**
   * Location of the JSHint options file on the classpath or filesystem.
   */
  @Parameter(defaultValue = "jshintrc")
  private String optionsFile;

  /**
   * JSHint options (these take priority over the options file).
   */
  @Parameter
  private Map<String, String> options;

  /**
   * Allowed globals (these take priority over the options file).
   */
  @Parameter
  private Map<String, String> globals;

  /**
   * Maximum number of JSHint violations. Exceeding this will fail the build.
   */
  @Parameter(defaultValue = "0")
  private int maxErrorsAllowed;

  /**
   * Whether to skip execution.
   */
  @Parameter
  private boolean skip;

  /**
   * Location of the violation report.
   */
  @Parameter(defaultValue = "${project.build.directory}/jshint.xml")
  private File reportOutput;

  private final JsHintReporter reporter = new JsHintCheckstyleReporter();

  /**
   * Open a classpath resource for reading
   * @param path the path to the resource
   * @return an {@link InputStream} or <code>null</code> if the resource is not found
   */
  private InputStream openClasspathResource(String path) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
  }

  /**
   * Open a file or classpath resource for reading
   * @param path the path (first looked up on the file system, then on the classpath)
   * @return an {@link InputStream} or <code>null</code> if the resource is not found
   */
  private InputStream openFileOrClasspathResource(String path) {
    try {
      return new FileInputStream(path);
    } catch (FileNotFoundException e) {
      return openClasspathResource(path);
    }
  }

  /**
   * Read JSHint options from a JSON file specified in the configuration
   * @param engine JS engine
   * @return JS object with the options or an empty object if the options file doesn't exist
   * @throws MojoExecutionException if an error occurred while reading or parsing the file
   */
  private NativeObject readOptionsFromFile(JsEngine engine) throws MojoExecutionException {
    if (StringUtils.isNotEmpty(optionsFile)) {
      InputStream optionsSrc = openFileOrClasspathResource(optionsFile);
      if (optionsSrc != null) {
        String json;
        try {
          json = IOUtils.toString(optionsSrc);
        } catch (IOException e) {
          throw new MojoExecutionException("failed to read " + optionsFile, e);
        }
        try {
          return engine.parseJSON(json);
        } catch (RhinoException e) {
          throw new MojoExecutionException("failed to parse " + optionsFile, e);
        }
      }
    }

    return new NativeObject();
  }

  /**
   * Extract (and remove) the globals parameter from jshintrc options
   */
  private NativeObject extractGlobals(NativeObject optionsFromFile) throws MojoExecutionException {
    Object globalsFromFile = optionsFromFile.remove("globals");
    if (globalsFromFile != null && !(globalsFromFile instanceof NativeObject)) {
      throw new MojoExecutionException("bad globals definition");
    } else {
      return (NativeObject) globalsFromFile;
    }
  }

  /**
   * Apply the globals from the plugin configuration to the globals extracted from the options file
   */
  private NativeObject addGlobals(NativeObject globalsFromFile) {
    if (globals != null) {
      NativeObject merged = globalsFromFile == null ? new NativeObject() : globalsFromFile;
      for (Entry<String, String> keyval : globals.entrySet()) {
        merged.put(keyval.getKey(), merged, Boolean.valueOf(keyval.getValue()));
      }
      return merged;
    } else {
      return globalsFromFile;
    }
  }

  /**
   * @return source files (relative to the source directory)
   */
  private String[] getSourceFiles() {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(srcDirectory);
    scanner.setIncludes(includes);
    scanner.setExcludes(excludes);
    scanner.scan();

    return scanner.getIncludedFiles();
  }

  /**
   * @return true/false if the value is "true"/"false", integer if the value is numeric, or the string value itself
   */
  private Object toOptionValue(String value) {
    if ("true".equals(value)) {
      return true;
    } else if ("false".equals(value)) {
      return false;
    } else if (StringUtils.isNumeric(value)) {
      return Integer.valueOf(value);
    } else {
      return value;
    }
  }

  /**
   * combine the options from the plugin configuration with the options read from the file
   */
  private void addOptions(NativeObject optionsObject) {
    if (options != null) {
      for (Entry<String, String> keyval : options.entrySet()) {
        optionsObject.put(keyval.getKey(), optionsObject, toOptionValue(keyval.getValue()));
      }
    }
  }

  /**
   * create the error report
   * @param errors errors
   * @throws IOException if the report file cannot be written to
   */
  private void writeReport(Multimap<String, JsHintError> errors) throws IOException, MojoExecutionException {
    try {
      org.codehaus.plexus.util.FileUtils.forceMkdir(reportOutput.getParentFile());
    } catch (IOException e) {
      throw new MojoExecutionException("cannot create directory " + reportOutput.getParentFile());
    }

    OutputStream reportOut = new FileOutputStream(reportOutput);

    try {
      reporter.report(errors, reportOut);
    } finally {
      reportOut.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("skipping execution");
    } else {
      String[] sourceFiles = getSourceFiles();
      if (sourceFiles.length == 0) {
        getLog().info("no source files found");
      } else {
        final InputStream jsHintSrc = openClasspathResource(jsHintJS);

        JsEngine engine;
        try {
          engine = new JsEngine().browserEnv().eval(jsHintSrc);
        } catch (IOException e) {
          throw new MojoExecutionException("failed to bootstrap JSHint", e);
        }

        NativeObject combinedOpts = readOptionsFromFile(engine);
        NativeObject combinedGlobals = extractGlobals(combinedOpts);

        addOptions(combinedOpts);
        addGlobals(combinedGlobals);

        Function jsHint = (Function) engine.get("JSHINT");

        Multimap<String, JsHintError> errors = HashMultimap.create();

        for (String srcFile : sourceFiles) {
          List<String> source;
          try {
            source = FileUtils.readLines(new File(srcDirectory, srcFile));
          } catch (IOException e) {
            throw new MojoExecutionException("failed to read " + srcFile, e);
          }
          NativeArray array = new NativeArray(source.toArray());
          engine.call(jsHint, array, combinedOpts, combinedGlobals);
          NativeArray nativeErrors = (NativeArray) engine.get(jsHint, "errors");
          for (int i = 0; i < nativeErrors.size(); i++) {
            NativeObject nativeError = (NativeObject) nativeErrors.get(i);
            JsHintError error = new JsHintError(srcFile, nativeError);
            // handling the built-in JsHint error limit
            if (error.getReason().startsWith("Too many errors")) {
              break;
            }
            getLog().error(error.toString());
            errors.put(error.getSource(), error);
          }
        }

        try {
          writeReport(errors);
        } catch (IOException e) {
          throw new MojoExecutionException("failed to write the report", e);
        }

        if (errors.size() > maxErrorsAllowed) {
          throw new MojoFailureException("JSHint violations: " + errors.size() + ". Allowed violations: " + maxErrorsAllowed);
        }

      }
    }
  }
}
