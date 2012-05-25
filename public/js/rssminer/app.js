(function () {
  var RM = window.RM,
      data = RM.data,
      notify = RM.notify,
      tmpls = RM.tmpls,
      util = RM.util,
      layout = RM.layout,
      location = window.location,
      call_if_fn = util.call_if_fn;

  var SHOW_NAV = 'show-nav',
      SHOW_IFRAME = 'show-iframe';

  var mark_as_read_timer_id = 0,
      current_sub_id;

  var $loader = $('#footer img'),
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
    page = page || 1;
    sort = sort || 'newest';
    $reading_area.removeClass(SHOW_IFRAME);
    var sub = data.get_subscription(id);
    layout.select('#sub-list', "item-" + id);
    data.get_feeds(id, page, sort, function (data) {
      data.title = sub.title;
      show_feeds(data, sub.title, sub.url);
      call_if_fn(callback);
    });
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
    if(mark_as_read_timer_id) {
      window.clearTimeout(mark_as_read_timer_id);
      mark_as_read_timer_id = null;
    }
  }

  function read_feed (subid, feedid) {
    var read = function () {
      current_sub_id = subid;
      $reading_area.addClass(SHOW_IFRAME);
      var me = "feed-" + feedid,
          $me = $('#' + me);
      $logo.removeClass(SHOW_NAV);
      layout.select('#feed-list', me);
      var feed = data.get_feed(feedid),
          link = feed.link;
      feed.domain = util.hostname(link);
      set_document_title(feed.title);
      var html = tmpls.footer_info(feed);
      $('#footer .feed').replaceWith(html);
      $loader.css({visibility: 'visible'});
      iframe.src = util.get_final_link(link, feedid);
      var mark = mark_as_read($me, feedid, subid);
      clear_timer();
      mark_as_read_timer_id = window.setTimeout(mark, 500);
      iframe.onload = function () {
        mark();
        $loader.css({visibility: 'hidden'});
      };
    };
    if(current_sub_id === subid) {
      read();                   // just read feed
    } else {
      if(_.isNumber(subid)) {
        read_subscription(subid, 1, 'newest', read);
      } else {
        show_welcome(subid, 1, read);
      }
    }
  }

  function mark_as_read ($me, feedid, subid) {
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

  function show_feeds (data, title, url) {
    data.title = title;
    data.url = url;
    iframe.src = 'about:blank';
    var html = tmpls.feeds_nav(data);
    $navigation.empty().append(html);
    html = tmpls.sub_feeds(data);
    $welcome_list.empty().append(html).trigger('child_change.rm');
    set_document_title(title);
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
      location.hash = "settings";
    }
  }

  function save_user_vote (vote, $feed) {
    var id = $feed.attr('data-id');
    if(!$feed.hasClass('sys')) {
      if(($feed.hasClass('dislike') && vote === -1)
         || ($feed.hasClass('like') && vote === 1)) {
        vote = 0;                 // reset
      }
    }
    if(id) {
      id = parseInt(id, 10);
      data.save_vote(id, vote, function () {
        notify.show_msg('Saved', 1000);
        if(vote === 1) {
          $feed.addClass('like').removeClass('dislike neutral sys');
        } else if(vote === -1) {
          $feed.addClass('dislike').removeClass('like neutral sys');
        } else if(vote === 0) {
          $feed.addClass('neutral sys').removeClass('like dislike');
        }
      });
    }
  }

  function show_settings (section) {
    $reading_area.removeClass(SHOW_IFRAME);
    var html = tmpls.settings(data.user_settings());
    $welcome_list.empty().append(html).find('img').each(util.favicon_error);
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
    return location.hash === '#settings';
  }

  function fetcher_finished (result) {
    if(!result) { return ; }
    if(result.refresh) {
      fetch_and_show_user_subs(function () {
        // if user is waiting, just put he there
        if(still_in_settings()) {
          location.hash = 'read/' + result.id;
        }
      });
    } else if(still_in_settings()){
      location.hash = 'read/' + result.id;
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

  function save_vote_up (e) {
    save_user_vote(1, $(this).closest('.feed'));
    return false;
  }

  function save_vote_down (e) {
    save_user_vote(-1, $(this).closest('.feed'));
    return false;
  }

  function unsubscribe () {
    var $tr = $(this).closest('tr'),
        id = $tr.attr('data-id');
    if(id) {
      id = parseInt(id, 10);
      var sub = data.get_subscription(id);
      if(confirm('unsubscribe "' + sub.title + '"')) {
        data.unsubscribe(id, function () {
          $tr.remove();
          $('#item-' + id).remove();
        });
      }
    }
  }

  function switch_settings_tab () {
    var $this = $(this),
        text = $.trim($this.text());
    $('.settings-sort li').removeClass('selected');
    $this.addClass('selected');
    $('#all-settings').removeClass().addClass('show-' + text);
  }

  function fetch_and_show_user_subs (cb) {
    data.get_user_subs(function (subs) {
      var html = tmpls.subs_nav({groups: subs});
      $subs_list.empty().append(html).find('img').each(util.favicon_error);
      $subs_list.trigger('refresh.rm');
      util.call_if_fn(cb);
    });
  }

  window.RM = _.extend(window.RM, {
    app: {save_user_vote: save_user_vote}
  });

  util.delegate_events($(document), {
    'click #add-subscription': add_subscription,
    'click #save-settings': save_settings,
    'click .settings-sort li': switch_settings_tab,
    'click #add-sub a': function () {
      alert('Only avaiable when published, please wait a while');
      return false;
    },
    'click .vote span.down': save_vote_down,
    'click .vote span.up': save_vote_up,
    'click #all-settings .delete': unsubscribe
  });

  fetch_and_show_user_subs(function () { // app start here
    $logo.mouseenter(function () {
      $logo.addClass(SHOW_NAV);
    }).mouseleave(function () {
      // if reading feed
      if(/#read\/.+\/\d+/.test(location.hash)) {
        $logo.removeClass(SHOW_NAV);
      }
    });

    RM.hashRouter({
      '': show_welcome,
      '?s=:section&p=:p': show_welcome,
      'settings': show_settings,
      'settings/:section': show_settings,
      'read/:id?p=:page&s=:sort': read_subscription,
      'read/:id?p=:page': read_subscription,
      'read/:id/:id': read_feed,
      'read/:id': read_subscription
    });
  });
})();
