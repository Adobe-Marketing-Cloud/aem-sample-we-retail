package we.retail.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 04/12/15.
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class Profile {

    private static final String DEFAULT_NAME = "Anonymous";

    @Inject
    @Optional
    public String givenName;

    @Inject
    @Optional
    public String familyName;


    public String getName() {
        List<String> ret = new ArrayList<String>();
        if (givenName != null) {
            ret.add(givenName);
        }

        if (familyName != null) {
            ret.add(familyName);
        }

        if (ret.isEmpty()) {
            ret.add(DEFAULT_NAME);
        }

        return StringUtils.join(ret, " ");
    }
}
