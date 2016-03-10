(function () {
    'use strict';

    window.we = window.we || {};

    Vue.component('we-product-filter', {
        props: ['type']
    });

    class FiltersStore {
      constructor (data) {
        this.data = data || {
            color: [],
            price: [],
            size: []
        }
      }
    };

    we.filterStore = we.filterStore || new FiltersStore();


    // Vue.config.debug = true;

    _.each(document.querySelectorAll('.product-filter'), function (el, index) {
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
                }
            },
            events: {
                'grid-ready': function(filters) {
                    console.log('grid ready', filters.color, this.filters);
                    // this.filters.push(this.filters, filters.color);
                    // debugger;

                }
            }
        });
    });

}).call(this);