(function () {
  function layout () {
    var width = $(window).width(),
        height = $(window).height(),
        nav_width = $('#navigation').width(),
        list_height = $('#footer').height();
    $("#navigation .wrapper").height(height - $("#admin-controls").height());
    $("#reading-area").height(height - list_height).width(width - nav_width);
  }

  $(window).resize(_.debounce(layout, 100));

  layout();

  (function () {
    var down = false,
        startY,
        old_footer_height,
        old_list_height,
        $footer = $('#footer'),
        $list = $('#feed-list');

    function noop () { return false; }

    $(document).bind('mousedown', function (e) {
      var $target = $(e.target);
      if($target.hasClass('resizer') || $target.parents('.resizer').length) {
        startY = e.clientY;
        down = true;
        old_footer_height = $footer.height();
        old_list_height = $list.height();
        $(document).bind('selectstart', noop);
      }
    }).bind('mouseup', function (e) {
      $(document).unbind('selectstart', noop);
      down = false;
    }).bind('mousemove', function (e) {
      if(down) {
        var delta = e.clientY - startY;
        $footer.height(old_footer_height - delta);
        $list.height(old_list_height  - delta);
        layout();
      }
    });
  })();

  function scrollIntoView ($container, $element) {
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

  function select (context, id) {
    var $me = $('#' + id);
    if($me.hasClass('selected')) {
      return false;
    } else {
      $(".selected", context).removeClass('selected');
      $me.addClass('selected');
      var $container = $me.parents('.wrapper').length > 0 ?
            $me.parents('.wrapper') : $me.parents('#feed-list');
      _.defer(function () {
        scrollIntoView($container, $me);
      });
      return true;
    }
  }

  window.RM = $.extend(window.RM, {
    layout: {
      select: select
    }
  });

})();
