(function () {
    'use strict';

    Vue.component('we-product-variant', {
        props: [
            'isBase',

            'path',
            'pagePath',
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

            if (typeof data.color !== 'undefined') {
                var colorVariants = self.$parent.colorVariants[data.color];

                self.$parent.colorVariants[data.color] = colorVariants ? colorVariants + 1 : 1;
            }

            if (!!parseInt(self.isBase, 10)) {
                self.$parent.product = data;
            }
        }
    });

    if (document.querySelector('.we-Product')) {
        new Vue({
            name: 'we-Product',
            el: '.we-Product',
            data: {
                variants: [],
                colorVariants: {},
                product: null,

                isChecked: function (productSku) {
                    return productSku === this.product.sku;
                }
            },
            props: [
                'sku',
                'title',
                'pagePath'
            ],
            ready: function() {
                this.trackView();
            },
            methods: {
                _setProduct: function(sku) {
                    var self = this;

                    self.variants.forEach(function (product) {
                        if (product.sku === sku) {
                            self.product = product;
                        }
                    });
                },
                setProduct: function (event) {
                    this._setProduct(event.currentTarget.attributes['data-sku'].value);
                },
                showSizes: function () {
                    return this.colorVariants[this.product.color] > 1 || Object.keys(this.colorVariants).length === 0;
                },
                trackView: function() {
                    if (thsi.product && window.ContextHub && ContextHub.getStore("recentlyviewed")) {
                        ContextHub.getStore("recentlyviewed").record(
                            this.pagePath,
                            this.product.title,
                            this.product.image,
                            this.product.price
                        );
                    }

                    if (this.product && window.CQ_Analytics && CQ_Analytics.ViewedProducts) {
                        CQ_Analytics.ViewedProducts.record(
                            this.pagePath,
                            this.product.title,
                            this.product.image,
                            this.product.price
                        );
                    }
                }
            }
        });
    }

}).call(this);