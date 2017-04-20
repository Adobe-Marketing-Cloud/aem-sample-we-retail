/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.model;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.day.cq.commons.ImageResource;
import com.day.cq.dam.api.DamConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;

@Model(adaptables = {SlingHttpServletRequest.class})
public class Article {

    static Logger LOGGER = LoggerFactory.getLogger(Article.class);

    private static final String CONTENT_FRAGMENT_REF_PATH = "jcr:content/root/responsivegrid/content_fragment";
    private static final String METADATA_PATH = "jcr:content/metadata";
    private static final String AUTHOR_REF_PATH = METADATA_PATH + "/" + JcrConstants.JCR_LAST_MODIFIED_BY;
    private static final String LAST_MODIFIED_PATH = METADATA_PATH + "/" + DamConstants.DC_MODIFIED;
    private static final String PN_FILE_REFERENCE = "fileReference";

    private static final String MAIN_ELEMENT = "main";
    private static final String TEASER_VARIATION = "teaser";

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;

    private Resource imageResource;
    private String title;
    private ContentFragment contentFragment;

    @PostConstruct
    protected void initModel() {
        Page page = resource.adaptTo(Page.class);
        if (page != null) {
            title = page.getTitle();
        }
        imageResource = resource.getChild(JcrConstants.JCR_CONTENT + "/root/hero_image");
        if (imageResource != null) {
            // wrap the hero image resource and inject the value for the alt image attribute
            final Map<String, Object> resourceProperties = new HashMap<String, Object>(imageResource.getValueMap());
            if (title != null) {
                resourceProperties.put(ImageResource.PN_ALT, title);
            }
            final ValueMapDecorator decorator = new ValueMapDecorator(resourceProperties);
            imageResource = new ResourceWrapper(imageResource) {

                @Override
                public ValueMap getValueMap() {
                    return decorator;
                }

                @Override
                public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                    if (type == ValueMap.class) {
                        return (AdapterType) decorator;
                    }
                    return super.adaptTo(type);
                }
            };
        }
        contentFragment = getContentFragment();
    }

    public String getTeaser() {
        String teaser = null;
        if (contentFragment != null) {
            ContentElement element = contentFragment.getElement(MAIN_ELEMENT);
            if (element != null) {
                ContentVariation variation = element.getVariation(TEASER_VARIATION);

                if (variation != null) {
                    teaser = variation.getContent();
                }
            }
        }
        return teaser;
    }

    public String getTitle() {
        return title;
    }

    public Resource getImageResource() {
        return imageResource;
    }

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<Tag>();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);

        if (contentFragment != null) {
            Object[] tagIds = (Object[]) contentFragment.getMetaData().get("cq:tags");
            if (tagIds != null) {
                for (Object cqTag : tagIds) {
                    tags.add(tagManager.resolve(cqTag.toString()));
                }
            }
        }
        return tags;
    }

    public Profile getAuthor() {
        Profile author = null;

        try {
            if (contentFragment != null) {
                // TODO: find the right property to get the author id from
                Resource contentFragmentResource = contentFragment.adaptTo(Resource.class);
                ValueMap contentFragmentProperties = contentFragmentResource.getValueMap();

                String authorId = contentFragmentProperties.get(AUTHOR_REF_PATH, String.class);

                if (authorId != null) {
                    UserManager userManager = resourceResolver.adaptTo(UserManager.class);
                    String authorPath = userManager.getAuthorizable(authorId).getPath();

                    author = resourceResolver.getResource(authorPath).adaptTo(Profile.class);
                }
            }
        } catch (RepositoryException ex) {
            LOGGER.error("Error getting article author", ex);
        }

        return author;
    }

    public String getModified() {
        String result = null;

        if (contentFragment != null) {
            Resource contentFragmentResource = contentFragment.adaptTo(Resource.class);
            ValueMap contentFragmentProperties = contentFragmentResource.getValueMap();

            Date modified = contentFragmentProperties.get(LAST_MODIFIED_PATH, Date.class);
            result = new SimpleDateFormat("MMM dd, YYYY", Locale.US).format(modified);
        }

        return result;
    }

    private ContentFragment getContentFragment() {
        ContentFragment contentFragment = null;
        Resource contentFragmentResource = resource.getChild(CONTENT_FRAGMENT_REF_PATH);
        if (contentFragmentResource != null) {
            ValueMap valueMap = contentFragmentResource.getValueMap();
            String fileReference = valueMap.get(PN_FILE_REFERENCE, String.class);
            if (StringUtils.isNotEmpty(fileReference)) {
                Resource resource = resourceResolver.getResource(fileReference);
                if (resource != null) {
                    contentFragment = resource.adaptTo(ContentFragment.class);
                }
            }
        }
        return contentFragment;
    }
}
