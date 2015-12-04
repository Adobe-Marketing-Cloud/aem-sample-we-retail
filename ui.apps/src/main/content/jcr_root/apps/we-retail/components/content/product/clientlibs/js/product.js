(function () {
    'use strict';

    Vue.component('product-variant', {
        props: [
            'isBase',

            'path',
            'variants',
            'sku',
            'title',
            'description',
            'color',
            'colorClass',
            'size',
            'price',
            'summary',
            'features',
            'image'
        ],
        compiled: function () {
            var self = this, data = {};

            Object.getOwnPropertyNames(this._props).forEach(function (prop) {
                data[prop] = self[prop];
            });

            self.$parent.variants.push(data);

            if (!!parseInt(self.isBase, 10)) {
                self.$parent.product = data;
            }
        }
    });

    new Vue({
        el: '.we-Product',
        data: {
            variants: [],
            product: null,

            isChecked: function (productSku) {
                return productSku === this.product.sku;
            }
        },
        methods: {
            setProduct: function (event) {
                var self = this;

                self.variants.forEach(function (product) {
                    if (product.sku === event.currentTarget.attributes['data-sku'].value) {
                        self.product = product;
                    }
                });
            }
        }
    });

}).call(this);