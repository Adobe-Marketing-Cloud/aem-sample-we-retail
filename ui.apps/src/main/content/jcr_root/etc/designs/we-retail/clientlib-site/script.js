/** a JS file that shall be included */

(function () {
    'use strict';

    window.we = window.we || {};
    we.app = new Vue({name: 'we-retail'});

    Vue.component('cq', function () {}); // Remove warns about <cq> elements

}).call(this);