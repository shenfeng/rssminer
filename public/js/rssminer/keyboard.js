(function () {

  var location = window.location,
      notify = window.RM.notify;

  var $logo = $('#logo'),
      $feed_content = $('#feed-content'),
      $reading_area = $('#reading-area'),
      $subs_list = $('#sub-list'),    // sub list
      $welcome_list = $('#welcome-list');

  var SHOW_NAV = 'show-nav',
      SHOW_CONTENT = 'show-content';

  function is_reading () {
    return $reading_area.hasClass(SHOW_CONTENT);
  }

  var prev_key = 0;

  function keyboard_shortcut (e) {
    if(e.which === 191) {       // /
      $('#q').click();
      return false;
    }
    if(is_reading()) {
      var $feed_list = $('#feed-list'),
          $current = $('#feed-list .selected'),
          $next = $current.next(),
          $prev = $current.prev();
      switch(e.which) {
      case 85:
        var args = /#read\/(.+)\/\d+\?(.+?)s=(.+)/.exec(location.hash);
        // console.log(args);
        if(/^\d+$/.test(args[1]) || /f_.*/.test(args[1])) {
          location.hash = 'read/' + args[1] + '?p=1&s=' + args[3];
        } else {
          location.hash = '?s=' + args[1] + '&p=1';
        }
        break;
      case 75:                  // j
        if(!$prev.length) {
          $prev = $('#feed-list .feed:first');
        }
        location.hash = $prev.find('a').attr('href');
        break;
      case 74:                  // k
        //  load more
        if(!$next.next().length) { $feed_content.scroll(); }
        if($next.length) {
          location.hash = $next.find('a').attr('href');
        } else {
          notify.show_msg('No more', 5000);
        }
        break;
      }
    } else {
      var $all = $subs_list.find('.folder, .item');
      var $selected = $all.filter('.selected')[0];
      var idx = $.inArray($selected, $all);
      var pre = 0, next = 0;
      if(idx !== -1) {
        pre = idx === 0 ? 0 : idx - 1;
        next = idx === $all.length -1 ? idx : idx + 1;
      } else { pre = 0; next = 0; }
      switch(e.which) {
      case 74:                  // j
        var $n = $($all[next]);
        var href = $n.find('a').attr('href');
        if(!href) {             // a, folder
          href = $n.attr('href');
        }
        location.hash = href;
        break;
      case 75:                  // k
        var $p = $($all[pre]);
        var href = $p.find('a').attr('href');
        if(!href) {             // a, folder
          href = $p.attr('href');
        }
        location.hash = href;
        break;
      case 79:
        var $f = $welcome_list.find('.feed:first').find('a');
        var href = $f.attr('href');
        location.hash = href;
        break;
      case 84:                  // t, swith tab
        var $tabs = $('.sort li');
        var selected = $tabs.filter('.selected')[0];
        var idx = $.inArray(selected, $tabs);
        if(idx === $tabs.length - 1) {
          idx = 0;
        } else {
          idx += 1;
        }
        location.hash = $($tabs[idx]).find('a').attr('href');
        break;
      }
    }
    prev_key = e.which;
  }

  $(document).keydown(keyboard_shortcut);
})();
