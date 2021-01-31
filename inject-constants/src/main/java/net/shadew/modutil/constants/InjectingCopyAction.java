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

import groovy.lang.Closure;
import org.gradle.api.Project;
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
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.TypeHolderSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InjectingCopyAction implements CopyAction {
    private final Map<String, String> computedConstants = new HashMap<>();

    private final PathToFileResolver fileResolver;
    private final Project project;

    public InjectingCopyAction(PathToFileResolver fileResolver, Project project) {
        this.fileResolver = fileResolver;
        this.project = project;
    }

    public Map<String, String> getComputedConstants() {
        return computedConstants;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        InjectingCopyDetailsInternalAction action = new InjectingCopyDetailsInternalAction();
        stream.process(action);
        return WorkResults.didWork(action.didWork);
    }

    private class InjectingCopyDetailsInternalAction implements CopyActionProcessingStreamAction {
        private boolean didWork;

        private InjectingCopyDetailsInternalAction() {
        }

        @Override
        public void processFile(FileCopyDetailsInternal details) {
            File target = fileResolver.resolve(details.getRelativePath().getPathString());

            if (details.isDirectory()) {
                target.mkdirs();
                didWork = true;
                return;
            }

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                details.copyTo(baos);

                target.getParentFile().mkdirs();
                try (PrintStream stream = new PrintStream(target)) {
                    if (details.getRelativePath().getPathString().endsWith(".java")) {
                        JavaUnit unit = Roaster.parseUnit(baos.toString());
                        for (JavaType<?> type : unit.getTopLevelTypes()) {
                            if (type instanceof FieldHolderSource<?>) {
                                FieldHolderSource<?> src = (FieldHolderSource<?>) type;
                                process(src);
                                stream.println(src);
                            } else {
                                stream.println(type);
                            }
                        }
                    } else {
                        String raw = baos.toString();
                        String repl = modifyResource(details.getRelativePath().getPathString(), raw);
                        stream.println(repl == null ? raw : repl);
                    }
                }

                didWork = true;
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        private void process(FieldHolderSource<?> src) {
            modifyJava(src);

            if (src instanceof TypeHolderSource<?>) {
                TypeHolderSource<?> parent = (TypeHolderSource<?>) src;
                for (int i = 0, l = parent.getNestedTypes().size(); i < l; i++) {
                    JavaSource<?> inner = parent.getNestedTypes().get(i);
                    if (inner instanceof FieldHolderSource<?>) {
                        process((FieldHolderSource<?>) inner);
                        parent.getNestedTypes().set(i, inner);
                    }
                }
            }
        }
    }


    private FieldHolderSource<?> modifyJava(FieldHolderSource<?> src) {
        src.getFields()
           .stream()
           .filter(field -> field.isStatic() && field.isFinal())
           .forEach(
               field -> {
                   ConstantsExtension extension = project.getExtensions().getByType(ConstantsExtension.class);

                   Optional<? extends AnnotationSource<?>> annotation
                       = field.getAnnotations()
                              .stream()
                              .filter(annot -> annot.getQualifiedName().equals(extension.getAnnotation()))
                              .findFirst();

                   String annField = extension.getAnnotationField();

                   annotation.ifPresent(ann -> {
                       String value = annField.equals("value") ? ann.getStringValue() : ann.getStringValue(annField);
                       if (value == null && annField.equals("value")) value = ann.getStringValue("value");
                       Object fv = extension.getConstant(value);
                       if (fv instanceof Supplier<?>) {
                           fv = ((Supplier<?>) fv).get();
                       } else if (fv instanceof Closure<?>) {
                           Closure<?> cl = (Closure<?>) fv;
                           fv = cl.call();
                       }

                       computedConstants.put(value, fv == null ? null : fv.toString());

                       if (fv instanceof String)
                           field.setStringInitializer((String) fv);
                       else {
                           if (fv instanceof Integer) {
                               field.setLiteralInitializer(fv + "");
                           } else if (fv instanceof Long) {
                               field.setLiteralInitializer(fv + "L");
                           } else if (fv instanceof Float) {
                               field.setLiteralInitializer(fv + "F");
                           } else if (fv instanceof Double) {
                               field.setLiteralInitializer(fv + "D");
                           } else if (fv != null && field.getType().getQualifiedName().equals("java.lang.String")) {
                               field.setStringInitializer(fv.toString());
                           }
                       }
                   });
               }
           );
        return src;
    }

    private String modifyResource(String path, String content) {
        ConstantsExtension extension = project.getExtensions().getByType(ConstantsExtension.class);

        String[] contentArr = {content};
        Map<Pattern, Pattern> patterns = extension.getResourcePatterns();
        patterns.forEach((filePattern, contentPattern) -> {
            if (filePattern.matcher(path).matches()) {
                StringBuffer replaced = new StringBuffer();
                Matcher matcher = contentPattern.matcher(content);

                while (matcher.find()) {
                    String constName;
                    if (matcher.groupCount() == 0) {
                        constName = matcher.group();
                    } else {
                        constName = null;
                        int i = 1;

                        while (constName == null && i <= matcher.groupCount()) {
                            constName = matcher.group(i);
                            i++;
                        }

                        if (constName == null) {
                            constName = matcher.group();
                        }
                    }

                    Object repl = extension.getConstant(constName);
                    if (repl instanceof Supplier<?>) {
                        repl = ((Supplier<?>) repl).get();
                    } else if (repl instanceof Closure<?>) {
                        Closure<?> cl = (Closure<?>) repl;
                        repl = cl.call();
                    }


                    computedConstants.put(constName, repl == null ? null : repl.toString());
                    if (repl != null) {
                        matcher.appendReplacement(replaced, repl.toString());
                    } else {
                        matcher.appendReplacement(replaced, matcher.group());
                    }
                }

                matcher.appendTail(replaced);
                contentArr[0] = replaced.toString();
            }
        });

        return contentArr[0];
    }
}
