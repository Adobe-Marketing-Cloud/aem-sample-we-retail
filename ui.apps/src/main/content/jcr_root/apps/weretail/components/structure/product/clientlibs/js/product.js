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

    const entityMap = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
        '`': '&#x60;',
        '=': '&#x3D;'
    };

    function escapeHtml(string) {
        if (string === undefined) {
            return string;
        }
        return String(string).replace(/[&<>"'`=]/g, function (s) {
            return entityMap[s];
        });
    }

    Vue.component('we-product-variant', {
        props: [
            'isBase',
            'path',
            'pagePath',
            'variants',
            'sku',
            'title',
            'description',
            'price',
            'summary',
            'features',
            'image',
            'thumbnail',
            'variantAxes'
        ],
        compiled: function () {
            var self = this, data = {};

            Object.getOwnPropertyNames(this._props).forEach(function (prop) {
                if (prop == 'variantAxes') {
                    data[prop] = JSON.parse(self[prop]);
                }
                else {
                    data[prop] = escapeHtml(self[prop]);
                }
            });

            self.$parent.variants.push(data);

            if (!!parseInt(self.isBase, 10)) {
                self.$parent.product = data;
                self.$parent.variantAxes = JSON.parse(JSON.stringify(data.variantAxes));
            }
        }
    });

    if (document.querySelector('.we-Product')) {
        new Vue({
            name: 'we-Product',
            el: '.we-Product',
            data: {
                variants: [],
                product: null,
                variantAxes: null,

                isChecked: function (name, value) {
                    if (name == 'color' && this.product.variantAxes[name] == value) {
                        $("input[name='color']").siblings('div').removeClass('tick');
                        $("input[value='" + value + "']").siblings('div').addClass('tick');
                    }

                    return this.product.variantAxes[name] == value;
                }
            },
            props: [
                'sku',
                'title',
                'pagePath'
            ],
            ready: function () {
                this.processHash();
                this.trackView();
            },
            methods: {
                _setProduct: function (name, value) {
                    var self = this;
                    self.variantAxes[name] = value;

                    var done = false;
                    self.variants.forEach(function (product) {
                        if (done) {
                            return;
                        }

                        var ok = true;
                        for (var key in self.variantAxes) {
                            if (self.variantAxes.hasOwnProperty(key)) {
                                if (product.variantAxes[key] != self.variantAxes[key]) {
                                    ok = false;
                                    break;
                                }
                            }
                        }

                        if (ok)
                        {
                            done = true;
                            self.product = product;
                            history.pushState(null, null, '#' + product.sku);
                        }
                    });
                },
                setProduct: function (event) {
                    var name = event.currentTarget.attributes['name'].value;
                    var value = event.currentTarget.attributes['value'].value;
                    this._setProduct(name, value);

                    if (name == 'color') {
                        $("input[name='color']").siblings('span').removeClass('tick');
                        $("input[value='" + value + "']").siblings('span').addClass('tick');
                    }
                },
                trackView: function() {
                    if (this.product && window.ContextHub && ContextHub.getStore("recentlyviewed")) {
                        ContextHub.getStore("recentlyviewed").record(
                            this.product.pagePath,
                            this.product.title,
                            this.product.thumbnail,
                            this.product.price
                        );
                    }

                },
                trackCartAdd: function (event) {
                    if (this.product && window.ContextHub && ContextHub.getStore("abandonedproducts")) {
                        ContextHub.getStore("abandonedproducts").record(
                            this.product.pagePath,
                            this.product.title,
                            this.product.thumbnail,
                            this.product.price
                        );
                    }
                    window.cartComponent.show();
                },
                addToWishlist: function (event) {
                    if (this.product) {
                        var $form = $(event.target).closest('form');
                        $.ajax({
                            url: event.currentTarget.getAttribute("data-smartlist-url"),
                            data: $form.serialize(),
                            cache: false,
                            type: $form.attr('method')
                        }).done(function (json) {
                            if (window.ContextHub && ContextHub.getStore('smartlists')) {
                                if (ContextHub.getStore('smartlists').getTree().length == 0) {
                                    // wait until new created smart list is available, which may take > 1 sec
                                    var smartlistCheck = setInterval(function(){
                                        ContextHub.getStore('smartlists').queryService();
                                        if (ContextHub.getStore('smartlists').getTree().length > 0) {
                                            clearInterval(smartlistCheck);
                                            window.smartlistComponent.show();
                                        }
                                    }, 500);
                                } else {
                                    ContextHub.getStore('smartlists').queryService();
                                    window.smartlistComponent.show();
                                }
                            }
                        }).fail(function () {
                            alert('An error occured while trying to perform this operation.');
                        });
                    }

                },
                processHash: function () {
                    var self = this;
                    var done = false;
                    if (window.location.hash) {
                        var sku = window.location.hash.slice(1);
                        self.variants.forEach(function (product) {
                            if (done) {
                                return;
                            }

                            if (sku == product.sku) {
                                self.product = product;
                                self.variantAxes = JSON.parse(JSON.stringify(product.variantAxes));
                                done = true;
                            }
                        });
                    }

                    if (!done) {
                        history.pushState(null, null, '#' + self.product.sku);
                    }
                }
            }
        });
    }

}).call(this);
