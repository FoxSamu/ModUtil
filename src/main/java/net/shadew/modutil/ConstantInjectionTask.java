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
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Copy;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConstantInjectionTask extends Copy {
    private String annotation;
    private String annotArgument = "value";
    private Function<String, Object> constants;

    public ConstantInjectionTask() {

    }

    @Override
    protected CopyAction createCopyAction() {
        File destinationDir = getDestinationDir();
        if (destinationDir == null) {
            throw new InvalidUserDataException("No copy destination directory has been specified, use 'into' to specify a target directory.");
        } else {
            return new RoasterCopyAction(getFileLookup().getFileResolver(destinationDir), this::modify);
        }
    }

    public void constants(Function<String, Object> constants) {
        this.constants = constants;
    }

    public void constants(Map<String, Object> constants) {
        this.constants = constants::get;
    }

    public void constants(Closure<Object> constants) {
        this.constants = constants::call;
    }

    public void annotation(String annotation) {
        this.annotation = annotation;
    }

    public void argument(String argument) {
        this.annotArgument = argument;
    }

    private JavaClassSource modify(JavaClassSource src) {
        src.getFields()
           .stream()
           .filter(field -> field.isStatic() && field.isFinal() && field.getLiteralInitializer() != null)
           .forEach(
               field -> {
                   Optional<AnnotationSource<JavaClassSource>> annotation
                       = field.getAnnotations()
                              .stream()
                              .filter(annot -> annot.getQualifiedName().equals(this.annotation))
                              .findFirst();

                   annotation.ifPresent(annot -> {
                       String value = annot.getStringValue(annotArgument);
                       Object fv = constants.apply(value);
                       if (fv instanceof Supplier<?>) {
                           fv = ((Supplier<?>) fv).get();
                       }
                       if (fv instanceof Closure<?>) {
                           Closure<?> cl = (Closure<?>) fv;
                           fv = cl.call();
                       }
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
                           }
                       }
                   });
               }
           );
        return src;
    }
}
