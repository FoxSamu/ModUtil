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
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ConstantsExtension extends GroovyObjectSupport {
    private final Project project;

    private String annotation;
    private String annotationField = "value";

    private final ArrayList<Function<String, Object>> constants = new ArrayList<>();
    private final Map<Pattern, Pattern> resourcePatterns = new LinkedHashMap<>();

    public ConstantsExtension(Project proj) {
        project = proj;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setAnnotationField(String annotationField) {
        this.annotationField = annotationField;
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getAnnotationField() {
        return annotationField;
    }

    public void annotation(String ann) {
        setAnnotation(ann);
        setAnnotationField("value");
    }

    public void annotation(String ann, String field) {
        setAnnotation(ann);
        setAnnotationField(field);
    }

    public List<Function<String, Object>> getConstants() {
        return constants;
    }

    public Object getConstant(String name) {
        for (Function<String, Object> fn : constants) {
            Object val = fn.apply(name);
            if (val != null)
                return val;
        }
        return null;
    }

    public void constant(String name, Object value) {
        constants.add(key -> name.equals(key) ? value : null);
    }

    public void constants(Function<String, Object> fn) {
        constants.add(fn);
    }

    public void constants(Map<String, Object> map) {
        constants.add(map::get);
    }

    public void constants(Closure<?> closure) {
        constants.add(closure::call);
    }

    public void constantsFromProperties() {
        constants.add(project::findProperty);
    }

    public Map<Pattern, Pattern> getResourcePatterns() {
        return resourcePatterns;
    }

    public void pattern(String filePattern, String constantPattern) {
        resourcePatterns.put(Pattern.compile(filePattern), Pattern.compile(constantPattern));
    }
}
