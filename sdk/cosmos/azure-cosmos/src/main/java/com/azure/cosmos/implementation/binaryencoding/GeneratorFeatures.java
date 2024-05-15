// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.FormatFeature;

public enum GeneratorFeatures implements FormatFeature {
    ;

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public int getMask() {
        return 0;
    }

    @Override
    public boolean enabledIn(int i) {
        return false;
    }
}
