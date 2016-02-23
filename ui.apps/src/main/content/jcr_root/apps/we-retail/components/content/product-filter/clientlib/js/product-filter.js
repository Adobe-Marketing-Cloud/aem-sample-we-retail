(function () {
    'use strict';

    window.we = window.we || {};

    Vue.component('we-product-filter', {
        props: ['type']
    });

    document.querySelectorAll('.product-filter').forEach(function (el, index) {
        new Vue({
            parent: we.app,
            name: 'product-filter',
            el: el,
            ready: function () {
                var vm = this;

                vm.filters = vm.$parent.filters;
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
                }
            }
        });
    });

}).call(this);