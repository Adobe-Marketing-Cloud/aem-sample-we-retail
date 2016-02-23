(function () {
    'use strict';
    function isElementInViewport () {
        var el = this,
            rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    var component = Vue.extend({
        data: function() {
          return{
            page: 0,
            pages: []
          }
        },
        beforeCompile: function() {
            // observe page property
            this.$el.setAttribute(':data-page', 'page');

            // add mobile pagination template
            this.$el.innerHTML += [
                '<div class="products-grid-mobilePagination" v-if="pages.length > 1">',
                    '<a v-for="index in pages" v-on:click="page = index" :class="{active: (page == index)}">{{ index }}</a>',
                '</div>'].join('');
        },
        ready: function() {
            var $el = $(this.$el),
                $items = $el.find('.foundation-list-item'),
                $visibleItems = $items.filter(isElementInViewport),
                pagesCount = Math.ceil($items.length / ($visibleItems.length || $items.length));

            // initialize mobile pagination with calculated pages
            this.pages = _.range(pagesCount);
        }
    });

    // attach component to all occurances of .products-grid element
    $('.products-grid').each(function() {
        new component().$mount(this);
    });

}).call(this);