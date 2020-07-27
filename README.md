# Azure Table service demo

Contains a demo for creating a table, and adding an entity. Service clients are authorised through credentials stored in
Key Vault.

In production, Key Vault is the preferred approach for managing and accessing credentials. Hopefully, these programs
demonstrate the advantages of using the unified client libraries versus existing libraries.

## Running the sample

1. Create an Azure subscription.
1. Create a key vault account.
1. Create a storage account.
1. [Create a service
   principal](https://docs.microsoft.com/azure/active-directory/develop/howto-create-service-principal-portal).
    1. Remember the tenant id, client id, and client secret for this service principal.
1. [Set-up key vault to manage storage
   account](https://docs.microsoft.com/azure/key-vault/secrets/overview-storage-keys#manage-storage-account-keys).
    1. When following step "[Create a shared access signature
       token](https://docs.microsoft.com/azure/key-vault/secrets/overview-storage-keys#create-a-shared-access-signature-token)",
       replace `--permissions rw` with `--permissions rwlaud`. This grants the sas token permissions to "read", "write",
       "list", "add", "update", and "delete" objects in table service.
1. In Key Vault, add a secret with name "tablesurl" pointing to the primary endpoint for the table service.
   1. `az keyvault secret set --vault-name "<name-of-keyvault>" --name "tablesurl" --description "URL for tables
      endpoint" --value "https://<storage-name>.table.core.windows.net/"`
1. Change values for `KEY_VAULT_URL` and `TABLES_SAS_TOKEN_KEY` to match the values of the key vault and the managed storage secret name.
1. Set the following environment variables with values from the service principal.
   1. `AZURE_CLIENT_ID`
   1. `AZURE_CLIENT_SECRET`
   1. `AZURE_TENANT_ID`
1. Run program.
