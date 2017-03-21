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
(function ($) {
    'use strict';

    window.we = window.we || {};

    Vue.component('we-product-filter', {
        props: ['type']
    });

    var FiltersStore = function(data) {
        this.data = data || {
            color: [],
            price: [],
            size: []
        }
    };

    we.filterStore = we.filterStore || new FiltersStore();


    // Vue.config.debug = true;

    $(".we-product-filter").each(function (index, el) {
        new Vue({
            parent: we.app,
            name: 'product-filter',
            el: el,
            data: {
                filters: we.filterStore.data
            },
            ready: function () {
                var vm = this;

                vm.$parent.activeFilters = {};

                if (index === 0) {
                    we.app.$on('show-product-item', function (filters) {
                        we.app.$broadcast('show-product-item', filters);
                    });
                }
            },
            methods: {
                onFilterClick: function (prop, val, event) {
                    if (!this.$parent.activeFilters[prop]) {
                        this.$parent.activeFilters[prop] = [];
                    }

                    if (!event.target.checked && this.$parent.activeFilters[prop].includes(val)) {
                        this.$parent.activeFilters[prop].splice(this.$parent.activeFilters[prop].indexOf(val), 1);
                    }
                    else if (event.target.checked && !this.$parent.activeFilters[prop].includes(val)) {
                        this.$parent.activeFilters[prop].push(val);
                    }

                    this.$dispatch('show-product-item', this.$parent.activeFilters);
                    
                    $("input[value='" + val + "']").siblings('div').toggleClass('tick');
                },
                toggle: function() {
                    we.app.$broadcast('toggle-filters');
                }
            },
            events: {
                'toggle-filters': function(filters) {
                    this.$el.classList.toggle('product-filter-visible');
                }
            }
        });
    });

})(jQuery);