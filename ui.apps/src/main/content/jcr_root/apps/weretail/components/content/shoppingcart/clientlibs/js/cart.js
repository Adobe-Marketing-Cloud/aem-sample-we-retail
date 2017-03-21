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
(function ($) {
    'use strict';

    var bindFormElements = function () {
        // bind form handler
        $('.we-ShoppingCart form, .we-Cart-content form, .we-Product-form').submit(submitForm);
        // submit quantity form on change
        $('.we-ShoppingCart form input[name="quantity"], .we-Cart-content form input[name="quantity"]').change(function () {
            if (parseInt($(this).val()) >= 0) {
                $(this).closest('form').submit();
            }
        });

    }

    var toggleCartEmptyMsg = function () {
        if ($('div.we-ShoppingCart div.we-ShoppingCart-empty').length > 0) {
            $('a.btn-checkout').hide();
        } else {
            $('a.btn-checkout').show();
        }
    }

    var submitForm = function (event) {
        event.preventDefault();
        var $form = $(event.target);
        $.ajax({
            url: $form.attr('action'),
            data: $form.serialize(),
            cache: false,
            type: 'POST'
        }).done(function (json) {
            var _cart = window.ContextHub ? ContextHub.getStore('cart') : null;
            if (_cart) {
                _cart.queryService();
            } else {
                refreshCart();
            }
        }).fail(function () {
            alert('An error occured while trying to perform this operation.');
        });
    }

    var refreshCart = function () {
        var shoppingCart = $('div.we-ShoppingCart');
        shoppingCart.parent().load(Granite.HTTP.externalize(shoppingCart.data("resource") + ".html"), function () {
            bindFormElements();
            toggleCartEmptyMsg();
        });
    }

    bindFormElements();
    toggleCartEmptyMsg();

    if (window.ContextHub) {
        ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":cart", refreshCart);
    }

})(jQuery);