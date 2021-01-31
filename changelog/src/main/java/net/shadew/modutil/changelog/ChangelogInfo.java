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

import com.google.gson.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import net.shadew.util.misc.Version;

public class ChangelogInfo {
    private Version version;
    private String versionName;
    private String description;
    private String mcversion;
    private boolean stable;
    private final List<String> changelog = new ArrayList<>();

    public void parseVersionNumber(String version) {
        this.version = Version.parse(version);
    }

    public String getVersionNumber() {
        return version.toString();
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public void addChangelog(String item) {
        changelog.add(item);
    }

    public List<String> getChangelog() {
        return changelog;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setMcversion(String mcversion) {
        this.mcversion = mcversion;
    }

    public String getMcversion() {
        return mcversion;
    }

    public boolean isStable() {
        return stable;
    }

    public void setStable(boolean stable) {
        this.stable = stable;
    }

    public static ChangelogInfo parseChangelog(JsonElement element) {
        if (!element.isJsonObject()) throw new JsonSyntaxException("changelog.json: Expected an object");
        JsonObject object = element.getAsJsonObject();
        JsonElement version = object.get("version");
        JsonElement changelog = object.get("changelog");
        JsonElement description = object.get("description");

        if (version == null) throw new JsonSyntaxException("changelog.json: Missing 'version'");
        if (!version.isJsonObject()) throw new JsonSyntaxException("changelog.json: 'version' is not an object");
        JsonObject versionObject = version.getAsJsonObject();

        ChangelogInfo info = new ChangelogInfo();
        JsonElement number = versionObject.get("number");
        if (number.isJsonPrimitive()) {
            try {
                info.parseVersionNumber(number.getAsString());
            } catch (IllegalArgumentException nfe) {
                nfe.printStackTrace();
                throw new JsonSyntaxException("changelog.json: Invalid version number format");
            }
        } else {
            throw new JsonSyntaxException("changelog.json: Version number not a string");
        }

        JsonElement name = versionObject.get("name");
        if (name == null)
            throw new JsonSyntaxException("changelog.json: Missing version name");
        if (!name.isJsonPrimitive())
            throw new JsonSyntaxException("changelog.json: Version name is not a string");
        info.setVersionName(name.getAsString());

        JsonElement minecraft = versionObject.get("minecraft");
        if (minecraft == null)
            throw new JsonSyntaxException("changelog.json: Missing minecraft version");
        if (!minecraft.isJsonPrimitive())
            throw new JsonSyntaxException("changelog.json: Minecraft version is not a string");
        info.setMcversion(minecraft.getAsString());

        JsonElement stable = versionObject.get("stable");
        if (stable == null)
            throw new JsonSyntaxException("changelog.json: Missing version stability");
        if (!stable.isJsonPrimitive())
            throw new JsonSyntaxException("changelog.json: Version stability is not a boolean");
        info.setStable(stable.getAsBoolean());

        if (changelog != null) {
            if (!changelog.isJsonArray()) throw new JsonSyntaxException("changelog.json: Changelog is not an array");
            JsonArray changelogArray = changelog.getAsJsonArray();

            for (JsonElement entry : changelogArray) {
                if (!entry.isJsonPrimitive())
                    throw new JsonSyntaxException("changelog.json: Changelog entry is not a string");
                info.addChangelog(entry.getAsString());
            }
        }

        if (description != null) {
            if (description.isJsonPrimitive()) {
                info.setDescription(description.getAsString());
            } else if (description.isJsonArray()) {
                JsonArray descriptionArray = description.getAsJsonArray();
                List<String> lines = new ArrayList<>();
                for (JsonElement entry : descriptionArray) {
                    if (!entry.isJsonPrimitive())
                        throw new JsonSyntaxException("changelog.json: Description entry is not a string");
                    lines.add(entry.getAsString());
                }
                info.setDescription(String.join(" ", lines));
            } else {
                throw new JsonSyntaxException("changelog.json: Description is not a string or array");
            }
        }

        return info;
    }

    public static ChangelogInfo load(File file) throws FileNotFoundException {
        JsonElement el = JsonParser.parseReader(new FileReader(file));
        if (!el.isJsonObject()) throw new JsonParseException("changelog.json: Root element not an object");
        try {
            return parseChangelog(el.getAsJsonObject());
        } catch (Throwable thr) {
            thr.printStackTrace();
            throw new RuntimeException(thr);
        }
    }
}
