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

    var BILLING_IS_SHIPPING_CHECKBOX_SELECTOR = "input:checkbox[name='billing-is-shipping-address']",
        BILLING_INPUT_FIELD_SELECTOR = "input[name^='billing.']",
        BILLING_SELECT_FIELD_SELECTOR = "select[name^='billing.']",
        COUNTRY_SELECT_FIELD_SELECTOR = "select[name$='.country']",
        USA_CODES = ['US', 'USA'];

    var $billingIsShippingCheckbox = $(BILLING_IS_SHIPPING_CHECKBOX_SELECTOR),
        $billingFields = $(BILLING_INPUT_FIELD_SELECTOR).add(BILLING_SELECT_FIELD_SELECTOR).parent(),
        $countryFields = $(COUNTRY_SELECT_FIELD_SELECTOR);


	var payment = "input:radio[name='payment-option']";

    if ($billingIsShippingCheckbox.is(':checked')) {
		$billingFields.hide();
    }
    
    var toogleStateSelect = function() {
        var $this = $(this);
        var stateName = $this.attr('name').replace('country', 'state');
        var selector = "select[name='" + encodeURI(stateName) + "']";
        if ($.inArray($this.val(), USA_CODES) > -1) {
            $(selector).show().parents('.cmp-form-options').removeClass('hidden');
        }
        else {
            $(selector).hide().parents('.cmp-form-options').addClass('hidden');
            $("select[name$='.state']").prop('selectedIndex', 0).change();
        }
    }
    
    $countryFields.each(toogleStateSelect).change(toogleStateSelect);
    
    if ($('div.we-MiniCart-empty:visible').length > 0) {
        $('#checkout button.btn-primary').hide();
        $('#order button.btn-primary').hide();
    }

    $(payment).each(function() {
        var $payment = $(this);
        if ($payment.is(':checked')) {
            if ($payment.val().endsWith('paypal')) {
                $("input[name^='card.']").hide();
            }
            else {
                $('#checkout .cmp-text').hide();
            }
        }
    });

    $billingIsShippingCheckbox.change(function() {
        $billingFields.toggle();
    });
    
    $(payment).change(function() {
        var $payment = $(this);
        if ($payment.is(':checked') && !$payment.val().endsWith('creditcard')) {
            $("input[name^='card.']").hide().val('');
            $('#checkout .cmp-text').show();
        }
        else {
            $("input[name^='card.']").show();
            $('#checkout .cmp-text').hide();
        }
    });
    
    $('#checkout').submit(function() {
        var $form = $(this);
        
        if ($billingIsShippingCheckbox.is(':checked')) {
            $("input[name^='billing.']").each(function() {
                var $this = $(this);
                var shippingName = $this.attr('name').replace('billing.', 'shipping.');
                $this.val($("input[name='" + encodeURI(shippingName) + "']").val());
            });
            $("select[name^='billing.']").each(function() {
                var $this = $(this);
                var shippingName = $this.attr('name').replace('billing.', 'shipping.');
                $this.val($("select[name='" + encodeURI(shippingName) + "']").val());
            });
        }
        
        // Browsers usually do not POST an "empty" selected option
        // To make sure we initialize the 'states' field if USA is not selected, we add an empty hidden input on the fly 
        $countryFields.each(function() {
            var $this = $(this);
            if ($.inArray($this.val(), USA_CODES) < 0) {
                var stateName = $this.attr('name').replace('country', 'state');
                $form.append('<input type="hidden" name="' + encodeURI(stateName) + '" value="" />');
            }
        });
    });

})(jQuery);