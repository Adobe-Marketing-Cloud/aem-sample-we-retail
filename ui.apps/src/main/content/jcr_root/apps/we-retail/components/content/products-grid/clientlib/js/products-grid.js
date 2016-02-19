(function () {
    'use strict';

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
                items = $el.find('.foundation-list-item').length,
                visibleItems = $el.find('.foundation-list-item:visible').length,
                pagesCount = Math.ceil(items / visibleItems);

            // initialize mobile pagination with calculated pages
            this.pages = _.range(pagesCount);
        }
    });

    // attach component to all occurances of .products-grid element
    $('.products-grid').each(function() {
        new component().$mount(this);
    });

}).call(this);