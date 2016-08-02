/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.app.environment.impl;

import org.apache.eagle.app.Application;
import org.apache.eagle.app.ApplicationConfig;
import org.apache.eagle.app.environment.ExecutionRuntime;
import org.apache.eagle.app.environment.ExecutionRuntimeProvider;

public class SparkExecutionRuntime implements ExecutionRuntime<SparkEnvironment> {
    @Override
    public void prepare(SparkEnvironment environment) {

    }

    @Override
    public void start(Application executor, ApplicationConfig config) {

        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void stop(Application executor, ApplicationConfig config) {

        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void status(Application executor, ApplicationConfig config) {

        throw new RuntimeException("Not implemented yet");
    }

    public static class Provider implements ExecutionRuntimeProvider<SparkEnvironment> {
        @Override
        public SparkExecutionRuntime get() {
            return new SparkExecutionRuntime();
        }
    }
}