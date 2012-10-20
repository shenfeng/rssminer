(function () {
  // cache
  var $win = $(window),
      $nav = $('#navigation'),
      $header = $('#header'),
      $reading_area = $('#reading-area'),
      $subs_list = $('#sub-list');

  var SELECTED = 'selected';

  function layout () {
    var height = $win.height() - $header.height();
    $reading_area.height(height);
    $('.scroll-wrap').each(function (idx, e) {
      var $e = $(e),
          $c = $(e.children[0]);
      $c.height(height).width($e.width() + scrollbar_size);
    });
  }

  var scrollbar_size = (function () {
    var div = $(
      '<div style="width:50px;height:50px;overflow:hidden;'
        + 'position:absolute;top:-200px;left:-200px;"><div style="height:100px;">'
        + '</div>'
    );

    $('body').append(div);
    var w1 = $('div', div).innerWidth();
    div.css('overflow-y', 'scroll');
    var w2 = $('div', div).innerWidth();
    $(div).remove();
    return  w1 - w2;
  })();


  function scroll_into_view ($me) {
    if($me.length) {
      var me = $me[0],
          rect = me.getBoundingClientRect();
      if(rect.top <= 30 || rect.top >= $win.height() - 30) {
        me.scrollIntoView();
      }
    }
  }

  function select (context, $me) {
    scroll_into_view($me);
    if(!$me.hasClass(SELECTED)) {
      $("." + SELECTED, context).removeClass(SELECTED);
      $me.addClass(SELECTED);
      _.defer(function () {
        // expand navigation if collapsed
        $me.closest('li.collapse').removeClass('collapse');
      });
      return true;
    }
    return false;
  }

  $win.resize(_.debounce(layout, 100));

  layout();

  window.RM = $.extend(window.RM, {
    layout: { select: select }
  });
})();
