/*
 * Copyright (c) 2021 Shadew
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

package net.shadew.modutil.shade

import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class ShadePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def ext = project.extensions.create("shading", ShadingExtension, project)

        if (project.pluginManager.findPlugin("java") == null) {
            project.pluginManager.apply "java"
        }

        def shade = project.container(ShadeRenameTask) { String jarName ->
            def name = jarName.substring(0, 1).toUpperCase() + jarName.substring(1)

            def reobfJarName = 'reobf' + name
            def shadeJarName = 'shade' + name

            def task = project.tasks.create(shadeJarName, ShadeRenameTask) {
                group = 'other'
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

        project.afterEvaluate {
            shade.maybeCreate("jar")
        }
    }
}
