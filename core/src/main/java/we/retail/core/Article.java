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

    private static String PROP_GIVEN_NAME = "givenName";
    private static String PROP_FAMILY_NAME = "familyName";
    private static String DEFAULT_NAME = "Anonymous";

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
    @Optional
    private Resource articleAuthor;

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<Tag>();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        for (String cqTag : cqTags) {
            tags.add(tagManager.resolve(cqTag));
        }

        return tags;
    }

    public String getAuthorName() {
        List<String> ret = new ArrayList<String>();
        if (articleAuthor != null) {
            ValueMap vm = articleAuthor.getValueMap();
            if (vm.containsKey(PROP_GIVEN_NAME)) {
                ret.add(vm.get(PROP_GIVEN_NAME, String.class));
            }
            if (vm.containsKey(PROP_FAMILY_NAME)) {
                ret.add(vm.get(PROP_FAMILY_NAME, String.class));
            }
        }

        if (ret.isEmpty()) {
            ret.add(DEFAULT_NAME);
        }

        return StringUtils.join(ret, " ");
    }
}
