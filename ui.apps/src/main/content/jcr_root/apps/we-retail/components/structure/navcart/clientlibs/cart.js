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

    var EXPANDABLE_CLASS = 'we-Cart-expandable',
        EXPAND_VALUE = 'expanded',

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
        }
    });

    Vue.component('cart-button', {});

    var CartComponent = Vue.extend({
        ready: function() {
            this.$expandable = $(this.$el).closest(EXPANDABLE_SELECTOR);
            this.$expandable.addClass(EXPANDABLE_CLASS);
        },
        methods: {
            toggle: function() {
                var $el = this.$expandable;

                if ($el.hasClass(EXPAND_VALUE)) {
                    $el.removeClass(EXPAND_VALUE);
                    this.$root.$broadcast('cart-button-expand', false);
                } else {
                    $el.addClass(EXPAND_VALUE);
                    this.$root.$broadcast('cart-button-expand', true);
                }
            }
        }
    });

    $('.we-Cart').each(function() {
        new CartComponent().$mount(this);
    });

}).call(this);