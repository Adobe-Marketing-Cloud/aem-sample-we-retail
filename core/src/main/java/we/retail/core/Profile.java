package we.retail.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 04/12/15.
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class Profile {

    @Inject
    @Named("profile/givenName")
    @Optional
    public String givenName;

    @Inject
    @Named("profile/familyName")
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

        return StringUtils.join(ret, " ");
    }
}
