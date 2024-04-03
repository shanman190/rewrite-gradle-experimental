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
package org.openrewrite.gradle.experimental

import org.jetbrains.kotlin.assignment.plugin.AssignmentComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingCompilerExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.openrewrite.ExecutionContext
import org.openrewrite.Parser
import org.openrewrite.SourceFile
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class GradleKtsParser private constructor(private val base: Builder) : Parser {
    private lateinit var defaultClasspath: Collection<Path>
    private var buildParser: KotlinParser? = null
    private var settingsParser: KotlinParser? = null

    @OptIn(ExperimentalCompilerApi::class)
    override fun parseInputs(sources: Iterable<Parser.Input>, relativeTo: Path?, ctx: ExecutionContext): Stream<SourceFile> {
        if (this.buildParser == null) {
            this.buildParser = KotlinParser.builder(this.base.kotlinParser)
                    .classpath(this.base.buildscriptClasspath)
                    .compilerCustomizers(
                            { config ->
                                val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
                                config.add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, ScriptDefinition.FromConfigurations(
                                        hostConfiguration = hostConfiguration,
                                        compilationConfiguration = ScriptCompilationConfiguration {
                                            baseClass("org.gradle.kotlin.dsl.KotlinBuildScript") // org.gradle.kotlin.dsl.KotlinProjectScriptTemplate
//                                            defaultImports(implicitImports)
                                            hostConfiguration(hostConfiguration)
                                            implicitReceivers("org.gradle.api.Project")
                                        },
                                        evaluationConfiguration = null
                                ))
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ScriptingK2CompilerPluginRegistrar())
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, SamWithReceiverComponentRegistrar())
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, AssignmentComponentRegistrar())
                            }
                    )
                    .build()
        }

        if (this.settingsParser == null) {
            this.settingsParser = KotlinParser.builder(this.base.kotlinParser)
                    .classpath(this.base.settingsClasspath)
                    .compilerCustomizers(
                            { config ->
                                val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
                                config.add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, ScriptDefinition.FromConfigurations(
                                        hostConfiguration = hostConfiguration,
                                        compilationConfiguration = ScriptCompilationConfiguration {
                                            baseClass("org.gradle.kotlin.dsl.KotlinSettingsScript") // org.gradle.kotlin.dsl.KotlinProjectScriptTemplate
//                                            defaultImports(implicitImports)
                                            hostConfiguration(hostConfiguration)
                                            implicitReceivers("org.gradle.api.initialization.Settings")
                                        },
                                        evaluationConfiguration = null
                                ))
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ScriptingK2CompilerPluginRegistrar())
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, SamWithReceiverComponentRegistrar())
                                config.add(org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, AssignmentComponentRegistrar())
                            }
                    )
                    .build()
        }

        return StreamSupport.stream(sources.spliterator(), false)
                .flatMap { source ->
                    if (source.path.endsWith("settings.gradle.kts")) {
                        return@flatMap settingsParser!!.parseInputs(Collections.singletonList(source), relativeTo, ctx);
                    }
                    return@flatMap buildParser!!.parseInputs(Collections.singletonList(source), relativeTo, ctx);
                };
    }

    override fun accept(path: Path): Boolean = path.toString().endsWith(".gradle.kts")

    override fun sourcePathFromSourceText(prefix: Path, sourceCode: String): Path = prefix.resolve("build.gradle.kts")

    class Builder : Parser.Builder(K.CompilationUnit::class.java) {
        internal var kotlinParser = KotlinParser.builder()
        internal var buildscriptClasspath: Collection<Path>? = null
        internal var settingsClasspath: Collection<Path>? = null

        fun kotlinParser(kotlinParser: KotlinParser.Builder): Builder {
            this.kotlinParser = kotlinParser
            return this
        }

        fun buildscriptClasspath(classpath: Collection<Path>): Builder {
            this.buildscriptClasspath = classpath;
            return this
        }

        fun buildscriptClasspath(vararg classpath: String): Builder {
            this.buildscriptClasspath = JavaParser.dependenciesFromClasspath(*classpath)
            return this
        }

        fun buildscriptClasspathFromResources(ctx: ExecutionContext, vararg artifactNamesWithVersions: String): Builder {
            this.buildscriptClasspath = JavaParser.dependenciesFromResources(ctx, *artifactNamesWithVersions)
            return this
        }

        fun settingsClasspath(classpath: Collection<Path>): Builder {
            this.settingsClasspath = classpath
            return this
        }

        fun settingsClasspath(vararg classpath: String): Builder {
            this.settingsClasspath = JavaParser.dependenciesFromClasspath(*classpath)
            return this
        }

        fun settingsClasspathFromResources(ctx: ExecutionContext, vararg artifactNamesWithVersions: String): Builder {
            this.settingsClasspath = JavaParser.dependenciesFromResources(ctx, *artifactNamesWithVersions)
            return this
        }

        override fun build() = GradleKtsParser(this)

        override fun getDslName() = "gradle kts"
    }

    private companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}