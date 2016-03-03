(function () {
    'use strict';

    var EXPANDABLE_CLASS = 'we-Cart-expandable',
        EXPAND_VALUE = 'expanded';

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


    Vue.component('cart-content', {});


    new Vue({
        parent: we.app,
        el: '.we-Cart',
        name: 'cart',
        props: [
            'cartExpandable'
        ],
        ready: function() {
            console.log('cart vue!!!');
            this.$expandable = $(this.$el).closest(this.cartExpandable);
            this.$expandable.addClass(EXPANDABLE_CLASS);
        },
        methods: {
            toggle: function() {
                var $el = this.$expandable;
                _fixed = _fixed || new Fixed(this.$refs.content.$el);

                if ($el.hasClass(EXPAND_VALUE)) {
                    $el.removeClass(EXPAND_VALUE);
                    _fixed.off();
                } else {
                    $el.addClass(EXPAND_VALUE);
                    _fixed.on();
                }
            }
        }
    });

}).call(this);