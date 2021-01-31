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

package net.shadew.modutil.constants;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;

public class InjectConstantsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("constants", ConstantsExtension.class, project);

        if (project.getPluginManager().findPlugin("java") != null) {
            JavaPluginConvention java = (JavaPluginConvention) project.getConvention().getPlugins().get("java");
            for (SourceSet set : java.getSourceSets()) {
                String taskName = set.getTaskName("inject", "constants");

                ConstantInjectionTask injectConstants = project.getTasks().create(taskName, ConstantInjectionTask.class, t -> {
                    t.setGroup("other");
                    t.setDestinationDir(new File(project.getBuildDir() + "/sources/"));
                    for (File file : set.getAllJava().getSrcDirs()) {
                        t.from(file, spec -> spec.into(project.getBuildDir() + "/sources/" + set.getName() + "/java/"));
                    }

                    for (File file : set.getResources().getSrcDirs()) {
                        t.from(file, spec -> spec.into(project.getBuildDir() + "/sources/" + set.getName() + "/resources/"));
                    }
                });

                JavaCompile compileJava = (JavaCompile) project.getTasks().getByName(set.getCompileJavaTaskName());
                compileJava.setSource(project.getBuildDir() + "/sources/" + set.getName() + "/java/");
                compileJava.dependsOn(injectConstants);

                ProcessResources processResources = project.getTasks().replace(set.getProcessResourcesTaskName(), ProcessResources.class);
                processResources.from(project.getBuildDir() + "/sources/" + set.getName() + "/resources/");
                processResources.into(set.getOutput().getResourcesDir());
                processResources.dependsOn(injectConstants);

                Task classes = project.getTasks().getByName(set.getClassesTaskName());
                classes.dependsOn(processResources);
            }
        }
    }
}
