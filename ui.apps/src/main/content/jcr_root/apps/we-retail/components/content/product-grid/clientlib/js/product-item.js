(function () {
    'use strict';

    window.we = window.we || {};

    var parentEl;
    Vue.config.debug = true

    Vue.component('we-product-item', {
        props: [
            'price',
            'color',
            'size',
            'isVisible'
        ],
        compiled: function () {
            console.log('...');
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
                    //parentEl.appendChild(vm.$el.parentNode);
                }
                else {
                    vm.$el.parentNode.classList.add('hidden');
                    //vm.$el.parentNode.remove();
                }
            }
        }
    });

    _.each(document.querySelectorAll('.product-grid'), function (el, index) {
        new Vue({
            parent: we.app,
            name: 'product-grid',
            el: el,
            data: {
                filters: {}
            },
            ready: function () {
                if (this.filters.price) {
                    this.filters.price = getPriceList(this.filters.price);
                }

                this.$parent.filters = this.filters;

                parentEl = this.$el.querySelector('.foundation-ordered-list-container');

                console.log('.product-grid ready', this);
            }
        });
    });

    function getPriceList(prices) {
        var list = []
            , maxPrice
            , step = 50;

        maxPrice = Math.max.apply(null, prices);

        for (var i = 0; i < maxPrice / step; i++) {
            list[i] = {
                label: '$' + (step * i) + ' - $' + (step * (i+1) - 1),
                list: []
            };
        }

        prices.forEach(function (price) {
            list[(parseInt(price / step, 10))].list.push(price);
        });

        return list;
    }

    function parsePrice(price) {
        var r = /(\d+(?:.\d{1,2}))/
            , e = r.exec(price);

        return e ? parseFloat(e[0]) : price;
    }

    function sortSizes(sizes) {
        var temp1 = ['XS', 'S', 'M', 'L', 'XL', '2XL', 'XXL', '3XL', 'XXXL']
            , temp2 = ['cm', 'in'];

        sizes.sort(function (a, b) {
            //if ()

            return 0;
        });

        return sizes;
    }

}).call(this);