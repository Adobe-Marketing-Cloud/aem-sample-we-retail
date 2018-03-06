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
(function($, document) {
    'use strict';

    var EXPANDABLE_CLASS = 'we-Cart-expandable',
        EXPAND_SMARTLIST_VALUE = 'smartlist-expanded',
        EXPAND_CART_VALUE = 'cart-expanded',

        EXPANDABLE_SELECTOR = 'body';

    var _fixed = null;

    var Fixed = function($el) {
        this.$el = $($el);
        this.$window = $(window);

        this._onScroll = _.throttle(this.onScroll.bind(this), 100);
    };

    Fixed.prototype.onScroll = function() {
        this.$el.css('top', this.$window.scrollTop());
    };

    Fixed.prototype.on = function() {
        this.$window.on('scroll', this._onScroll);
    };

    Fixed.prototype.off = function() {
        this.$window.off('scroll', this._onScroll);
    };

    Vue.component('cart-content', {
        ready: function() {
            // move cart contents to body
            // so we won't interfere with any mobile styles
            document.body.appendChild(this.$el);
            if (window.ContextHub) {
                ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":cart", this.refreshCart);
            }
            window.cartContent = this;
        },
        data: function() {
            var _cart = window.ContextHub ? ContextHub.getStore('cart') : null; 
            if (_cart) {
                return {
                    cartEntries: _cart.getItem('entries'),
                    cartEntriesSize: _cart.getItem('entries') ? _cart.getItem('entries').length : 0,
                    cartTotalPrice: _cart.getItem('totalPrice'),
                    cartPromotions: _cart.getItem('promotions')
                }
            }
            else {
                return {
                    cartEntries: [],
                    cartEntriesSize: 0,
                    cartTotalPrice: '0.00',
                    cartPromotions: []
                }
            }
        },
        computed: {
            orderPromotions: function () {
                if (this.cartPromotions) {
                    return this.cartPromotions.filter(function (promotion) {
                        return promotion.cartEntryIndex === undefined || promotion.cartEntryIndex === null;
                    })
                } else {
                    return null;
                }
            }
        },
        events: {
            'cart-button-expand': function(show) {
                // handle fixed in js
                // position fixed in css doesn't work with transform
                this._fixed = this._fixed || new Fixed(this.$el);
                if (show) {
                    this._fixed.on();
                } else {
                    this._fixed.off();
                }
            }
        },
        methods: {
            refreshCart: function(event) {
                var _cart = window.ContextHub ? ContextHub.getStore('cart') : null;
                if (_cart) {
                    this.$data.cartEntries = _cart.getItem('entries');
                    this.$data.cartEntriesSize = _cart.getItem('entries') ? _cart.getItem('entries').length : 0;
                    this.$data.cartTotalPrice = _cart.getItem('totalPrice');
                    this.$data.cartPromotions = _cart.getItem('promotions');
                }
            },
            updateCart: function(event) {
                if (parseInt($(event.target).val()) < 0) {
                    return;
                }
                var $form = $(event.target).closest('form');
                $.ajax({
                    url: $form.attr('action'),
                    data: $form.serialize(),
                    cache: false,
                    type: $form.attr('method')
                }).done(function (json) {
                    var _cart = window.ContextHub ? ContextHub.getStore('cart') : null;
                    if (_cart) {
                        _cart.queryService();
                    }
                }).fail(function () {
                    alert('An error occured while trying to perform this operation.');
                });
            },
            setTop: function() {
                // handle fixed in js
                // position fixed in css doesn't work with transform
                this._fixed = this._fixed || new Fixed(this.$el);
                this._fixed.on();
                this._fixed.onScroll();
            },
            cartEntryPromotions: function(i) {
                if (this.cartPromotions) {
                    return this.cartPromotions.filter(function (promotion) {
                        return promotion.cartEntryIndex == i;
                    })
                } else {
                    return null;
                }
            }
        }
    });

    var CartComponent = Vue.extend({
        ready: function() {
            this.$expandable = $(this.$el).closest(EXPANDABLE_SELECTOR);
            this.$expandable.addClass(EXPANDABLE_CLASS);
            if (window.ContextHub) {
                ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":cart", this.refreshCart);
            }
            window.cartComponent = this;
        },
        data: function() {
            var _cart = window.ContextHub ? ContextHub.getStore('cart') : null;
            return {
                cartEntriesSize: (_cart && _cart.getItem('entries')) ? _cart.getItem('entries').length : 0
            }
        },
        methods: {
            toggle: function() {
                var $el = this.$expandable;

                if ($el.hasClass(EXPAND_CART_VALUE)) {
                    $el.removeClass(EXPAND_CART_VALUE);
                    $(".we-Cart-content").hide();
                    this.$root.$broadcast('cart-button-expand', false);
                } else {
                    $el.addClass(EXPAND_CART_VALUE);
                    $el.removeClass(EXPAND_SMARTLIST_VALUE);
                    $(".we-Cart-content").show();
                    $(".we-Smartlist-content").hide();
                    this.$root.$broadcast('cart-button-expand', true);
                }
            },
            refreshCart: function(event) {
                var _cart = window.ContextHub ? ContextHub.getStore('cart') : null;
                if (_cart) {
                    this.$data.cartEntriesSize = _cart.getItem('entries') ? _cart.getItem('entries').length : 0;
                }
            },
            show: function() {
                var $el = this.$expandable;

                if (!$el.hasClass(EXPAND_CART_VALUE)) {
                    $el.addClass(EXPAND_CART_VALUE);
                    $el.removeClass(EXPAND_SMARTLIST_VALUE);
                    $(".we-Cart-content").show();
                    $(".we-Smartlist-content").hide();
                    // this.$root.$broadcast('smartlist-button-expand', true); does not work
                    // when this method is called by product.js, so this is a workaround
                    window.cartContent.setTop();
                }
            }
        }
    });

    function init() {
        $('.we-Cart').each(function() {
            new CartComponent().$mount(this);
        });
    }

    $(document).bind('we-header-loaded', function() {
        init();
    });

    init();

})(jQuery, document);