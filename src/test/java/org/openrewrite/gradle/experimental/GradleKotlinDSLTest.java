/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.experimental;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.openrewrite.kotlin.Assertions.kotlinScript;

class GradleKotlinDSLTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Recipe.noop())
          .parser(
            GradleKtsParser.builder()
              .buildscriptClasspath(Arrays.asList(
                Paths.get(System.getProperty("user.home") + "/.gradle/caches/8.6/kotlin-dsl/accessors/3de762cda0816e8e5d63ec46a2392ea9/classes"),
                Paths.get(System.getProperty("user.home") + "/.gradle/wrapper/dists/gradle-8.6-bin/afr5mpiioh2wthjmwnkmdsd5w/gradle-8.6/lib/gradle-kotlin-dsl-8.6.jar")
              ))
          );
    }

    @Test
    void simple() {
        rewriteRun(
          kotlinScript(
            """
              plugins {
                id("java")
              }
              """,
            spec -> spec.path("build.gradle.kts")
          )
        );
    }
}
