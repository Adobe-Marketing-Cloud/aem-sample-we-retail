(function ($) {
    $(function () {
        $('.navbar-absolute-top').on('mouseenter mouseleave', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
            e.delegateTarget.classList.toggle('submenu-opened');
        });
    });
})(jQuery);