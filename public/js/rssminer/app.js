(function () {
  "use strict";
  var _RM_ = window._RM_ || {},
      RM = window.RM,
      router = RM.Router,
      data_api = RM.data,
      notify = RM.notify,
      tmpls = RM.tmpls,
      util = RM.util,
      to_html = util.to_html,
      layout = RM.layout,
      location = window.location,
      call_if_fn = util.call_if_fn;

  var SHOW_NAV = 'show-nav',
      MIN_TIME = 400,           // at least 400ms, then record
      SAVE_THREASH_HOLD = 3,
      DATA_ID = 'data-id',
      SUMMARY_SELECTOR = '#s-',
      FEED_SELECTOR = '#feed-',
      READING_CLS = 'reading',
      D_READING_CLS = '.reading',
      NEWEST_TAB = 'newest',
      READ_URL_PATTEN = 'read/:id/:id?p=:page&s=:sort',
      SHOW_CONTENT = 'show-content';

  var $reading_area = $('#reading-area'),
      $navigation = $('#navigation'), // feed list
      $subs_list = $('#sub-list'),    // sub list
      $feed_content = $('#feed-content'),
      $logo = $('#logo'),
      $feedback = $('#feedback-form'),
      $welcome_list = $('#welcome-list');

  var gcur_page,
      gcur_sort,
      gcur_group,
      gcur_subid,
      greading_meta = {},
      greading_times = {},
      GROUP_FOLDER = 'GROUP',
      GROUP_WELCOME = 'ALL',
      GROUP_SUB = 'SUB';

  function _update_state (subid, page, sort, group) {
    $welcome_list.scrollTop(0);
    $feed_content.empty(); // save memory, when reading list
    $reading_area.scrollTop(0);
    gcur_page = page;
    gcur_sort = sort;
    gcur_group = group;
    gcur_subid = subid;
  }

  function set_document_title (title) {
    var rssminer = 'Rssminer, yet another RSS reader';
    if(title) {
      if(title.toLowerCase().indexOf('rssminer') !== -1) {
        document.title = title;
      } else {
        document.title = title + ' - ' + rssminer;
      }
    } else {
      document.title = rssminer;
    }
  }

  function read_subscription (id, page, sort, callback) {
    _update_state(id, page, sort, GROUP_SUB);
    $reading_area.removeClass(SHOW_CONTENT);
    layout.select('#sub-list', $("#item-" + id));
    data_api.fetch_sub_feeds(id, page, sort, function (data) {
      show_feeds(data);
      call_if_fn(callback);
    });
  }

  function read_group_subs (group, page, sort, callback) {
    _update_state(group, page, sort, GROUP_FOLDER);
    layout.select('#sub-list', $("[data-name='" + group + "']"));
    data_api.fetch_group_feeds(group, page, sort, function (data) {
      show_feeds(data);
      call_if_fn(callback);
    });
  }

  function decrement_number ($just_read, subid) {
    var $c = $("#item-" + subid + ' .c'),
        c = + /\d+/.exec($c.text())[0] - 1;
    if(!c) {
      $c.parents('.unread').removeClass('unread');
      $c.remove();
    } else {
      $c.text('(' + c + ')');
    }
  }

  function record_reading_time (url_pattern, args) {
    if(greading_meta.read) {
      var time = new Date() - greading_meta.start;
      if(time > MIN_TIME) {
        time = parseInt(Math.floor(time/100)); // save store it in 0.1s
        greading_times[greading_meta.id] = time;
        if(_.keys(greading_times).length >= SAVE_THREASH_HOLD) {
          var data = greading_times;
          greading_times = {};  // empty it
          data_api.save_reading_times(data);
        }
      }
    }
    if(url_pattern !== READ_URL_PATTEN) {
      greading_meta.read = false;
    }
  }

  function fetch_and_apppend (selected_id, clean, cb) {
    var ids = get_fetch_ids(selected_id);
    if(!clean) {                // filter out already added items
      ids = _.filter(ids, function (id) {
        return !$(SUMMARY_SELECTOR + id).length;
      });
    }
    if(ids.length) {
      data_api.fetch_summary(ids, function (feeds) {
        var html = to_html(tmpls.feed_content, {feeds: feeds});
        if(clean) {
          $reading_area.scrollTop(0);
          $feed_content.empty();
        }
        $feed_content.append(html);
        _.defer(cleanup);
        call_if_fn(cb);
      });
    }
  }

  function cleanup () {
    $feed_content.find('.summary a').attr('target', '_blank');

    _.each(["p", 'pre'], function (selector) {
      $feed_content.find(selector).each(function (idx, p) {
        var $p = $(p);
        // only remove if no chillren and no text. 516264
        if(!$.trim($p.text()) && !$p.find('img').length) {
          $p.hide();            // 4037/330457
        }
      });
    });

    try {
      var brs = $feed_content[0].querySelectorAll('br');
      for(var i = 0; i < brs.length; i++) {
        var br = brs[i],
            p = br.previousSibling;
        if(p && p.previousSibling && p.previousSibling.nodeName === 'BR') {
          br.parentElement.removeChild(br);
        }
      }
    }catch(e) {}

  }

  function reading_feed (id) {
    var $me = $(FEED_SELECTOR+id);
    router.navigate($me.find('a').attr('href'));
    set_document_title($me.find('.feed h2').text());
    $reading_area.addClass(SHOW_CONTENT);
    layout.select('#feed-list', $me); // layout
    if(!$me.hasClass('read')) {
      data_api.mark_as_read(id);
      data_api.fetch_summary([id], function (feeds) {
        if(feeds.length) {
          decrement_number($me, feeds[0].sub.id);
        }
      });
      $me.removeClass('unread sys-read').addClass('read');
    }

    $(D_READING_CLS).removeClass(READING_CLS);
    $('#s-'+id).addClass(READING_CLS);
  }

  function read_feed (subid, feedid, page, sort, folder) {
    var is_group = _.isString(subid) && subid.indexOf('f_') === 0;
    if(is_group) {
      subid = subid.substring(2);
    }
    var read_cb = function () {
      $logo.removeClass(SHOW_NAV);
      fetch_and_apppend(feedid, true, function () {
        reading_feed(feedid);
      });
    };
    if(gcur_subid === subid) {
      read_cb();                   // just read feed
    } else {
      if(_.isNumber(subid)) {
        read_subscription(subid, page, sort, read_cb);
      } else if(is_group) {
        read_group_subs(subid, page, sort, read_cb);
      } else {
        show_welcome(subid, page, read_cb);
      }
    }
  }

  function mark_feed_as_read ($me, feedid, subid) {
    if(!$me.hasClass('read')) {
      data_api.mark_as_read(feedid);
      decrement_number($me, subid);
      $me.removeClass('unread sys-read').addClass('read');
    }
  }

  function show_feeds (data) {
    gcur_has_more = data.pager.has_more;
    show_server_message();
    var html = to_html(tmpls.feeds_nav, data);
    $navigation.empty().append(html);
    html = to_html(tmpls.sub_feeds, data);
    $welcome_list.empty().append(html);
    set_document_title(data.title);
    $reading_area.removeClass(SHOW_CONTENT);
    $logo.addClass(SHOW_NAV);
  }

  function show_welcome (section, page, cb) {
    var d = !section && !page;
    section = section || data_api.user_conf.pref_sort || NEWEST_TAB;
    page = page || 1;
    _update_state(section, page, section, GROUP_WELCOME);
    $subs_list.find('.selected').removeClass('selected');
    if(data_api.get_subscriptions().length) { // user has subscriptions
      data_api.fetch_welcome(section, page, section, function (data) {
        if(!data.feeds.length && d) {
          // try to show something that has data
          location.hash = '?s=newest&p=1';
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
    $reading_area.removeClass(SHOW_CONTENT);
    var old_sort = data_api.user_conf.pref_sort;
    var sections = ['add', 'settings'],
        sortings = [{value: NEWEST_TAB,
                     text: _LANG_(NEWEST_TAB),
                     s: old_sort === NEWEST_TAB},
                    {value: 'recommend', // default to recommend
                     text: _LANG_('recommend'),
                     s: old_sort === 'recommend'}];
    var d = {
      title: _LANG_(section),
      selected: section,
      sortings: sortings,
      tabs: _.map(sections, function (s) {
        return { text: _LANG_(s),  n: s, s: s === section };
      })
    };
    d.demo = _RM_.demo;
    var html = to_html(tmpls.settings, d);
    $welcome_list.empty().append(html);
  }

  function save_settings (e) {
    var d = util.extract_data( $('#all-settings .settings') );
    for(var i in d) { if(!d[i]) { delete d[i]; } }
    if(d.password && d.password !== d.password2) {
      alert('password not match');
      return;
    }
    delete d.password2;
    data_api.save_settings(d, function () {
      notify.show_msg('Settings saved', 3000);
    });
  }

  function still_in_settings () {
    return location.hash === "#s/add";
  }

  function fetcher_finished (result) {
    if(!result) {
      notify.show_msg('Sorry, Fetcher is too busy.. job queued.', 10000);
      return ;
    }
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

  function import_from_greader (e) {
    if(_RM_.demo) {
      alert('If you like it, please login in with Google OpenID' +
            ', then import. This is a public account');
      return false;
    }
  }

  function add_subscription (e) {
    var $input = $("#rss_atom_url"),
        url = $.trim($input.val()),
        added = function (result) {
          if(!result.rss_link_id) {
            notify.show_msg('Not a valid rss/atom link', 3000);
            return;
          }
          $input.val('');
          notify.show_msg('Subscription added successfully', 400);
          window.setTimeout(function () {
            // if user is waiting, just put he there
            if(still_in_settings()) {
              notify.show_msg('Working hard to fetch the feeds...', 10000);
            }
          }, 1500);
        };

    if(url && url.indexOf('http://') === 0) {
      if(_RM_.demo) {
        alert('This is a demo account');
        return;
      }
      data_api.add_subscription(url, added, fetcher_finished);
    } else {
      notify.show_msg('Not a valid rss/atom link', 3000);
    }
  }

  function fetch_and_show_user_subs (cb) {
    data_api.get_user_subs(function (subs) {
      var html = to_html(tmpls.subs_nav, {groups: subs});
      $subs_list.empty().append(html).find('img').each(util.favicon_ok);
      $subs_list.trigger('refresh.rm');
      util.call_if_fn(cb);
    });
  }

  var is_loading = false;
  var gcur_has_more = true;
  function on_navigation_scroll(e) {     // feed list scroll, auto load
    if(!gcur_has_more) { $('#navigation .loader').remove(); return; }
    if(is_loading) { return; }
    var total_height = $navigation[0].scrollHeight, // ie8, ff, chrome
        scrollTop = $navigation.scrollTop(),
        height = $navigation.height();
    if(scrollTop + height >= total_height - 30) {
      var fn = data_api.fetch_sub_feeds;
      if( gcur_group === GROUP_FOLDER) {
        fn = data_api.fetch_group_feeds;
      } else if (gcur_group === GROUP_WELCOME ) {
        fn = data_api.fetch_welcome;
      }
      gcur_page += 1;
      is_loading = true;
      fn(gcur_subid, gcur_page, gcur_sort, function (data) {
        is_loading = false;
        gcur_has_more = data.pager && data.pager.has_more;
        if(!gcur_has_more) {
          $('#navigation .loader').remove();
        } else {
          var html = to_html(tmpls.feeds_list, data);
          $('#feed-list').append(html);
        }
      });
    }
  };

  function on_readarea_scroll(e) {
    if(!$reading_area.hasClass(SHOW_CONTENT)) { return; }
    var current_scroll = $reading_area.scrollTop(),
        $reading = $reading_area.find(D_READING_CLS);

    var height = $reading_area.height() / 3; // top 30%
    var $articles = _.map($('#feed-content > li'), function (a) { return $(a); });

    var $n = _.find($articles, function ($a) {
      var o = $a.offset(), top = o.top, bottom = o.top + $a.height();
      return top < height && bottom > height;
    });

    if($n) {
      var feed_id = $n.attr(DATA_ID);
      if(!$n.hasClass(READING_CLS)) { reading_feed(feed_id); }

      // console.log($feed_content.height() - current_scroll, $reading_area.height() * 2);
      // keep one more screen
      if($feed_content.height() - current_scroll < $reading_area.height() * 2) {
        fetch_and_apppend(feed_id);
      }
    }
  };

  function get_fetch_ids (current_id) {
    var $me = $(FEED_SELECTOR + current_id),
        n = 5;
    var ids = [current_id];
    var $next = $me.next();
    while($next.length && --n >= 0) {
      ids.push($next.attr(DATA_ID));
      $next = $next.next();
    }
    return ids;
  }

  function show_server_message () { // only show once
    if(_RM_.gw) {               // google import wait
      var msg = 'Busy importing from google reader, please refresh in a few seconds';
      notify.show_msg(msg, 10000);
      _RM_.gw = 0;
    }
    if(_RM_.ge) {
      var msg2 = 'Error: ' + _RM_.ge + ' , please try again';
      notify.show_msg(msg2, 6000);
      _RM_.ge = 0;
    }
  }

  function save_pref_sort (e) {
    var val = $(this).val();
    data_api.save_settings({pref_sort: val}, function () {
      fetch_and_show_user_subs();
      notify.show_msg('Settings saved', 3000);
    });
  }

  function close_shortcut_help () {
    $('#shortcuts').fadeOut();
    $('#overlay').hide();
  }

  function show_shortcut_help () {
    $('#shortcuts').fadeIn();
    $('#overlay').show();
    return false;
  }

  function show_feedback_form () {
    $feedback.show().find('textarea').focus();
    return false;
  }

  function submit_feedback () {
    var data = {email: _RM_.user.email};
    if(_RM_.demo) {
      data.email = $.trim($feedback.find('input').val());
    }
    var feedback = $.trim($feedback.find('textarea').val());
    if(feedback) {
      data.feedback = feedback;
      data.refer = location.pathname + location.search + location.hash;
      $.post('/api/feedback', data, function () {
        notify.show_msg("Thanks, Let's make it better");
        $feedback.hide();
      });
    }
  }

  function search (q, tags, authors, offset) {
    var fs = !$('#search-result').length || offset === 0;
    if(q) {
      data_api.fetch_search(q, tags, authors, offset, fs, function (data) {
        data.q = q;
        var $html = $(to_html(tmpls.search_result, data));
        $reading_area.removeClass(SHOW_CONTENT);
        if(fs) {
          $welcome_list.empty().append($html);
        } else {
          $('#search-result .feeds').replaceWith($('.feeds', $html));
          $('#search-result .pager').replaceWith($('.pager', $html));
        }
      });
    } else {
      $reading_area.removeClass(SHOW_CONTENT);
      $welcome_list.empty().append(to_html(tmpls.search_result, {}));
    }
  }

  function update_search_hash (e) {
    var val = $.trim($(this).val());
    var $link = $('#search-go').find('a');
    // var hash = 'search?q=&tags=&authors=&offset=0';
    var hash = location.hash;
    hash = hash.replace(/q=.*?&/, function (a, b, c) {
      return "q=" + val + '&';
    });
    $link.attr('href', hash);
    if(e.which === 13) {
      location.hash = hash;
    }
  }

  util.delegate_events($(document), {
    'click .add-sub a.import': import_from_greader,
    'change #all-settings select': save_pref_sort,
    'keyup #search-go input': update_search_hash,
    'click #add-subscription': add_subscription,
    'click .show-shortcuts': show_shortcut_help,
    'click .show-feedback': show_feedback_form,
    'click #save-settings': save_settings,
    'click #feedback-form .close': function () { $feedback.hide(); },
    'click #feedback-form button': submit_feedback,
    'click #overlay': close_shortcut_help,
    'click .icon-ok-circle': close_shortcut_help,
    'mouseenter #logo': function () { $logo.addClass(SHOW_NAV); },
    'mouseleave #logo': function () {
      if(/#read\/.+\/\d+/.test(location.hash)) { // if reading feed
        _.delay(function () { $logo.removeClass(SHOW_NAV); }, 50);
      }
    }
  });

  // app start here
  fetch_and_show_user_subs();   // show quickly
  data_api.fetch_unread_count(function () {
    // async show unread numbers
    var id = $('.item.selected').attr(DATA_ID);
    fetch_and_show_user_subs();
    if(id) { $('#item-'+id).addClass('selected'); };
  });

  router.route({
    '': show_welcome,
    '?s=:section&p=:p': show_welcome,
    's/:section': show_settings,
    'read/f_:group?p=:page&s=:sort': read_group_subs,
    'read/:group/:id?p=:page&s=:sort': read_feed,
    'read/:id?p=:page&s=:sort': read_subscription,
    'read/:id/:id?p=:page&s=:sort': read_feed,
    'search?q=:q&tags=:tags&authors=:authors&offset=:offset': search
  });

  $reading_area.scroll(_.debounce(on_readarea_scroll, 40));
  $navigation.scroll(on_navigation_scroll);
})();
