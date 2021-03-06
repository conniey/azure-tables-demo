// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.conniey;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.MsalException;
import com.microsoft.aad.msal4j.SilentParameters;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.AuthenticationResult;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableResult;
import com.microsoft.azure.storage.table.TableServiceEntity;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

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

    private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
    private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;

    /**
     * Main method to invoke this demo.
     *
     * @param args Unused arguments to the program.
     */
    public static void main(String[] args) {
        final ITokenCacheAccessAspect tokenCache = new TokenCacheAspect("token.json");

        final KeyVaultClient keyVaultClient = createKeyVaultClient(CLIENT_ID, CLIENT_SECRET, AUTHORITY, tokenCache);
        System.out.println("Fetching sas token and tables url from Key Vault.");
        final SecretBundle sasToken = keyVaultClient.getSecret(KEY_VAULT_URL, TABLES_SAS_TOKEN_KEY);
        final SecretBundle tablesUrl = keyVaultClient.getSecret(KEY_VAULT_URL, TABLES_URL_KEY);
        final StorageCredentials storageCredentials = new StorageCredentialsSharedAccessSignature(sasToken.value());

        System.out.println("Creating client for: " + tablesUrl.value());
        final CloudTableClient tableClient = new CloudTableClient(URI.create(tablesUrl.value()),
            storageCredentials);

        final String tableName = getUserInput("Enter name of table");
        final CloudTable cloudTable;
        try {
            cloudTable = tableClient.getTableReference(tableName);
        } catch (URISyntaxException | StorageException e) {
            throw new RuntimeException("Unable to get table reference.", e);
        }

        System.out.printf("Creating table '%s'.%n", tableName);
        try {
            cloudTable.createIfNotExists();
        } catch (StorageException e) {
            throw new RuntimeException("Unable to create table: " + tableName, e);
        }

        final String partitionKey = getUserInput("Enter partition key");
        final String rowKey = getUserInput("Enter row key");
        final TableServiceEntity entity = new TableServiceEntity(partitionKey, rowKey);

        final TableOperation operation = TableOperation.insert(entity);

        System.out.printf("Adding table entity. Partition key: %s, row key: %s.%n",
            entity.getPartitionKey(), entity.getRowKey());

        final TableResult result;
        try {
            result = cloudTable.execute(operation);
        } catch (StorageException e) {
            throw new RuntimeException("Unable to create entity.", e);
        }

        final TableServiceEntity createdEntity = result.getResultAsType();
        System.out.printf("Added table entity. Partition key: %s, row key: %s, ETag: %s.%n",
            createdEntity.getPartitionKey(), createdEntity.getRowKey(), createdEntity.getEtag());

        System.out.println("Finished.");
    }

    private static KeyVaultClient createKeyVaultClient(String clientId, String clientSecret, String authority,
        ITokenCacheAccessAspect tokenCache) {

        final Set<String> scopes = Collections.singleton("https://vault.azure.net/.default");

        return new KeyVaultClient(new KeyVaultCredentials() {
            @Override
            public AuthenticationResult doAuthenticate(String authorization, String resource, String scope,
                String schema) {
                try {
                    return acquireToken(clientId, clientSecret, authority, scopes, tokenCache)
                        .thenApply(result -> new AuthenticationResult(result.accessToken(), null))
                        .get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Problem happened while getting secret.", e);
                }
            }
        });
    }

    private static CompletableFuture<IAuthenticationResult> acquireToken(String clientId, String clientSecret,
        String authority, Set<String> scopes, ITokenCacheAccessAspect tokenCache) {

        final IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);
        final ConfidentialClientApplication confidentialClientApplication;
        try {
            confidentialClientApplication = ConfidentialClientApplication.builder(clientId, credential)
                .authority(authority)
                .setTokenCacheAccessAspect(tokenCache)
                .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to parse authority: " + authority, e);
        }

        final SilentParameters silentParameters = SilentParameters.builder(scopes).build();

        try {
            return confidentialClientApplication.acquireTokenSilently(silentParameters)
                .handleAsync((result, error) -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    if (!(error instanceof MsalException) && !(error instanceof CompletionException)) {
                        return CompletableFuture.<IAuthenticationResult>failedFuture(error);
                    }

                    // Occurs when a .get() is made on an asynchronous operation, it's possible that the inner
                    // exception is the MsalException.
                    if (error.getCause() != null && !(error.getCause() instanceof MsalException)) {
                        return CompletableFuture.<IAuthenticationResult>failedFuture(error);
                    }

                    final ClientCredentialParameters parameters = ClientCredentialParameters.builder(scopes).build();
                    return confidentialClientApplication.acquireToken(parameters);
                })
                .thenCompose(result -> result);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL for silent parameters.", e);
        }
    }

    private static String getUserInput(String prompt) {
        System.out.print(prompt + ": ");
        return SCANNER.nextLine();
    }
}
