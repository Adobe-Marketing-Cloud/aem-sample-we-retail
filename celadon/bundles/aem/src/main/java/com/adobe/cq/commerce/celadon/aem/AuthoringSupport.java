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
package com.adobe.cq.commerce.celadon.aem;

import java.net.URI;
import java.net.URL;

/**
 * Pure string helpers shared by the authoring service and MCP tools. No AEM
 * dependencies, so these are exercised by plain unit tests.
 */
public final class AuthoringSupport {

    private AuthoringSupport() {}

    /**
     * Parse a source URL into a URI, percent-encoding the path/query so that raw
     * spaces and other unsafe characters (common in DAM asset names like
     * {@code "Jola Blue.jpg"}) do not break {@code URI.create}.
     */
    public static URI encodedUri(String rawUrl) {
        try {
            URL u = new URL(rawUrl);
            return new URI(u.getProtocol(), u.getAuthority(), u.getPath(), u.getQuery(), u.getRef());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid source url: " + rawUrl, e);
        }
    }

    /** A local AEM asset reference (under {@code /content/...}) versus an external URL to import. */
    public static boolean isLocalDamPath(String value) {
        return value != null && value.startsWith("/content/");
    }

    /** File extension for a downloaded asset: MIME type first, then the URL suffix, else {@code bin}. */
    public static String extensionForMimeType(String mimeType, String sourceUrl) {
        if (mimeType == null || mimeType.isBlank()) {
            int dot = sourceUrl == null ? -1 : sourceUrl.lastIndexOf('.');
            return dot >= 0 ? sourceUrl.substring(dot + 1) : "bin";
        }
        String mime = mimeType.toLowerCase();
        if (mime.contains("jpeg") || mime.contains("jpg")) {
            return "jpeg";
        }
        if (mime.contains("png")) {
            return "png";
        }
        if (mime.contains("gif")) {
            return "gif";
        }
        if (mime.contains("webp")) {
            return "webp";
        }
        return "bin";
    }

    /** Asset path co-located with the product: {@code <productFolder>/<sku>_img.<ext>}. */
    public static String assetPath(String productFolderPath, String sku, String extension) {
        String safe = sku == null ? "" : sku.replaceAll("[^a-zA-Z0-9._-]", "-");
        return productFolderPath + "/" + safe + "_img." + extension;
    }

    /** Provided sku if non-blank, otherwise a stable {@code <masterSku>-<variantId>}. */
    public static String variantSku(String masterSku, String providedSku, String variantId) {
        if (providedSku != null && !providedSku.isBlank()) {
            return providedSku;
        }
        return masterSku + "-" + variantId;
    }
}
