// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.conniey;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.Entity;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import java.io.IOException;
import java.util.Scanner;

/**
 * Sample demonstrates how to:
 *
 * <ul>
 * <li>Authenticate to Azure Storage Tables with Azure AD.</li>
 * <li>Create a table and an entity.</li>
 * </ul>
 */
public class Program {
    private static final Scanner SCANNER = new Scanner(System.in);

    // Names of keys in Key Vault to fetch secrets from.
    private static final String KEY_VAULT_URL = "https://connieykv.vault.azure.net/";
    private static final String TABLES_SAS_TOKEN_KEY = "connieystorage-tablesdemo";
    private static final String TABLES_URL_KEY = "tablesurl";

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

        System.out.println("Fetching sas token and tables url from Key Vault.");
        final KeyVaultSecret sasToken = secretClient.getSecret(TABLES_SAS_TOKEN_KEY);
        final KeyVaultSecret tablesUrl = secretClient.getSecret(TABLES_URL_KEY);

        final TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .endpoint(tablesUrl.getValue())
            .sasToken(sasToken.getValue())
            .buildClient();

        final String tableName = getUserInput("Enter name of table");
        System.out.printf("Creating table '%s'.%n", tableName);

        final TableClient tableClient = tableServiceClient.getTableClient(tableName);
        tableClient.create();

        final String partitionKey = getUserInput("Enter partition key");
        final String rowKey = getUserInput("Enter row key");
        final Entity entity = new Entity(partitionKey, rowKey);

        final Entity createdEntity = tableClient.createEntity(entity);

        System.out.printf("Added table entity. Partition key: %s, row key: %s, ETag: %s.%n",
            createdEntity.getPartitionKey(), createdEntity.getRowKey(), createdEntity.getETag());

        System.out.println("Finished.");
    }

    private static String getUserInput(String prompt) {
        System.out.print(prompt + ": ");
        return SCANNER.nextLine();
    }
}
