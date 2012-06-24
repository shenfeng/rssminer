(function () {
  var _RM_ = window._RM_ || {},
      RM = window.RM,
      data = RM.data,
      notify = RM.notify,
      tmpls = RM.tmpls,
      util = RM.util,
      layout = RM.layout,
      location = window.location,
      call_if_fn = util.call_if_fn;

  var SHOW_NAV = 'show-nav',
      SHOW_IFRAME = 'show-iframe';

  var gmark_as_read_timer_id = 0,
      gcur_page,
      gcur_sort,
      gcur_sub_id;

  var $footer = $('#footer'),
      $reading_area = $('#reading-area'),
      $navigation = $('#navigation'),
      $subs_list = $('#sub-list'),
      iframe = $('iframe')[0],
      $logo = $('#logo'),
      $welcome_list = $('#welcome-list');

  function set_document_title (title) {
    var rssminer = 'Rssminer, intelligent rss reader';
    if(title) {
      document.title = title + ' - ' + rssminer;
    } else {
      document.title = rssminer;
    }
  }

  function read_subscription (id, page, sort, callback) {
    $reading_area.removeClass(SHOW_IFRAME);
    layout.select('#sub-list', "item-" + id);
    data.get_feeds(id, page, sort, function (data) {
      show_feeds(data);
      call_if_fn(callback);
    });
  }

  function load_feeds_into_left_nav () {
    data.get_feeds(gcur_sub_id, gcur_page, gcur_sort, function (data) {
      $navigation.empty().append(tmpls.feeds_nav(data));
      $navigation.scrollTop(0);
    });
  }

  function load_next_page () {
    gcur_page += 1;
    load_feeds_into_left_nav();
  }

  function load_prev_page () {
    gcur_page -= 1;
    load_feeds_into_left_nav();
  }

  function decrement_number ($just_read, subid) {
    var selector = "#item-" + subid;
    if($just_read.hasClass('neutral')) {
      selector += ' .unread-neutral';
    } else if ($just_read.hasClass('like')) {
      selector += ' .unread-like';
    } else { selector += ' .unread-dislike'; }
    var $n = $(selector), n = + ($n.text() || "0").trim();
    if(n === 1) { $n.remove(); }
    else { $n.text(n-1); }
  }

  function clear_timer () {
    if(gmark_as_read_timer_id) {
      window.clearTimeout(gmark_as_read_timer_id);
      gmark_as_read_timer_id = null;
    }
  }

  function read_feed (subid, feedid, page, sort) {
    var read = function () {
      gcur_sub_id = subid;
      gcur_sort = sort;
      gcur_page = page;
      $reading_area.addClass(SHOW_IFRAME);
      var me = "feed-" + feedid,
          $me = $('#' + me);
      $logo.removeClass(SHOW_NAV);
      layout.select('#feed-list', me);
      var feed = data.get_feed(feedid),
          link = feed.link;
      feed.domain = util.hostname(link);
      set_document_title(feed.title);
      $footer.empty().append(tmpls.footer_info(feed));
      var $loader = $footer.find('> img');
      iframe.src = util.get_final_link(link, feedid);
      var mark_read = mark_feed_as_read($me, feedid, subid);
      clear_timer();
      gmark_as_read_timer_id = window.setTimeout(mark_read, 500);
      iframe.onload = function () {
        mark_read();
        $loader.css({visibility: 'hidden'});
      };
    };
    if(gcur_sub_id === subid) {
      read();                   // just read feed
    } else {
      if(_.isNumber(subid)) {
        read_subscription(subid, page, sort, read);
      } else {
        show_welcome(subid, page, read);
      }
    }
  }

  function mark_feed_as_read ($me, feedid, subid) {
    var called = false;
    return function () {
      if(!called && !$me.hasClass('read')) {
        called = true;
        data.mark_as_read(feedid);
        decrement_number($me, subid);
        $me.removeClass('unread sys-read').addClass('read');
      }
    };
  }

  function show_feeds (data) {
    show_server_message();
    iframe.src = 'about:blank';
    var html = tmpls.feeds_nav(data);
    $navigation.empty().append(html);
    html = tmpls.sub_feeds(data);
    $welcome_list.empty().append(html).trigger('child_change.rm');
    set_document_title(data.title);
    $reading_area.removeClass(SHOW_IFRAME);
    $logo.addClass(SHOW_NAV);
  }

  function show_welcome (section, page, cb) {
    var d = !section && !page;
    section = section || 'recommand';
    page = page || 1;
    if(data.get_subscriptions().length) { // user has subscriptions
      data.get_welcome_list(section, page, function (data) {
        if(!data.feeds.length && d) {
          // try to show something that has data
          location.hash = '?s=latest&p=1';
        } else {
          show_feeds(data, section);
          call_if_fn(cb);
        }
      });
    } else {
      location.hash = "s/add";
      show_server_message();
    }
  }

  function show_settings (section) {
    $reading_area.removeClass(SHOW_IFRAME);
    var sections = ['add', 'account', 'about'];
    var d = {
      selected: section,
      tabs: _.map(sections, function (s) {
        return { n: s, s: s === section };
      })
    };
    var html = tmpls.settings(d);
    $welcome_list.empty().append(html);
  }

  function save_settings (e) {
    var d = util.extract_data( $('#all-settings .account') );
    for(var i in d) { if(!d[i]) { delete d[i]; } }
    d.expire = parseInt(d.expire, 10);
    if(d.password && d.password !== d.password2) {
      alert('password not match');
      return;
    }
    delete d.password2;
    data.save_settings(d, function () {
      notify.show_msg('Settings saved', 3000);
    });
  }

  function still_in_settings () {
    return location.hash === "#s/add";
  }

  function fetcher_finished (result) {
    if(!result) { return ; }
    if(result.refresh) {
      fetch_and_show_user_subs(function () {
        // if user is waiting, just put he there
        if(still_in_settings()) {
          location.hash = 'read/' + result.id + '?p=1&s=newest';
        }
      });
    } else if(still_in_settings()){
      location.hash = 'read/' + result.id + '?p=1&s=newest';
    }
  }

  function add_subscription (e) {
    var $input = $("#rss_atom_url"),
        url = $.trim($input.val()),
        added = function () {
          $input.val('');
          notify.show_msg('subscription added successfully', 400);
          window.setTimeout(function () {
            // if user is waiting, just put he there
            if(still_in_settings()) {
              notify.show_msg('working hard to fetch the feed...', 10000);
            }
          }, 1500);
        };
    if(url && url.indexOf('http://') === 0) {
      data.add_subscription(url, added, fetcher_finished);
    } else {
      notify.show_msg('Not valid rss/atom link', 3000);
    }
  }

  function fetch_and_show_user_subs (cb) {
    data.get_user_subs(function (subs) {
      var html = tmpls.subs_nav({groups: subs});
      $subs_list.empty().append(html).find('img').each(util.favicon_error);
      $subs_list.trigger('refresh.rm');
      util.call_if_fn(cb);
    });
  }

  function show_server_message () {
    if(_RM_.gw) {               // google import wait
      var msg = 'Busy importing from google reader, refresh in a few seconds';
      notify.show_msg(msg, 10000);
      _RM_.gw = 0;
    }
    if(_RM_.ge) {
      notify.show_msg('Error import from google reader : ' + _RM_.ge, 4000);
      _RM_.ge = 0;
    }
  }

  util.delegate_events($(document), {
    'click #add-subscription': add_subscription,
    'click #save-settings': save_settings,
    'click #nav-pager .next': load_next_page,
    'click #nav-pager .prev': load_prev_page,
    'mouseenter #logo': function () { $logo.addClass(SHOW_NAV); },
    'mouseleave #logo': function () {
      if(/#read\/.+\/\d+/.test(location.hash)) { // if reading feed
        $logo.removeClass(SHOW_NAV);
      }
    }
  });

  fetch_and_show_user_subs(function () { // app start here
    RM.hashRouter({
      '': show_welcome,
      '?s=:section&p=:p': show_welcome,
      's/:section': show_settings,
      'read/:id?p=:page&s=:sort': read_subscription,
      'read/:id/:id?p=:page&s=:sort': read_feed
    });
  });

  // show for ie first
  if(/msie/i.test(navigator.userAgent)) {
    $('#warn-msg, #warn-msg .support').show();
  } else if(_RM_.demo) {
    $('#warn-msg, #warn-msg .demo').show();
  }
})();
