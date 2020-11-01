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

package net.shadew.modutil.changelog;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class MarkdownGenTask extends DefaultTask {
    private ChangelogInfo info;
    private final List<File> markdownOut = new ArrayList<>();

    public void setInfo(ChangelogInfo info) {
        this.info = info;
    }

    public void setMarkdownOut(File markdownOut) {
        this.markdownOut.clear();
        this.markdownOut.add(markdownOut);
    }

    public void setMarkdownOut(List<File> markdownOut) {
        this.markdownOut.clear();
        this.markdownOut.addAll(markdownOut);
    }

    public ChangelogInfo getInfo() {
        return info;
    }

    public List<File> getMarkdownOut() {
        return markdownOut;
    }

    public void markdown(File out) {
        this.markdownOut.add(out);
    }

    public void changelog(ChangelogInfo info) {
        setInfo(info);
    }

    @TaskAction
    private void invoke() {
        for (File out : markdownOut) {
            MarkdownChangelogGenerator gen = new MarkdownChangelogGenerator(info, out);
            try {
                gen.generate();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
