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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VersionJsonGenerator {
    private final JsonObject update;
    private final ChangelogInfo addChangelog;

    public VersionJsonGenerator(JsonObject update, ChangelogInfo addChangelog) {
        this.update = update;
        this.addChangelog = addChangelog;
    }

    public void update() {
        String mcver = addChangelog.getMcversion();
        String mnver = addChangelog.getVersionNumber();

        if (!update.has(mcver)) {
            update.add(mcver, new JsonObject());
        }

        if (!update.has("promos")) {
            update.add("promos", new JsonObject());
        }

        JsonObject mcverReleases = update.getAsJsonObject(mcver);
        mcverReleases.addProperty(mnver, genChangelogString());

        JsonObject promos = update.getAsJsonObject("promos");
        promos.addProperty(mcver + "-latest", mnver);
        if (addChangelog.isStable()) {
            promos.addProperty(mcver + "-recommended", mnver);
        }
    }

    private String genChangelogString() {
        if (addChangelog.getDescription() == null) {
            return String.format("%s - %s", addChangelog.getVersionNumber(), addChangelog.getVersionName());
        }
        return String.format("%s - %s: %s", addChangelog.getVersionNumber(), addChangelog.getVersionName(), addChangelog.getDescription());
    }

    public void saveTo(File file) throws IOException {
        try {
            FileWriter writer = new FileWriter(file);
            JsonWriter out = new JsonWriter(writer);
            out.setIndent("  ");
            Streams.write(update, out);
            out.close();
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public static VersionJsonGenerator loadFrom(File file, ChangelogInfo info) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return new VersionJsonGenerator(new JsonObject(), info);
        }
        try {
            JsonElement element = new JsonParser().parse(new FileReader(file));
            return new VersionJsonGenerator(element.getAsJsonObject(), info);
        } catch (Exception exc) {
            exc.printStackTrace();
            return new VersionJsonGenerator(new JsonObject(), info);
        }
    }
}
