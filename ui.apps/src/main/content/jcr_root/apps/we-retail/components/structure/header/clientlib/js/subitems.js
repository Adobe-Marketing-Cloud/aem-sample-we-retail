(function ($) {
    $(function () {
        var $navbarShutter = $('.navbar-absolute-top .navbar-shutter');

        $('.navbar-absolute-top')
            .on('mouseenter', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
                e.delegateTarget.classList.add('submenu-opened');
                $navbarShutter.height($(e.target).next('.navbar-nav-subitems').innerHeight(true));
            })
            .on('mouseleave', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
                e.delegateTarget.classList.remove('submenu-opened');
                $navbarShutter.height(0);
            });
    });
})(jQuery);