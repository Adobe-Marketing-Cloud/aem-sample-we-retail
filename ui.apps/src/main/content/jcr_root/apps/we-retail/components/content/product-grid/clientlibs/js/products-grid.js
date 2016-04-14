/*
 *  Copyright 2016 Adobe Systems Incorporated
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
(function () {
    'use strict';
    function isElementInViewport () {
        var el = this,
            rect = el.getBoundingClientRect();

        return (
            rect.left >= 0 &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    var component = Vue.extend({
        data: function() {
          return{
            page: 0,
            pages: []
          }
        },
        beforeCompile: function() {
            // observe page property
            this.$el.setAttribute(':data-page', 'page');

            // add mobile pagination template
            this.$el.innerHTML += [
                '<div class="product-grid-mobilePagination" v-if="pages.length > 1">',
                    '<a v-for="index in pages" v-on:click="page = index" :class="{active: (page == index)}">{{ index }}</a>',
                '</div>'].join('');
        },
        ready: function() {
            var $el = $(this.$el),
                $items = $el.find('.foundation-list-item'),
                $visibleItems = $items.filter(isElementInViewport),
                pagesCount = Math.ceil($items.length / ($visibleItems.length || $items.length));

            // initialize mobile pagination with calculated pages
            this.pages = _.range(pagesCount);
        }
    });

    // attach component to all occurances of .products-grid element
    $('.product-grid').each(function() {
        new component().$mount(this);
    });

}).call(this);