(function () {
  "use strict";
  var location = window.location,
      notify = window.RM.notify;

  var $logo = $('#logo'),
      $q = $('#q'),
      $feed_content = $('#feed-content'),
      $reading_area = $('#reading-area'),
      $subs_list = $('#sub-list'),    // sub list
      $navigation = $('#navigation'), // feed list
      $welcome_list = $('#welcome-list');

  var SHOW_NAV = 'show-nav',
      STEP = 90,
      ABOUT_HASH = '#s/help',
      SHOW_CONTENT = 'show-content';

  function is_reading () {
    return $reading_area.hasClass(SHOW_CONTENT);
  }

  function list_shortcuts (e) {
    var $all = $subs_list.find('.folder, .item'),
        $selected = $all.filter('.selected')[0],
        idx = $.inArray($selected, $all),
        href,
        pre = 0,
        next = 0;
    if(idx !== -1) {
      pre = idx === 0 ? 0 : idx - 1;
      next = idx === $all.length -1 ? idx : idx + 1;
    } else {
      pre = 0;
      next = 0;
    }
    switch(e.which) {
    case 74:                  // j
      var $n = $($all[next]);
      href = $n.find('a').attr('href');
      if(!href) {             // a, folder
        href = $n.attr('href');
      }
      location.hash = href;
      break;
    case 75:                  // k
      var $p = $($all[pre]);
      href = $p.find('a').attr('href');
      if(!href) {             // a, folder
        href = $p.attr('href');
      }
      location.hash = href;
      break;
    case 79:                    // o, open first
      var $f = $welcome_list.find('.feed:first').find('a');
      href = $f.attr('href');
      location.hash = href;
      break;
    case 84:                  // t, swith tab
      var $tabs = $('.sort li');
      var selected = $tabs.filter('.selected')[0];
      var s = $.inArray(selected, $tabs);
      if(s === $tabs.length - 1) {
        s = 0;
      } else {
        s += 1;
      }
      location.hash = $($tabs[s]).find('a').attr('href');
      break;
    case 40:                    // down
    case 78:                    // n
      if(location.hash === ABOUT_HASH) {
        $welcome_list[0].scrollTop += STEP;
      }
      break;
    case 38:                    // up
    case 80:                    // p
      if(location.hash === ABOUT_HASH) {
        $welcome_list[0].scrollTop -= STEP;
      }
      break;
    }
  }

  function reading_shortcuts (e) {
    console.log(e.which);
    var $feed_list = $('#feed-list'),
        $current = $('#feed-list .selected'),
        $next = $current.next(),
        $prev = $current.prev();
    switch(e.which) {
    case 79:                    // o open the link
      window.open($('.reading .link').attr('href'));
      break;
    case 85:                    // u, return to list
      var args = /#read\/(.+)\/\d+\?(.+?)s=(.+)/.exec(location.hash);
      if(/^\d+$/.test(args[1]) || /f_.*/.test(args[1])) {
        location.hash = 'read/' + args[1] + '?p=1&s=' + args[3];
      } else {
        location.hash = '?s=' + args[1] + '&p=1';
      }
      break;
    case 75:                    // k
      if(!$prev.length) {
        $prev = $('#feed-list .feed:first');
      }
      location.hash = $prev.find('a').attr('href');
      break;
    case 74:                    // j
      //  load more, logic in app.js
      if(!$next.next().length) {
        $navigation.scroll();
      }
      if($next.length) {
        location.hash = $next.find('a').attr('href');
      } else {
        notify.show_msg('No more', 5000);
      }
      break;
    case 40:                    // down
    case 78:                    // n
      $reading_area[0].scrollTop += STEP;
      break;
    case 38:                    // up
    case 80:                    // p
      $reading_area[0].scrollTop -= STEP;
      break;
    }
  }

  var prev_key = 0;

  function keyboard_shortcut (e) {
    if($('input').is(':focus')) { return; }

    if(prev_key === 71) {       // g
      if(e.which === 65 || e.which === 72) {      // a(ll) or h(ome)
        location.hash = '';     // go home
        return;
      }
    }

    prev_key = e.which;
    if(e.which === 191) {       // /
      if(e.shiftKey) {
        $('#shortcuts').fadeIn();
        $('#overlay').show();
        // location.hash = ABOUT_HASH;
      } else {
        $q.focus().click();
      }
      return false;
    }
    if(is_reading()) { reading_shortcuts(e);}
    else { list_shortcuts(e); }
  }

  $(document).keydown(keyboard_shortcut);
})();
