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
    $(function () {
        $('.we-retail-header').on('click.we-retail.modal', '[data-toggle="modal"][data-target]', function (event) {
            event.preventDefault();
            event.stopPropagation();
            $($(this).attr('data-target')).modal('show');
        });

        $('#navbar-search').on('show.bs.modal hidden.bs.modal', function () {
            $('body').toggleClass('modal-color-'+ $(this).data('color'));
        });

        $('#navbar-search').on('shown.bs.modal', function () {
            $(this).find('.cmp-search__input').trigger('focus');
        }).on('hidden.bs.modal', function () {
            var input = $(this).find('.cmp-search__input').val('').get(0);
            if (input) {
                input.dispatchEvent(new Event('input'));
            }
        });
    });
})(jQuery);
