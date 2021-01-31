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

package net.shadew.modutil.shade;

import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;

import java.util.Map;

public class ShadingExtension extends GroovyObjectSupport {
    protected final Project project;
    protected final ShadeRemapper shadeRemapper = new ShadeRemapper();

    public ShadingExtension(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void shade(String fromPackage, String toPackage) {
        shadeRemapper.addPackageRename(fromPackage.replace('.', '/'), toPackage.replace('.', '/'));
    }

    public void shade(Map<String, String> renames) {
        renames.forEach(this::shade);
    }

    public ShadeRemapper getShadeRemapper() {
        return shadeRemapper;
    }
}
