package we.retail.core;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.foundation.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 03/12/15.
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class Article {

    @Inject
    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_TITLE)
    public String title;

    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_DESCRIPTION)
    @Optional
    public String description;

    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/cq:tags")
    @Default(values = {})
    public String[] cqTags;

    @ResourcePath(name=JcrConstants.JCR_CONTENT + "/articleAuthor", optional = true)
    public Profile author;

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<Tag>();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        for (String cqTag : cqTags) {
            tags.add(tagManager.resolve(cqTag));
        }

        return tags;
    }
}
