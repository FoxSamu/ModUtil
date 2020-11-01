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

import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;
import org.gradle.internal.file.PathToFileResolver;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.function.Function;

public class RoasterCopyAction implements CopyAction {
    private final PathToFileResolver fileResolver;
    private final Function<JavaClassSource, JavaClassSource> transformer;

    public RoasterCopyAction(PathToFileResolver fileResolver, Function<JavaClassSource, JavaClassSource> transformer) {
        this.fileResolver = fileResolver;
        this.transformer = transformer;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        RoasterCopyDetailsInternalAction action = new RoasterCopyDetailsInternalAction();
        stream.process(action);
        return WorkResults.didWork(action.didWork);
    }

    private class RoasterCopyDetailsInternalAction implements CopyActionProcessingStreamAction {
        private boolean didWork;

        private RoasterCopyDetailsInternalAction() {
        }

        @Override
        public void processFile(FileCopyDetailsInternal details) {
            File target = fileResolver.resolve(details.getRelativePath().getPathString());

            if (details.getRelativePath().getPathString().endsWith(".java")) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    details.copyTo(baos);

                    target.getParentFile().mkdirs();
                    PrintStream stream = new PrintStream(target);

                    JavaUnit unit = Roaster.parseUnit(baos.toString());
                    for (JavaType<?> type : unit.getTopLevelTypes()) {
                        if (type instanceof JavaClassSource) {
                            JavaClassSource src = (JavaClassSource) type;
                            src = process(src);
                            stream.println(src);
                        } else {
                            stream.println(type);
                        }
                    }

                    stream.close();

                    this.didWork = true;
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }

        private JavaClassSource process(JavaClassSource src) {
            src = transformer.apply(src);

            for (int i = 0, l = src.getNestedTypes().size(); i < l; i++) {
                JavaSource<?> inner = src.getNestedTypes().get(i);
                if (inner instanceof JavaClassSource) {
                    inner = process((JavaClassSource) inner);
                    src.getNestedTypes().set(i, inner);
                }
            }

            return src;
        }
    }
}
