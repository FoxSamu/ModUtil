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

import org.gradle.api.DefaultTask;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class ShadeRenameTask extends DefaultTask {
    private File input;
    private File output;
    private ShadeRemapper remapper;

    public ShadeRenameTask() {

        doLast(task -> {
            try {
                JarOutputStream out = new JarOutputStream(new FileOutputStream(output));

                JarFile jar = new JarFile(input);
                jar.stream().forEach(entry -> {
                    try {
                        out.putNextEntry(new JarEntry(this.remapper.map(entry.getName())));
                        if (entry.getName().endsWith(".class")) {
                            ClassReader reader = new ClassReader(jar.getInputStream(entry));
                            ClassWriter writer = new ClassWriter(0);
                            ClassRemapper remapper = new ClassRemapper(writer, this.remapper);
                            reader.accept(remapper, ClassReader.EXPAND_FRAMES);
                            out.write(writer.toByteArray());
                        } else {
                            InputStream is = jar.getInputStream(entry);
                            byte[] buf = new byte[1024];
                            int r;
                            while ((r = is.read(buf)) != -1) {
                                out.write(buf, 0, r);
                            }
                        }
                        out.closeEntry();
                    } catch (Throwable thr) {
                        thr.printStackTrace();
                        throw new RuntimeException(thr);
                    }
                });
                out.close();
            } catch (IOException exc) {
                exc.printStackTrace();
                throw new UncheckedIOException(exc);
            }
        });
    }

    public void setRemapper(ShadeRemapper remapper) {
        this.remapper = remapper;
    }

    public ShadeRemapper getRemapper() {
        return remapper;
    }

    public void setInput(File input) {
        this.input = input;
    }

    public void setOutput(File output) {
        output.getParentFile().mkdirs();
        this.output = output;
    }

    public File getInput() {
        return input;
    }

    public File getOutput() {
        return output;
    }
}
