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

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.copy.CopyActionExecuter;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.WorkResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConstantInjectionTask extends Copy {
    private boolean ignoreCache;

    @SuppressWarnings("unchecked")
    public ConstantInjectionTask() {
        getOutputs().upToDateWhen(o -> checkUpToDate());
    }

    public void setIgnoreCache(boolean ignoreCache) {
        this.ignoreCache = ignoreCache;
    }

    public boolean getIgnoreCache() {
        return ignoreCache;
    }

    public void ignoreCache(boolean on) {
        setIgnoreCache(on);
    }

    public void ignoreCache() {
        setIgnoreCache(true);
    }

    private boolean checkUpToDate() {
        if (ignoreCache) return false;

        File cache = new File(getProject().getBuildDir(), getName() + "/constantscache.txt");
        if (!cache.exists()) return false;

        Map<String, String> fileConsts = new HashMap<>();
        Pattern separatorPattern = Pattern.compile("(?<!\\\\)=");

        try (Scanner scanner = new Scanner(cache)) {
            while (scanner.hasNextLine()) {
                String ln = scanner.nextLine();
                Matcher matcher = separatorPattern.matcher(ln);
                String key, value = null;
                if (matcher.find()) {
                    key = ln.substring(0, matcher.start());
                    value = ln.substring(matcher.end());
                } else {
                    key = ln;
                }

                fileConsts.put(key, value);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        ConstantsExtension extension = getProject().getExtensions().getByType(ConstantsExtension.class);
        for (Map.Entry<String, String> e : fileConsts.entrySet()) {
            Object c = extension.getConstant(e.getKey());
            String cval = c == null ? null : c.toString();
            if (!Objects.equals(e.getValue(), cval)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected InjectingCopyAction createCopyAction() {
        File destinationDir = getDestinationDir();
        if (destinationDir == null) {
            throw new InvalidUserDataException("No copy destination directory has been specified, use 'into' to specify a target directory.");
        } else {
            return new InjectingCopyAction(getFileLookup().getFileResolver(destinationDir), getProject());
        }
    }

    @Override
    protected void copy() {
        CopyActionExecuter copyActionExecuter = createCopyActionExecuter();
        InjectingCopyAction copyAction = createCopyAction();
        WorkResult didWork = copyActionExecuter.execute(getRootSpec(), copyAction);
        setDidWork(didWork.getDidWork());

        if (getDidWork() && !ignoreCache) {
            Map<String, String> computed = copyAction.getComputedConstants();
            File cacheFile = new File(getProject().getBuildDir(), getName() + "/constantscache.txt");

            cacheFile.getParentFile().mkdirs();
            try (PrintStream stream = new PrintStream(cacheFile)) {
                for (Map.Entry<String, String> e : computed.entrySet()) {
                    String n = e.getKey().replace("=", "\\=");
                    String v = e.getValue();

                    stream.print(n);
                    if (v != null) {
                        stream.print("=");
                        stream.print(v);
                    }
                    stream.println();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
