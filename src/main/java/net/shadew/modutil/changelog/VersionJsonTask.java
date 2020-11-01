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

public class VersionJsonTask extends DefaultTask {
    private ChangelogInfo info;
    private File jsonFile;

    public void setInfo(ChangelogInfo info) {
        this.info = info;
    }

    public ChangelogInfo getInfo() {
        return info;
    }

    public void setJsonFile(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    public File getJsonFile() {
        return jsonFile;
    }

    public void changelog(ChangelogInfo info) {
        setInfo(info);
    }

    public void updateJson(File json) {
        setJsonFile(json);
    }

    @TaskAction
    private void invoke() {
        try {
            VersionJsonGenerator gen = VersionJsonGenerator.loadFrom(jsonFile, info);
            gen.update();
            gen.saveTo(jsonFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}