/*
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2013 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
(function(SCF) {
    "use strict";

    var ReviewSystemView = SCF.ReviewSystemView.extend({
        rate: function(e) {
            SCF.ReviewSystemView.prototype.rate.apply(this, arguments);
            var $star = $(e.target),
                val = $star.data('rating-value');

            $star.parent().attr('data-rating-shown', val);
            return false;
        }
    });

    SCF.registerComponent('we-retail/components/hbs/reviews/review', SCF.Review, SCF.ReviewView);
    SCF.registerComponent('we-retail/components/hbs/reviews', SCF.ReviewSystem, ReviewSystemView);

})(SCF);