package we.retail.core;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.day.cq.dam.api.DamConstants;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/**
 * Created by Daniel on 03/12/15.
 */
@Model(adaptables = {Resource.class})
public class Article {
    
    static Logger LOGGER = LoggerFactory.getLogger(Article.class); 

    private static final String CONTENT_FRAGMENT_REF_PATH = "root/responsivegrid/content_fragment/fileReference";

    private static final String METADATA_PATH = "jcr:content/metadata";
    private static final String AUTHOR_REF_PATH = METADATA_PATH + "/" + JcrConstants.JCR_LAST_MODIFIED_BY;
    private static final String LAST_MODIFIED_PATH = METADATA_PATH + "/" + DamConstants.DC_MODIFIED;

    private static final String MAIN_ELEMENT = "main";
    private static final String TEASER_VARIATION = "teaser";

    @Inject
    @SlingObject
    private ResourceResolver resourceResolver;
    
    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_TITLE)
    @Default(values = "")
    public String title;

    @ResourcePath(name=JcrConstants.JCR_CONTENT + "/" + CONTENT_FRAGMENT_REF_PATH, optional = true)
    protected ContentFragment contentFragment;

    public Resource resource;
    
    public Article(Resource resource) {
        this.resource = resource;
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



    public String getImagePath() {
        return resource.getPath() + ".article-image.jpeg";
    }

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<Tag>();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        
        if(contentFragment != null) {
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
            if(contentFragment != null) {
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
        }
        catch (RepositoryException ex) {
            LOGGER.error("Error getting article author", ex);
        }

        return author;
    }
    
    public String getModified() {
        String result = null;

        if(contentFragment != null) {
            Resource contentFragmentResource = contentFragment.adaptTo(Resource.class);
            ValueMap contentFragmentProperties = contentFragmentResource.getValueMap();

            Date modified = contentFragmentProperties.get(LAST_MODIFIED_PATH, Date.class);
            result = new SimpleDateFormat("MMM dd, YYYY", Locale.US).format(modified);
        }
        
        return result;
    }
}
