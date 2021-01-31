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

package net.shadew.modutil.changelog;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ChangelogExtension extends GroovyObjectSupport {
    protected final Project project;
    protected String constantAnnotation;
    protected Supplier<ChangelogInfo> info;
    protected final List<File> updateJsons = new ArrayList<>();
    protected final List<File> markdownOutput = new ArrayList<>();
    protected final List<ChangelogGenerator> changelogGenerators = new ArrayList<>();

    public ChangelogExtension(Project project) {
        this.project = project;
    }

    public void changelogJson(File changelogJson) {
        info = () -> {
            try {
                return ChangelogInfo.load(changelogJson);
            } catch (FileNotFoundException exc) {
                exc.printStackTrace();
                throw new UncheckedIOException(exc);
            }
        };
    }

    public void changelogInfo(ChangelogInfo info) {
        this.info = () -> info;
    }

    public void changelogInfo(Closure<?> closure) {
        this.info = () -> {
            ChangelogInfo info = new ChangelogInfo();
            closure.call(info);
            return info;
        };
    }

    public Supplier<ChangelogInfo> getChangelogInfo() {
        return () -> info.get(); // Wrap so it updates dynamically on subsequent setter calls
    }

    public void updateJson(Object updateJson) {
        generator(new VersionJsonGenerator(getChangelogInfo(), project.file(updateJson)));
    }

    public void updateJson(File updateJson) {
        generator(new VersionJsonGenerator(getChangelogInfo(), updateJson));
    }

    public List<File> getUpdateJsons() {
        return updateJsons;
    }

    public void markdownChangelog(Object out) {
        generator(new MarkdownChangelogGenerator(getChangelogInfo(), project.file(out)));
    }

    public void markdownChangelog(File out) {
        generator(new MarkdownChangelogGenerator(getChangelogInfo(), out));
    }

    public List<File> getMarkdownChangelog() {
        return markdownOutput;
    }

    public List<ChangelogGenerator> getGenerators() {
        return changelogGenerators;
    }

    public void generator(ChangelogGenerator generator) {
        changelogGenerators.add(generator);
    }
}
