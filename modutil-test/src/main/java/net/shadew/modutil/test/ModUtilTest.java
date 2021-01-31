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

package net.shadew.modutil.test;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.shadew.util.data.Pair;

@Mod("modutiltest")
public class ModUtilTest {
    @DynamicConstant("test_static_1")
    public static final String TEST_STATIC_1 = "<NOT INJECTED>";
    @DynamicConstant("test_static_2")
    public static final String TEST_STATIC_2 = "<NOT INJECTED>";
    @DynamicConstant("test_static_3")
    public static final String TEST_STATIC_3 = "<NOT INJECTED>";

    @DynamicConstant("test_map_1")
    public static final String TEST_MAP_1 = "<NOT INJECTED>";
    @DynamicConstant("test_map_2")
    public static final String TEST_MAP_2 = "<NOT INJECTED>";
    @DynamicConstant("test_map_3")
    public static final String TEST_MAP_3 = "<NOT INJECTED>";

    @DynamicConstant("test_dynamic_1")
    public static final String TEST_DYNAMIC_1 = "<NOT INJECTED>";
    @DynamicConstant("test_dynamic_2")
    public static final String TEST_DYNAMIC_2 = "<NOT INJECTED>";
    @DynamicConstant("test_dynamic_3")
    public static final String TEST_DYNAMIC_3 = "<NOT INJECTED>";

    @DynamicConstant("test_properties_1")
    public static final String TEST_PROPERTIES_1 = "<NOT INJECTED>";
    @DynamicConstant("test_properties_2")
    public static final String TEST_PROPERTIES_2 = "<NOT INJECTED>";
    @DynamicConstant("test_properties_3")
    public static final String TEST_PROPERTIES_3 = "<NOT INJECTED>";

    public ModUtilTest() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void load(FMLCommonSetupEvent event) {
        System.out.println("test_static_1:      " + TEST_STATIC_1);
        System.out.println("test_static_2:      " + TEST_STATIC_2);
        System.out.println("test_static_3:      " + TEST_STATIC_3);
        System.out.println("test_map_1:         " + TEST_MAP_1);
        System.out.println("test_map_2:         " + TEST_MAP_2);
        System.out.println("test_map_3:         " + TEST_MAP_3);
        System.out.println("test_properties_1:  " + TEST_PROPERTIES_1);
        System.out.println("test_properties_2:  " + TEST_PROPERTIES_2);
        System.out.println("test_properties_3:  " + TEST_PROPERTIES_3);

        // To test package shading
        Pair<String, String> testPair = Pair.of("Hello", "world");
        System.out.println(testPair);
    }
}
