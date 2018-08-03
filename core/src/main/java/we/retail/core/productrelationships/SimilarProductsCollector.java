/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.productrelationships;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.day.cq.tagging.TagConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.ProductRelationship;
import com.adobe.cq.commerce.common.AbstractJcrProduct;
import com.adobe.cq.commerce.common.DefaultProductRelationship;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.ValueMap;

/**
 * A sample RelatedProductsCollector which matches based on tags.
 */
public class SimilarProductsCollector {
    /**
     * The type of relationships to generate.
     */
    protected String relationshipType;
    protected String relationshipTitle;

    /**
     * The source context of the relationships.
     */
    protected ResourceResolver resolver;
    protected PageManager pageManager;
    protected CommerceSession commerceSession;
    protected List<List<String>> matchTagSets;
    protected List<String> exclusionSKUs;

    /**
     * A map from SKU to relationship.  This allows us to recommend a particular product
     * only once even if multiple presentations are found for it.
     */
    protected Map<String, ProductRelationship> relationships;


    SimilarProductsCollector(ResourceResolver resolver, CommerceSession session, String relationshipType, String relationshipTitle,
                             List<Product> contextProducts) {
        this.resolver = resolver;
        this.pageManager = resolver.adaptTo(PageManager.class);
        this.commerceSession = session;

        this.relationshipType = relationshipType;
        this.relationshipTitle = relationshipTitle;

        matchTagSets = new ArrayList<List<String>>();
        exclusionSKUs = new ArrayList<String>();
        for (Product product : contextProducts) {
            List<String> matchTags = new ArrayList<String>();
            Collections.addAll(matchTags, product.getProperty(TagConstants.PN_TAGS, String[].class));
            matchTagSets.add(matchTags);

            try {
                exclusionSKUs.add(product.getBaseProduct().getSKU());
            } catch (CommerceException e) {
                exclusionSKUs.add(product.getSKU());    // not perfect, but better than nothing
            }
        }

        this.relationships = new HashMap<String, ProductRelationship>();
    }

    /**
     * Add a product relationship. Include 'price', 'matched-tags' and 'rank' as extra metadata.
     * 
     * @param product
     *            The product to add
     * @param matchedTags
     *            The list of matched tags
     * @throws CommerceException
     */
    protected void addProduct(Product product, List<String> matchedTags) throws CommerceException {
        int rank = matchedTags.size();
        ProductRelationship existing = relationships.get(product.getSKU());
        if (existing != null && existing.getMetadata().get("rank", 0) > rank) {
            return;
        }

        ProductRelationship relationship = new DefaultProductRelationship(relationshipType, relationshipTitle, product);
        if (commerceSession != null) {
            relationship.getMetadata().put("price", commerceSession.getProductPrice(product));
        }
        String serializedTags = "";
        for (String tag : matchedTags) {
            if (serializedTags.length() > 0) {
                serializedTags += ",";
            }
            serializedTags += tag;
        }
        relationship.getMetadata().put("matched-tags", serializedTags);
        relationship.getMetadata().put("rank", rank);
        relationships.put(product.getSKU(), relationship);
    }

    public void walk(Resource resource) throws CommerceException {
        //
        // NB: this is a demonstration implementation which would not scale well in production cases.
        //

        if (AbstractJcrProduct.isABaseProduct(resource)) {
            Product product = resource.adaptTo(Product.class);
            if (product == null || exclusionSKUs.contains(product.getSKU())) {
                return;
            }
            if (pageManager.getContainingPage(resource).getPath().contains("activities")) {
                return;
            }
            String[] productTags = product.getProperty(TagConstants.PN_TAGS, String[].class);
            if (productTags == null || productTags.length == 0) {
                return;
            }
            for (List<String> matchTags : matchTagSets) {
                List<String> productTagList = new ArrayList<String>();
                Collections.addAll(productTagList, productTags);

                productTagList.retainAll(matchTags);

                if (productTagList.size() > 0) {
                    addProduct(product, productTagList);
                    break;
                }
            }
        } else {
            for (Iterator<Resource> iterator = resource.listChildren(); iterator.hasNext();) {
                walk(iterator.next());
            }
        }
    }

    /**
     * Return the collected relationships, sorted by 'rank'.
     * 
     * @return the list of relationships, sorted by 'rank'
     */
    public List<ProductRelationship> getRelationships() {
        List<ProductRelationship> relationshipList = new ArrayList<ProductRelationship>(relationships.values());
        Collections.sort(relationshipList, new Comparator<ProductRelationship>() {
            public int compare(ProductRelationship a, ProductRelationship b) {
                ValueMap aMetadata = a.getMetadata();
                ValueMap bMetadata = b.getMetadata();
                if (aMetadata == null && bMetadata == null) {
                    return 0;
                } else {
                    if (aMetadata == null) {
                        return 1;
                    } else if (bMetadata == null) {
                        return -1;
                    }
                    Integer aRank = aMetadata.get("rank", Integer.class);
                    Integer bRank = bMetadata.get("rank", Integer.class);
                    if (aRank == null && bRank == null) {
                        return 0;
                    } else {
                        if (aRank == null) {
                            return 1;
                        } else if (bRank == null) {
                            return -1;
                        }
                        return aRank.compareTo(bRank);
                    }
                }
            }
        });
        return relationshipList;
    }
}


