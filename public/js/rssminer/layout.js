(function () {
  var $footer = $('#footer'),
      $list = $('#feed-list'),
      util = RM.util;

  function layout () {
    var width = $(window).width(),
        height = $(window).height(),
        nav_width = $('#navigation').width(),
        list_height = $footer.height();
    $("#navigation .wrapper").height(height - $("#admin-controls").height());
    $("#reading-area").height(height - list_height).width(width - nav_width);
  }

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
    if(!$me.hasClass('selected')) {
      $(".selected", context).removeClass('selected');
      $me.addClass('selected');
      var $container = $me.parents('.wrapper').length > 0 ?
            $me.parents('.wrapper') : $me.parents('#feed-list');
      _.defer(function () {
        scrollIntoView($container, $me);
      });
      return true;
    }
    return false;
  }

  function toggleNavigationSection (e) {
    $(this).parents('.section').toggleClass('active');
  }

  function toggleFolder (e) {
    $(this).closest('li').toggleClass('collapse');
    var collapsed = [];
    $('#navigation li.collapse .folder').each(function (index, item) {
      collapsed.push($(item).attr('data-name'));
    });
    RM.ajax.jpost('/api/user/pref', {nav: collapsed});
    return false;
  }

  (function () {                        // footer height resize
    var down = false,
        startY,
        updated = false,
        old_footer_height,
        old_list_height;

    function noop () { return false; }

    $(document).bind('mousedown', function (e) {
      if( e.button === 0 ) {       // left
        var $target = $(e.target);
        if($target.hasClass('row-resize')) {
          startY = e.clientY;
          down = true;
          $footer.css('cursor', 'row-resize');
          old_footer_height = $footer.height();
          old_list_height = $list.height();
          $(document).bind('selectstart', noop);
        }
      }}).bind('mouseup', function (e) {
        if(e.button ===0) {
          $(document).unbind('selectstart', noop);
          $footer.css('cursor', 'auto');
          if(updated) {                // save on server
            RM.ajax.jpost('/api/user/pref', {height: $list.height()});
          }
          down = false;
          updated = false;
        }
      }).bind('mousemove', function (e) {
        if(down) {
          var delta = e.clientY - startY;
          updated = true;
          $footer.height(old_footer_height - delta);
          $list.height(old_list_height  - delta);
          layout();
        }
      });
  })();

  // user's last height of feed list
  if(_RM_.user && _RM_.user.conf && _RM_.user.conf.height) {
    $list.height(_RM_.user.conf.height);
    $footer.height(_RM_.user.conf.height + $('#footer .resizer').height());
  }

  $(window).resize(_.debounce(layout, 100));
  layout();

  window.RM = $.extend(window.RM, {
    layout: {
      reLayout: layout,
      select: select
    }
  });

  util.delegateEvents($(document), {
    'click #navigation .section h3': toggleNavigationSection,
    'click #navigation .folder span': toggleFolder
  });

})();
