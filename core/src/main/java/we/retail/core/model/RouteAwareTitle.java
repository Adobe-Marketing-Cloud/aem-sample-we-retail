/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe Systems Incorporated
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
package we.retail.core.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Title.class,
    resourceType = "weretail/components/content/title")
public class RouteAwareTitle implements Title {

    @Self
    private SlingHttpServletRequest request;

    @Self
    @Via(type = ResourceSuperType.class)
    private Title delegate;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private SiteStructure siteStructure;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @Override
    public String getText() {
        String delegateText = delegate != null ? delegate.getText() : StringUtils.EMPTY;
        if (hasMeaningfulAuthoredTitle(delegateText)) {
            return delegateText;
        }

        String routeTitle = resolveCategoryRouteTitle();
        if (StringUtils.isNotBlank(routeTitle)) {
            return routeTitle;
        }

        return delegateText;
    }

    @Override
    public String getType() {
        return delegate != null ? delegate.getType() : null;
    }

    @Override
    public Link getLink() {
        return delegate != null ? delegate.getLink() : null;
    }

    @Override
    public String getLinkURL() {
        return delegate != null ? delegate.getLinkURL() : null;
    }

    @Override
    public boolean isLinkDisabled() {
        return delegate != null && delegate.isLinkDisabled();
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    @Override
    public String getExportedType() {
        return resource != null ? resource.getResourceType() : null;
    }

    private String resolveCategoryRouteTitle() {
        if (siteStructure == null || currentPage == null || !siteStructure.isCategoryPage(currentPage)) {
            return StringUtils.EMPTY;
        }

        String routePath = extractRoutePath();
        if (StringUtils.isBlank(routePath)) {
            return StringUtils.EMPTY;
        }

        CategoryTree category = fetchCategoryByUrlPath(routePath);
        if (category != null && StringUtils.isNotBlank(category.getName())) {
            return category.getName();
        }

        return humanizeRoutePath(routePath);
    }

    private boolean hasMeaningfulAuthoredTitle(String delegateText) {
        if (resource == null) {
            return false;
        }

        String authoredTitle = resource.getValueMap().get("jcr:title", String.class);
        if (StringUtils.isBlank(authoredTitle)) {
            return false;
        }

        return !isPlaceholderTitle(delegateText) && !isPlaceholderTitle(authoredTitle);
    }

    private boolean isPlaceholderTitle(String text) {
        return StringUtils.equals(text, "Category Page") || StringUtils.equals(text, "Product Page");
    }

    private String extractRoutePath() {
        if (request == null) {
            return StringUtils.EMPTY;
        }

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null) {
            return StringUtils.EMPTY;
        }

        String suffix = pathInfo.getSuffix();
        if (StringUtils.isBlank(suffix)) {
            return StringUtils.EMPTY;
        }

        return StringUtils.removeStart(StringUtils.removeEnd(suffix, ".html"), "/");
    }

    private CategoryTree fetchCategoryByUrlPath(String routePath) {
        if (magentoGraphqlClient == null || StringUtils.isBlank(routePath)) {
            return null;
        }

        try {
            GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(buildCategoryQuery(routePath));
            if (response == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
                return null;
            }

            Query data = response.getData();
            List<CategoryTree> categories = data != null ? data.getCategoryList() : null;
            if (categories == null || categories.isEmpty()) {
                return null;
            }

            for (CategoryTree category : categories) {
                if (category != null) {
                    return category;
                }
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String buildCategoryQuery(String routePath) {
        CategoryFilterInput filter = new CategoryFilterInput().setUrlPath(new FilterEqualTypeInput().setEq(routePath));
        QueryQuery.CategoryListArgumentsDefinition searchArgs = args -> args.filters(filter);
        CategoryTreeQueryDefinition queryArgs = category -> category
            .urlPath()
            .name();
        return Operations.query(query -> query.categoryList(searchArgs, queryArgs)).toString();
    }

    private String humanizeRoutePath(String routePath) {
        String segment = StringUtils.substringAfterLast(routePath, "/");
        String normalized = StringUtils.defaultIfBlank(segment, routePath);
        if (StringUtils.equals(normalized, "me")) {
            return "Men";
        }
        if (StringUtils.equals(normalized, "wo")) {
            return "Women";
        }
        if (StringUtils.equals(normalized, "eq")) {
            return "Equipment";
        }

        String[] words = StringUtils.split(StringUtils.replaceChars(normalized, "-_", "  "));
        if (words == null || words.length == 0) {
            return normalized;
        }

        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(StringUtils.capitalize(StringUtils.lowerCase(word)));
        }
        return title.toString();
    }
}
