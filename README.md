JSHint Maven Plugin
====================

This Maven plugin uses [JSHint](http://www.jshint.com/) to detect errors and style violations in Javascript code.

Usage
----------------------------------

This is a sample set up that checks Javascript sources during the `process-sources` phase.

      <plugin>
        <groupId>com.vecna.maven</groupId>
        <artifactId>jshint-maven-plugin</artifactId>
        <version>${plugin.version}</version>
        <executions>
          <execution>
            <id>default-check</id>
            <phase>process-sources</phase>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
              <includes>
                <include>**/*.js</include>
              </includes>
              <excludes>
                <exclude>**/*-min.js</exclude>
              </excludes>
              <optionsFile>${basedir}/src/main/resources/jshintrc</optionsFile>
              <options>
                <quotmark>double</quotmark>
              </options>
              <globals>
                <Backbone>true</Backbone>
              </globals>
              <maxErrorsAllowed>5</maxErrorsAllowed>
            </configuration>
          </execution>
        </executions>
      </plugin>

The JSON options file can be loaded from the filesystem or the classpath. [JSHint docs](http://www.jshint.com/docs/) describe the actual options/format. It is recommended to have a global options file packaged as a separate jar artifact. Individual JSHint options can be overridden inline through the `options` and `globals` plugin parameters.

Note that one of the JSHint options is `maxerror` which controls the built-in limit of errors **per file**. Once the limit is reached, JSHint will stop scanning the file. It is recommended to set this limit to be much higher than the expected number of violations per file (e.g. 1000).

The plugin will fail the build if the number of violations exceeds the `maxErrorsAllowed` parameter (0 by default).

Error Report
----------------------------------

In addition to logging/counting violations, the plugin generates a Checkstyle-compatible report located by default at `target/jshint.xml`.

Credits
-------

Originally developed by [Vecna Technologies, Inc.](http://http://www.vecna.com/) and open sourced as part of its community service program. See the LICENSE file for more details.
Vecna Technologies encourages employees to give 10% of their paid working time to community service projects. 
To learn more about Vecna Technologies, its products and community service programs, please visit http://www.vecna.com.

