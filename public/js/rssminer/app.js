(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      layout = RM.layout,
      call_if_fn = util.call_if_fn;

  var SHOW_NAV = 'show-nav',
      SHOW_IFRAME = 'show-iframe';

  var mark_as_read_timer_id = 0;

  var $loader = $('#footer img'),
      $reading_area = $('#reading-area'),
      $navigation = $('#navigation'),
      $subs_list = $('#sub-list'),
      iframe = $('iframe')[0],
      $logo = $('#logo'),
      $welcome_list = $('.welcome-list'),
      $feeds_list = $('#feed-list');

  function focus_first_feed () {
    $($('.welcome-list li.feed')[0]).addClass('selected');
    $reading_area.removeClass(SHOW_IFRAME);
  }

  function switch_nav_to_subs () {
    $logo.addClass(SHOW_NAV);
  }

  function switch_nav_to_feeds (cb) {
    $logo.removeClass(SHOW_NAV);
    call_if_fn(cb);
  }

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
    if(typeof callback !== 'function') {
      switch_nav_to_subs();
    }
    layout.select('#sub-list', "item-" + id);
    data.get_feeds(id, page, sort, function (data) {
      data.title = sub.title;
      set_document_title(data.title);
      if(data.feeds.length) {
        var html = tmpls.feeds_nav(data);
        $feeds_list.empty().append(html);
        html = tmpls.sub_feeds(data);
        $welcome_list.empty().append(html);
        focus_first_feed();
      }
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
    read_subscription(subid, 1, 'newest', function () {
      $reading_area.addClass(SHOW_IFRAME);
      var me = "feed-" + feedid,
          $me = $('#' + me);
      switch_nav_to_feeds(function () {
        layout.select('#feed-list', me);
      });
      var feed = data.get_feed(feedid),
          link = feed.link;
      feed.domain = util.hostname(link);
      set_document_title(feed.title);
      var html = tmpls.footer_info(feed);
      $('#footer .feed').replaceWith(html);
      $loader.css({visibility: 'visible'});
      iframe.src = data.get_final_link(link, feedid);
      var mark = mark_as_read($me, feedid, subid);
      clear_timer();
      mark_as_read_timer_id = window.setTimeout(mark, 500);
      iframe.onload = function () {
        mark();
        $loader.css({visibility: 'hidden'});
      };
    });
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

  function show_welcome () {
    if(data.is_user_has_subscription()) { // user has subscriptions
      data.get_welcome_list(function (data) {
        $welcome_list.empty().append(tmpls.welcome(data));
        $reading_area.removeClass(SHOW_IFRAME);
        switch_nav_to_subs();
        set_document_title();
      });
    } else {
      location.hash = "add";
    }
  }

  function save_user_vote (vote, ele) {
    var $feed = $(ele).closest('.feed'),
        id = $feed.attr('data-id');
    if(!$feed.hasClass('sys')) {
      if(($feed.hasClass('dislike') && vote === -1)
         || ($feed.hasClass('like') && vote === 1)) {
        vote = 0;                 // reset
      }
    }
    if(id) {
      id = parseInt(id, 10);
      data.save_vote(id, vote, function () {
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

  function show_settings () {
    $reading_area.removeClass(SHOW_IFRAME);
    var html = tmpls.settings(data.user_settings());
    $welcome_list.empty().append(html).find('img').each(util.favicon_error);
  }

  function save_settings (e) {
    var d = util.extractData( $('#settings') );
    for(var i in d) { if(!d[i]) { delete d[i]; } }
    d.expire = parseInt(d.expire, 10);
    if(d.password && d.password !== d.password2) {
      alert('2 password not match');
      return;
    }
    delete d.password2;
    data.save_settings(d, function () { location = "/a"; });
  }

  function add_subscription (e) {
    var $input = $("#rss_atom_url"),
        url = $.trim($input.val()),
        fetcher_finished = function (result) {
          if(result) {
            fetch_and_show_user_subs(function () {
              // if user is waiting, just put he there
              if(location.hash === '#add') {
                location.hash = 'read/' + result.id;
              }
            });
          }
        },
        added = function () { $input.val(''); };
    if(url) {
      data.add_subscription(url, added, fetcher_finished);
    }
  }

  function show_add_sub_ui () {
    $reading_area.removeClass(SHOW_IFRAME);
    $welcome_list.empty().append(tmpls.add());
  }

  function save_vote_up (e) { save_user_vote(1, this); return false; }
  function save_vote_down (e) { save_user_vote(-1, this); return false; }

  function update_subs_sort_order (event, ui) {
    if(ui.sender) { // prevent be callded twice if move bettween categories
      return;
    }
    var $moved = $(ui.item),
        $before = $moved.prev(),
        moved_id = parseInt($moved.attr('data-id')),
        new_cat = $moved.closest('.rss-category').siblings('.folder').attr('data-name'),
        new_before_id = $before.length ? parseInt($before.attr('data-id')) : null;
    data.update_sort_order(moved_id, new_before_id, new_cat);
  }

  function update_category_sort_order () {

  }

  function toggle_nav_foler (e) {
    $(this).closest('li').toggleClass('collapse');
    var collapsed = [];
    $('#navigation li.collapse .folder').each(function (index, item) {
      collapsed.push($(item).attr('data-name'));
    });
    RM.ajax.spost('/api/settings', {nav: collapsed});
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

  util.delegate_events($(document), {
    'click #add-subscription': add_subscription,
    'click #sub-list .folder span': toggle_nav_foler,
    'click #save-settings': save_settings,
    'click .vote span.down': save_vote_down,
    'click .vote span.up': save_vote_up,
    'click #settings .delete': unsubscribe
  });

  function fetch_and_show_user_subs (cb) {
    data.get_user_subs(function (subs) {
      var html = tmpls.subs_nav({groups: subs});
      // $('#logo ul').append(html);
      $subs_list.empty().append(html).find('img').each(util.favicon_error);
      // $("#navigation .item img").each(util.favicon_error);
      // category sortable
      $subs_list.sortable({change: update_category_sort_order });
      $(".rss-category").sortable({ // subscription sortable with categories
        connectWith: ".rss-category",
        update: update_subs_sort_order
      });
      util.call_if_fn(cb);
    });
  }


  fetch_and_show_user_subs(function () { // app start here

    $logo.mouseenter(function () {
      $logo.addClass(SHOW_NAV);
    }).mouseleave(function () {
      // if reading feed
      if(/#read\/\d+\/\d+/.test(location.hash)) {
        $logo.removeClass(SHOW_NAV);
      }
    });

    RM.hashRouter({
      '': show_welcome,
      'settings': show_settings,
      'add': show_add_sub_ui,
      'read/:id?p=:page&s=:sort': read_subscription,
      'read/:id?p=:page': read_subscription,
      'read/:id/:id': read_feed,
      'read/:id': read_subscription
    });
  });
})();
