// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.conniey;

import com.azure.data.tables.AzureTable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableEntity;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample demonstrates how to:
 *
 * <ul>
 * <li>Authenticate to Azure Storage Tables with Azure AD.</li>
 * <li>Create a table and an entity.</li>
 * </ul>
 */
public class Program {
    private static final String TABLE_NAME = "MyTable";
    private static final String TABLES_CONNECTION_STRING_KEY = "AZURE_TABLES_CONNECTION_STRING";
    private static final String TABLES_URL_KEY = "AZURE_TABLES_URL";

    private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;
    private static final String KEY_VAULT_URL = System.getenv("AZURE_KEY_VAULT_URL");

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused arguments to the program.
     */
    public static void main(String[] args) throws IOException {
        final DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
        final SecretClient secretClient = new SecretClientBuilder()
            .credential(defaultAzureCredential)
            .vaultUrl(KEY_VAULT_URL)
            .buildClient();

        System.out.println("Fetching connection string from Key Vault.");
        final KeyVaultSecret tablesConnectionString = secretClient.getSecret(TABLES_CONNECTION_STRING_KEY);

        final TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .connectionString(tablesConnectionString.getValue())
            .buildClient();

        System.out.println("Creating tables client for: " + TABLE_NAME);
        final TableClient tableClient = tableServiceClient.getTableClient(TABLE_NAME);

        final Map<String, Object> entityProperties = new HashMap<>();
        entityProperties.put("partitionKey", "my-partition");
        entityProperties.put("rowKey", "my-row");
        final TableEntity entity = tableClient.createEntity(entityProperties);

        System.out.printf("Added table entity. Partition key: %s, row key: %s.%n",
            entity.getPartitionKey(), entity.getRowKey());

        System.out.println("Finished. Press any char to exit.");
        System.in.read();
    }
}
