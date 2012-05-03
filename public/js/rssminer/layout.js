(function () {
  var user = (_RM_ && _RM_.user) || {},
      user_conf = user.conf || {},
      util = RM.util;

  var $win = $(window),
      $nav = $('#navigation');

  function layout () {
    var height = $win.height() - $('#header').height();
    $nav.height(height);
    $("#reading-area").height(height);
  }

  function scroll_to_view ($container, $element) {
    if($container.length && $element.length) {
      var ct = $container.offset().top,
          ch = $container.height(),
          eh = $element.height(),
          et = $element.offset().top;
      if(et < ct) {               // hide in the above
        $container[0].scrollTop -= ct - et;
      } else if( ct + ch < et) {  // hide in the bottom
        $container[0].scrollTop += et - ct - eh * 2;
      }
    }
  }

  function select (context, id) {
    var $me = $('#' + id);
    if(!$me.hasClass('selected')) {
      $(".selected", context).removeClass('selected');
      $me.addClass('selected');
      _.defer(function () {
        // expand navigation if collapsed
        $me.closest('li.collapse').removeClass('collapse');
        // current sub-list and feed-list are all in navigation
        scroll_to_view($nav, $me);
      });
      return true;
    }
    return false;
  }

  $win.resize(_.debounce(layout, 100));
  layout();

  window.RM = $.extend(window.RM, {
    layout: {
      select: select
    }
  });
})();
