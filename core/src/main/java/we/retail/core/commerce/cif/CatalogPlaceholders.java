/*
 *   Copyright 2026 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package we.retail.core.commerce.cif;

import org.apache.commons.lang3.StringUtils;

/**
 * Recognizes the placeholder texts that the CIF components and the catalog page templates show when no catalog data
 * is available, e.g. in the template editor.
 */
public final class CatalogPlaceholders {

    private static final String PRODUCT_NAME = "Product name";
    private static final String CATEGORY_NAME = "Category name";
    private static final String PRODUCT_LIST_ITEM_PREFIX = "Product #";
    private static final String CATEGORY_PAGE_TITLE = "Category Page";
    private static final String PRODUCT_PAGE_TITLE = "Product Page";

    private CatalogPlaceholders() {
    }

    public static boolean isProductName(String text) {
        return StringUtils.equals(text, PRODUCT_NAME);
    }

    public static boolean isCategoryName(String text) {
        return StringUtils.equals(text, CATEGORY_NAME);
    }

    public static boolean isProductListItemTitle(String text) {
        return StringUtils.startsWith(text, PRODUCT_LIST_ITEM_PREFIX);
    }

    public static boolean isPageTitle(String text) {
        return StringUtils.equals(text, CATEGORY_PAGE_TITLE) || StringUtils.equals(text, PRODUCT_PAGE_TITLE);
    }
}
