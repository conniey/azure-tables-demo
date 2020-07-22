// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.conniey;

import com.microsoft.azure.keyvault;
import com.microsoft.azure.storage.table.CloudTableClient;

/**
 * Sample demonstrates how to:
 *
 * <ul>
 * <li>Authenticate to Azure Storage Tables with Azure AD.</li>
 * <li>Create a table and an entity.</li>
 * </ul>
 */
public class Program {
    /**
     * Main method to invoke this demo.
     *
     * @param args Unused arguments to the program.
     */
    public static void main(String[] args) {

        CloudTableClient client = new CloudTableClient();
    }
}
