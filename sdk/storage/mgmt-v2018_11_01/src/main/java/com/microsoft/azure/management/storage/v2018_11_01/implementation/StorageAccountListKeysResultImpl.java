/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.storage.v2018_11_01.implementation;

import com.microsoft.azure.management.storage.v2018_11_01.StorageAccountListKeysResult;
import com.microsoft.azure.arm.model.implementation.WrapperImpl;
import java.util.List;
import com.microsoft.azure.management.storage.v2018_11_01.StorageAccountKey;

class StorageAccountListKeysResultImpl extends WrapperImpl<StorageAccountListKeysResultInner> implements StorageAccountListKeysResult {
    private final StorageManager manager;
    StorageAccountListKeysResultImpl(StorageAccountListKeysResultInner inner, StorageManager manager) {
        super(inner);
        this.manager = manager;
    }

    @Override
    public StorageManager manager() {
        return this.manager;
    }

    @Override
    public List<StorageAccountKey> keys() {
        return this.inner().keys();
    }

}
