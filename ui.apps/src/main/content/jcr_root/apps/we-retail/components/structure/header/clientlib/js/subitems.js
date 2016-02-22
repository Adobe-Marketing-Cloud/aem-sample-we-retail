(function ($) {
    $(function () {
        var $navbarShutter = $('.navbar-absolute-top .navbar-shutter');

        $('.navbar-absolute-top').on('mouseenter mouseleave', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
            e.delegateTarget.classList.toggle('submenu-opened');
            $navbarShutter.height(
                e.delegateTarget.classList.contains('submenu-opened') ?
                    $(e.target).next('.navbar-nav-subitems').innerHeight(true) : 0
            );
        });
    });
})(jQuery);