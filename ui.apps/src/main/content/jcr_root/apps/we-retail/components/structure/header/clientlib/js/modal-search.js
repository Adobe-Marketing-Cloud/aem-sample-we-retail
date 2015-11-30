(function ($) {
    $(function () {
        $('#navbar-search').on('show.bs.modal hidden.bs.modal', function () {
            $('body').toggleClass('modal-color-'+ $(this).data('color'));
        });
    });
})(jQuery);