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

    var parentEl;
    // Vue.config.debug = true

    Vue.component('we-product-item', {
        props: [
            'price',
            'color',
            'size',
            'isVisible'
        ],
        compiled: function () {
            var vm = this;

            Object.keys(vm._props).forEach(function (filter) {
                if (!vm.$parent.filters[filter]) {
                    vm.$parent.filters[filter] = [];
                }

                if (vm[filter]) {
                    switch (filter) {
                        case 'price':
                            var pricesList = vm[filter].split(',');

                            pricesList.forEach(function (price, index) {
                                pricesList[index] = parsePrice(price);
                            });

                            vm[filter] = pricesList;

                            vm.$parent.filters[filter] = _.union(vm.$parent.filters[filter], pricesList);

                            break;
                        default:
                            vm.$parent.filters[filter] = _.union(vm.$parent.filters[filter], vm[filter].split(','));

                            break;
                    }
                }
            });

            vm.$parent.filters['size'] = _.sortBy(vm.$parent.filters['size'], function (size) {
                var tmp = parseFloat(size);
                return _.isNumber(tmp) && !_.isNaN(tmp);
            });

            vm.isVisible = true;
        },
        events: {
            'show-product-item': function (filters) {
                var vm = this
                    , visibleArray = []
                    , isVisible;

                Object.keys(filters).forEach(function (filter) {
                    var _visible = null;

                    if (filters[filter].length) {
                        _visible = _visible === null ? false : _visible;
                        _visible = _visible === true ? false : _visible;

                        switch (filter) {
                            case 'price':
                                filters[filter].forEach(function (list) {
                                    list.forEach(function (val) {
                                        _visible = vm[filter] && vm[filter].includes(val) ? true : _visible;
                                    });
                                });
                                break;
                            default:
                                filters[filter].forEach(function (val) {
                                    _visible = vm[filter] && vm[filter].split(',').includes(val) ? true : _visible;
                                });
                                break;
                        }

                        visibleArray.push(_visible === null ? true : _visible);
                    }
                });

                isVisible = visibleArray.length ?
                    _.reduce(visibleArray, function (memo, visible) {
                        return memo && visible;
                    }) : true;

                vm.isVisible = isVisible;

                if (vm.isVisible) {
                    vm.$el.parentNode.classList.remove('hidden');
                    vm.$el.querySelector('div.cmp-image img').dispatchEvent(new Event('cmp-image-redraw'));
                } else {
                    vm.$el.parentNode.classList.add('hidden');
                }
            }
        }
    });

    $('.we-product-grid-container').each(function (index, el) {
        new Vue({
            parent: we.app,
            name: 'productgrid',
            el: el,
            data: {
                filters: we.filterStore.data
            },
            ready: function () {
                if (this.filters.price) {
                    this.filters.price = getPriceList(this.filters.price);
                }
                if(this.filters.size) {
                    this.filters.size = sortSizes(this.filters.size);
                }

                parentEl = this.$el.querySelector('.foundation-ordered-list-container');
            }
        });
    });

    function getPriceList(prices) {
        var list = [],
            maxPrice,
            step = 50;

        maxPrice = Math.max.apply(null, prices);

        for (var i = 0; i < maxPrice / step; i++) {
            list[i] = {
                label: '$' + (step * i) + ' - $' + (step * (i+1) - 1),
                list: []
            };
        }

        prices.forEach(function (price) {
            var pos = Math.floor(price / step);
            if (list[pos]) {
                list[pos].list.push(price);
            }
        });

        return list;
    }

    function parsePrice(price) {
        var r = /(\d+(?:.\d{1,2}))/
            , e = r.exec(price);

        return e ? parseFloat(e[0]) : price;
    }

    function sortSizes(sizes) {
        var dualSizesRef = ['XS', 'S', 'M', 'L', 'XL', '2XL', 'XXL', '3XL', 'XXXL', 'XXK'],
            dualSizes = [],
            numbers = [],
            others = [];

        _.each(sizes, function(item) {
           if(!isNaN(item)) {
               numbers.push(item)
           } else if(_.contains(dualSizesRef, item)) {
               dualSizes.push(item);
           }
           else {
               others.push(item);
           }
        });

        dualSizes.sort(function(a, b) {
            return dualSizesRef.indexOf(a) - dualSizesRef.indexOf(b);
        });

        numbers.sort(function(a, b) {
            return parseInt(a) - parseInt(b);
        });

        return _.union(others, dualSizes, numbers);
    }

})(jQuery);
