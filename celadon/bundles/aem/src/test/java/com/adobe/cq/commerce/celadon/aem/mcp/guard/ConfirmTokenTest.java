/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.celadon.aem.mcp.guard;

import static org.junit.Assert.*;
import org.junit.Test;

public class ConfirmTokenTest {

    @Test
    public void validTokenVerifiesForSameOperationAndArgs() {
        ConfirmToken tokens = new ConfirmToken(60_000);
        String token = tokens.issue("demo", "delete_product", "{\"sku\":\"S1\"}");
        assertTrue(tokens.verify(token, "demo", "delete_product", "{\"sku\":\"S1\"}"));
    }

    @Test
    public void tokenRejectedForDifferentArgs() {
        ConfirmToken tokens = new ConfirmToken(60_000);
        String token = tokens.issue("demo", "delete_product", "{\"sku\":\"S1\"}");
        assertFalse(tokens.verify(token, "demo", "delete_product", "{\"sku\":\"S2\"}"));
    }

    @Test
    public void tokenRejectedForDifferentOperationOrCatalog() {
        ConfirmToken tokens = new ConfirmToken(60_000);
        String token = tokens.issue("demo", "delete_product", "{\"sku\":\"S1\"}");
        assertFalse(tokens.verify(token, "other", "delete_product", "{\"sku\":\"S1\"}"));
        assertFalse(tokens.verify(token, "demo", "set_catalog_ready", "{\"sku\":\"S1\"}"));
    }

    @Test
    public void expiredTokenRejected() {
        ConfirmToken tokens = new ConfirmToken(-1); // already expired for any elapsed >= 0
        String token = tokens.issue("demo", "delete_product", "{\"sku\":\"S1\"}");
        assertFalse(tokens.verify(token, "demo", "delete_product", "{\"sku\":\"S1\"}"));
    }

    @Test
    public void tamperedTokenRejected() {
        ConfirmToken tokens = new ConfirmToken(60_000);
        String token = tokens.issue("demo", "delete_product", "{\"sku\":\"S1\"}");
        assertFalse(tokens.verify(token + "x", "demo", "delete_product", "{\"sku\":\"S1\"}"));
    }
}
