// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.conniey;

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.ITokenCacheAccessContext;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TokenCacheAspect implements ITokenCacheAccessAspect {

    private String data;

    public TokenCacheAspect(String fileName) {
        this.data = readDataFromFile(fileName);
    }

    @Override
    public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        iTokenCacheAccessContext.tokenCache().deserialize(data);
    }

    @Override
    public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        data = iTokenCacheAccessContext.tokenCache().serialize();
        // you could implement logic here to write changes to file
    }

    private static String readDataFromFile(String resource) {
        try {
            final URL path = TokenCacheAspect.class.getClassLoader().getResource(resource);
            if (path == null) {
                System.out.println("Could not locate resource: " + resource);
                return "";
            }

            return new String(
                Files.readAllBytes(
                    Paths.get(path.toURI())));
        } catch (Exception ex){
            System.out.println("Error reading data from file: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
