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

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

public class ShadeRemapper extends Remapper {
    private final HashMap<String, String> packageRenames = new HashMap<>();

    public void addPackageRename(String oldName, String newName) {
        packageRenames.put(oldName, newName);
    }

    @Override
    public String map(String internalName) {
        for (Map.Entry<String, String> entry : packageRenames.entrySet()) {
            if (internalName.startsWith(entry.getKey())) {
                String sub = internalName.substring(entry.getKey().length());
                if (sub.startsWith("/")) {
                    return entry.getValue() + sub;
                }
            }
        }
        return internalName;
    }
}
