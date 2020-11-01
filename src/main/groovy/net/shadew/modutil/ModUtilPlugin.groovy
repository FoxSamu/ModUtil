/*
 * Copyright (c) 2020 Shadew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shadew.modutil

import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import net.shadew.modutil.changelog.MarkdownGenTask
import net.shadew.modutil.changelog.VersionJsonTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile

class ModUtilPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def ext = project.extensions.create("modutil", ModUtilExtension, project)

        if (project.pluginManager.findPlugin("java") == null) {
            project.pluginManager.apply "java"
        }

        def shade = project.container(ShadeRenameTask) { String jarName ->
            def name = jarName.substring(0, 1).toUpperCase() + jarName.substring(1)

            def reobfJarName = 'reobf' + name
            def shadeJarName = 'shade' + name

            def task = project.tasks.create(shadeJarName, ShadeRenameTask) {
                group = 'modutil'
                remapper = ext.shadeRemapper
            }
            project.tasks.assemble.dependsOn task

            project.afterEvaluate {
                def jar = project.tasks[jarName]
                def reobfJar = project.tasks[reobfJarName]

                if (!(jar instanceof Jar)) {
                    throw new IllegalStateException("$jarName is not a jar task. Can only shade jars!")
                }

                if (!(reobfJar instanceof RenameJarInPlace)) {
                    // This jar task is not reobfed, so we shade it directly
                    project.logger.info "$reobfJarName is not found. Shading $jarName output directly"

                    // Actually our input is our output now, so we shade into a separate output file since we can't
                    // modify a file while reading it in the meantime
                    def jarOut = jar.archivePath
                    def shadeOut = project.file "build/$shadeJarName/output.jar"

                    task.setInput jarOut
                    task.setOutput shadeOut

                    // After shading, copy the output file back in place (which
                    task.doLast {
                        FileInputStream fis
                        FileOutputStream fos
                        try {
                            fis = new FileInputStream(shadeOut)
                            fos = new FileOutputStream(jarOut)
                            fos.write fis.bytes
                        } finally {
                            if(fis) fis.close()
                            if(fos) fos.close()
                        }
                    }
                    task.dependsOn jar
                } else {
                    // This jar task is reobfed, so we shade the reobfed jar
                    task.setInput project.file("build/$reobfJarName/output.jar")
                    task.setOutput(jar.archivePath)
                    task.dependsOn(reobfJar)
                }
            }

            return task
        }
        project.extensions.add('shade', shade)

        def java = project.convention.plugins.java as JavaPluginConvention

        // TODO:
        //  - Inject constants in resources

        def injectConstants = project.tasks.create('injectConstants', ConstantInjectionTask) {
            group = 'modutil'
            from java.sourceSets.main.allSource
            into "$project.buildDir/sources/java/main"
            annotation ext.constantAnnotation
            constants {
                ext.getConstant()
            }
        }

        def compileJava = project.tasks.compileJava as JavaCompile
        compileJava.source = "$project.buildDir/sources/java/main/"
        compileJava.dependsOn injectConstants

        if (project == project.rootProject) {
            project.tasks.create('updateVersionJson', VersionJsonTask) {
                group = 'modutil'
                project.afterEvaluate {
                    jsonFile = ext.updateJson
                    info = ext.changelogInfo
                }
            }

            project.tasks.create('genChangelogMarkdown', MarkdownGenTask) {
                group = 'modutil'
                project.afterEvaluate{
                    markdownOut = ext.markdownChangelog
                    info = ext.changelogInfo
                }
            }

            project.tasks.create('makeVersionInfo', DefaultTask) {
                group = 'modutil'
                dependsOn 'genChangelogMarkdown'
                dependsOn 'updateVersionJson'
            }
        }
    }
}
