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

import java.util.List;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

/**
 * Looks up the name of a catalog category.
 */
public final class CategoryNames {

    private CategoryNames() {
    }

    public static String byUrlPath(MagentoGraphqlClient client, String urlPath) {
        return fetchName(client, new CategoryFilterInput().setUrlPath(new FilterEqualTypeInput().setEq(urlPath)));
    }

    public static String byUid(MagentoGraphqlClient client, String uid) {
        return fetchName(client, new CategoryFilterInput().setCategoryUid(new FilterEqualTypeInput().setEq(uid)));
    }

    private static String fetchName(MagentoGraphqlClient client, CategoryFilterInput filter) {
        if (client == null) {
            return null;
        }

        String query = Operations.query(q -> q.categoryList(args -> args.filters(filter), category -> category.name())).toString();
        GraphqlResponse<Query, Error> response = client.execute(query);
        if (response == null || response.getErrors() != null && !response.getErrors().isEmpty() || response.getData() == null) {
            return null;
        }

        List<CategoryTree> categories = response.getData().getCategoryList();
        if (categories == null) {
            return null;
        }
        for (CategoryTree category : categories) {
            if (category != null) {
                return category.getName();
            }
        }
        return null;
    }
}
