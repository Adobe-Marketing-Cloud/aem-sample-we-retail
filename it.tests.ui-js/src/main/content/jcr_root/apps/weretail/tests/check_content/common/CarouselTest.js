/*
 *  Copyright 2018 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
;(function(h) {

    var selectors = {
        carousel: {
            self: ".cmp-carousel",
            item: {
                self: ".cmp-carousel__item",
                active: ".cmp-carousel__item--active"
            },
            indicator: {
                self: ".cmp-carousel__indicator",
                first: ".cmp-carousel__indicator:first-of-type",
                second: ".cmp-carousel__indicator:eq(1)",
                last: ".cmp-carousel__indicator:last-of-type"
            },
            action:{
                next: ".cmp-carousel__action--next",
                prev: ".cmp-carousel__action--previous"
            }
        },
        teaser: {
            self: ".cmp-teaser",
            image: ".cmp-teaser__image",
            title: ".cmp-teaser__title",
            action: ".cmp-teaser__action-link"
        }
    };

    window.CQ.WeRetailIT.CarouselSlidesTest = function (h, $, count) {
        return new h.TestCase("Check carousel slides are visible")
            // check if carousel is visible
            .asserts.visible(selectors.carousel.self, true)
            // check number of carousel items
            .asserts.isTrue(function () {
                return window.CQ.WeRetailIT.checkNumberOfItems(h, selectors.carousel.item.self, count)
            })
            .asserts.isTrue(function () {
                return window.CQ.WeRetailIT.checkNumberOfItems(h, selectors.carousel.item.self + " " + selectors.teaser.image, count)
            })
            .asserts.isTrue(function () {
                return window.CQ.WeRetailIT.checkNumberOfItems(h, selectors.carousel.item.self + " " + selectors.teaser.action, count)
            })
            .asserts.isTrue(function () {
                return window.CQ.WeRetailIT.checkNumberOfItems(h, selectors.carousel.indicator.self, count)
            })
    };

    window.CQ.WeRetailIT.CarouselSlidesNavigationTest = function (h, $) {
        return new h.TestCase("Check carousel slide navigation")

            // clicking next button
            .click(selectors.carousel.action.next)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/biking/rocks-downhill.jpg']").length == 1})
            .click(selectors.carousel.action.next)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/climbing/climber-gear-rope.jpg']").length == 1})

            // clicking prev button
            .click(selectors.carousel.action.prev)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/biking/rocks-downhill.jpg']").length == 1})
            .click(selectors.carousel.action.prev)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/running/running-woods-woman.jpg']").length == 1})

            // navigate using indicators
            .click(selectors.carousel.indicator.first)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/running/running-woods-woman.jpg']").length == 1})
            .click(selectors.carousel.indicator.second)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/biking/rocks-downhill.jpg']").length == 1})
            .click(selectors.carousel.indicator.last)
            .assert.isTrue(function(){
                return h.find(selectors.carousel.item.active + " [data-asset='/content/dam/we-retail/en/activities/climbing/climber-gear-rope.jpg']").length == 1})
    }
})(hobs);
