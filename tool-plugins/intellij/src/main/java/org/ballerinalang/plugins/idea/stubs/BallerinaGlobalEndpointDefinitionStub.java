/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.ballerinalang.plugins.idea.stubs;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.ballerinalang.plugins.idea.psi.BallerinaGlobalEndpointDefinition;

/**
 * Stub for global endpoint definition.
 */
public class BallerinaGlobalEndpointDefinitionStub extends BallerinaNamedStub<BallerinaGlobalEndpointDefinition> {

    public BallerinaGlobalEndpointDefinitionStub(StubElement parent, IStubElementType elementType, StringRef name,
                                                 boolean isPublic) {
        super(parent, elementType, name, isPublic);
    }

    public BallerinaGlobalEndpointDefinitionStub(StubElement parent, IStubElementType elementType, String name,
                                                 boolean isPublic) {
        super(parent, elementType, name, isPublic);
    }
}
