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

    var EXPANDABLE_CLASS = 'we-Smartlist-expandable',
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
    
    Vue.component('smartlist-content', {
        ready: function() {
            // move smartlist contents to body
            // so we won't interfere with any mobile styles
            document.body.appendChild(this.$el);
            if (window.ContextHub) {
                ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":smartlists", this.refreshSmartlist);
            }
            window.smartlistContent = this;
        },
        data: function() {
            var _smartlists = window.ContextHub ? ContextHub.getStore('smartlists') : null;
            if (_smartlists) {
                return {
                    smartlist: _smartlists.getTree()[0],
                    smartlistEntriesSize: _smartlists.getTree()[0] ? _smartlists.getTree()[0].entries.length : 0
                }
            }
            else {
                return {
                    smartlist: undefined,
                    smartlistEntriesSize: 0
                }
            }
        },
        events: {
            'smartlist-button-expand': function(show) {
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
            refreshSmartlist: function(event) {
                var _smartlists = window.ContextHub ? ContextHub.getStore('smartlists') : null;
                if (_smartlists) {
                    this.$data.smartlist = _smartlists.getTree()[0];
                    this.$data.smartlistEntriesSize = _smartlists.getTree()[0] ? _smartlists.getTree()[0].entries.length : 0;
                }
            },
            updateSmartlist: function(event) {
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
                    var _smartlists = window.ContextHub ? ContextHub.getStore('smartlists') : null;
                    if (_smartlists) {
                        _smartlists.queryService();
                    }
                }).fail(function () {
                    alert('An error occured while trying to perform this operation.');
                });
            },
            onAddToCartSubmit: function(event) {
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
                    window.cartComponent.show();
                }).fail(function () {
                    alert('An error occured while trying to perform this operation.');
                });
            },
            setTop: function(show) {
                // handle fixed in js
                // position fixed in css doesn't work with transform
                this._fixed = this._fixed || new Fixed(this.$el);
                this._fixed.on();
                this._fixed.onScroll();
            }
        }
    });

    Vue.component('we-Smartlist-button', {});

    var SmartlistComponent = Vue.extend({
        ready: function() {
            this.$expandable = $(this.$el).closest(EXPANDABLE_SELECTOR);
            this.$expandable.addClass(EXPANDABLE_CLASS);
            if (window.ContextHub) {
                ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":smartlists", this.refreshSmartlist);
            }
            window.smartlistComponent = this;
        },
        data: function() {
            var _smartlists = window.ContextHub ? ContextHub.getStore('smartlists') : null;
            return {
                smartlistEntriesSize: (_smartlists && _smartlists.getTree()[0]) ? _smartlists.getTree()[0].entries.length : 0
            }
        },
        methods: {
            toggle: function() {
                var $el = this.$expandable;

                if ($el.hasClass(EXPAND_SMARTLIST_VALUE)) {
                    $el.removeClass(EXPAND_SMARTLIST_VALUE);
                    $(".we-Smartlist-content").hide();
                    this.$root.$broadcast('smartlist-button-expand', false);
                } else {
                    $el.removeClass(EXPAND_CART_VALUE);
                    $el.addClass(EXPAND_SMARTLIST_VALUE);
                    $(".we-Cart-content").hide();
                    $(".we-Smartlist-content").show()
                    this.$root.$broadcast('smartlist-button-expand', true);
                }
            },
            refreshSmartlist: function(event) {
                var _smartlists = window.ContextHub ? ContextHub.getStore('smartlists') : null;
                if (_smartlists) {
                    this.$data.smartlistEntriesSize = _smartlists.getTree()[0] ? _smartlists.getTree()[0].entries.length : 0;
                }
            },
            show: function() {
                var $el = this.$expandable;

                if (!$el.hasClass(EXPAND_SMARTLIST_VALUE)) {
                    $el.removeClass(EXPAND_CART_VALUE);
                    $el.addClass(EXPAND_SMARTLIST_VALUE);
                    $(".we-Cart-content").hide();
                    $(".we-Smartlist-content").show()
                    // this.$root.$broadcast('smartlist-button-expand', true); does not work
                    // when this method is called by product.js, so this is a workaround
                    window.smartlistContent.setTop();
                }
            }
        }
    });

    function init() {
        $('.we-Smartlist').each(function() {
            new SmartlistComponent().$mount(this);
        });
    }

    $(document).bind('we-header-loaded', function() {
        init();
    });

    init();


}).call(this);