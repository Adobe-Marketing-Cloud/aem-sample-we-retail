package we.retail.core;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

/**
 * Created by Daniel on 04/12/15.
 */
@Model(adaptables = {Resource.class})
public class Profile {

    @Inject
    @Named("profile/givenName")
    @Optional
    public String givenName;

    @Inject
    @Named("profile/familyName")
    @Optional
    public String familyName;

    @Inject
    @Named("profile/aboutMe")
    @Optional
    public String aboutMe;

    @Inject
    @Named("profile/photos/primary/image")
    @Optional
    public Resource profileImage;

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
