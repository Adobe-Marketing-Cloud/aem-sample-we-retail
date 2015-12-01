"use strict";

var global = this;

/**
 *
 */
use(["/libs/wcm/foundation/components/utils/AuthoringUtils.js",
     "/libs/wcm/foundation/components/utils/ResourceUtils.js",
     "/libs/sightly/js/3rd-party/q.js"], function (AuthoringUtils, ResourceUtils, Q) {

    var Profile = function(profilePath, profileData) {
        this.data = profileData || {};
        this.data.path = profilePath;
    };

    Profile.DEFAULT_AVATAR = "/etc/designs/default/images/social/avatar.png";
    Profile.ANONYMOUS_NAME = "Anonymous";
    Profile.DEFAULT_AVATAR_SIZE = 40;

    Profile.prototype.getData = function() {
        var defs = [],
            self = this;

        this.data.name = this._getName();
        defs.push(this._getAvatarPath(Profile.DEFAULT_AVATAR_SIZE).then(function(avatarPath) {
            self.data.avatar = avatarPath;
        }));

        return Q.all(defs).then(function() {
            return self.data;
        });
    };

    Profile.prototype._getName = function() {
        var ret = [];
        if (this.profileData) {
            if (this.data.hasOwnProperty("givenName")) {
                ret.push(this.data["givenName"]);
            }
            if (this.data.hasOwnProperty("familyName")) {
                ret.push(this.data["familyName"]);
            }
        }

        if (!ret.length) {
            ret.push(Profile.ANONYMOUS_NAME);
        }

        return ret.join(" ");
    };

    Profile.prototype._getAvatarPath = function(size) {
        var def = Q.defer();
        size = size || 40;
        if (this.data.path) {
            ResourceUtils.getResource(this.data.path + "/profile/photos/primary/image")
                .then(
                    function() {
                        def.resolve(this.data.path + "/profile/photos/primary/image.prof.thumbnail." + size + ".jpg");
                    },
                    function() {
                        def.resolve(Profile.DEFAULT_AVATAR);
                    }
                );
        } else {
            def.resolve(Profile.DEFAULT_AVATAR);
        }

        return def.promise;
    };

    var ProfileManager = function() {
    };

    ProfileManager.getProfile = function(profilePath) {
        var def = Q.defer();

        ResourceUtils.getResource(profilePath).then(
            function(profileData) {
                var profile = new Profile(profilePath, profileData);
                profile.getData().then(function(profileData) {
                    def.resolve(profileData);
                });
            },
            function() {
                var profile = new Profile();
                profile.getData().then(function(profileData) {
                    def.resolve(profileData);
                });
            }
        );

        return def.promise;
    };


    return ProfileManager;
});