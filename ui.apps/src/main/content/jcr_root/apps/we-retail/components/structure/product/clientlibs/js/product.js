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
                    if (this.product && window.ContextHub && ContextHub.getStore("recentlyviewed")) {
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