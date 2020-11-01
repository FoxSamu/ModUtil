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

package net.shadew.modutil;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.shadew.modutil.changelog.ChangelogInfo;

public class ModUtilExtension extends GroovyObjectSupport {
    protected final Project project;
    protected final ShadeRemapper shadeRemapper = new ShadeRemapper();
    protected final List<Function<String, Object>> constants = new ArrayList<>();
    protected String constantAnnotation;
    protected ChangelogInfo info;
    protected File updateJson;
    protected final List<File> markdownOutput = new ArrayList<>();

    public ModUtilExtension(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void shade(String fromPackage, String toPackage) {
        shadeRemapper.addPackageRename(fromPackage.replace('.', '/'), toPackage.replace('.', '/'));
    }

    public ShadeRemapper getShadeRemapper() {
        return shadeRemapper;
    }

    public List<Function<String, Object>> getConstants() {
        return Collections.unmodifiableList(constants);
    }

    public Object getConstant(String name) {
        for (Function<String, Object> fn : constants) {
            Object val = fn.apply(name);
            if (val != null) return val;
        }
        return null;
    }

    public void constantAnnotation(String annotation) {
        constantAnnotation = annotation;
    }

    public String getConstantAnnotation() {
        return constantAnnotation;
    }

    public void setConstantAnnotation(String constantAnnotation) {
        this.constantAnnotation = constantAnnotation;
    }

    public void constant(String name, Object value) {
        constants.add(key -> key.equals(name) ? value : null);
    }

    public void constants(Function<String, Object> fn) {
        constants.add(fn);
    }

    public void constants(Map<String, Object> map) {
        constants.add(map::get);
    }

    public void constants(Closure<Object> closure) {
        constants.add(closure::call);
    }

    public void changelogJson(File changelogJson) {
        try {
            info = ChangelogInfo.load(changelogJson);
        } catch (FileNotFoundException exc) {
            exc.printStackTrace();
            throw new UncheckedIOException(exc);
        }
    }

    public void changelogInfo(ChangelogInfo info) {
        this.info = info;
    }

    public ChangelogInfo getChangelogInfo() {
        return info;
    }

    public void updateJson(File updateJson) {
        this.updateJson = updateJson;
    }

    public File getUpdateJson() {
        return updateJson;
    }

    public void markdownChangelog(File out) {
        markdownOutput.add(out);
    }

    public List<File> getMarkdownChangelog() {
        return markdownOutput;
    }
}
