(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      layout = RM.layout,
      to_html = Mustache.to_html;

  var ANIMATION_TIME = 250;

  var current_subid,
      current_feeds_cnt = 0,
      current_nav_subs = true;

  var $loader = $('#footer img'),
      $reading_area = $('#reading-area'),
      $navigation = $('#navigation'),
      $footer_title = $('#footer a'),
      $footer_domain = $('#footer .domain'),
      $subs_list = $('.sub-list'),
      $feeds_list = $('#feed-list');

  function focus_first_feed () {
    $($('.welcome-list li.feed')[0]).addClass('selected');
    $reading_area.removeClass('show-iframe');
  }

  function set_welcome_title (title) {
    var $welcome = $('.welcome-list').empty();
    if(title) { $welcome.append('<h2>' + title +'</h2>'); }
    return $welcome;
  }

  function toggle_nav () {
    if(current_nav_subs) { switch_nav_to_feeds(); }
    else { switch_nav_to_subs(); }
  }

  function switch_nav_to_subs () {
    if(!current_nav_subs) {
      current_nav_subs = true;
      $feeds_list.animate({height: 0, opacity: 0}, ANIMATION_TIME, function () {
        $feeds_list.hide().css({height: 'auto', opacity: 1});
        $subs_list.show();
      });
    }
  }

  function switch_nav_to_feeds (cb) {
    if(current_nav_subs && current_feeds_cnt) {
      current_nav_subs = false;
      $subs_list.animate({height: 0, opacity: 0}, ANIMATION_TIME, function () {
        $subs_list.hide().css({ opacity: 1, height: 'auto' });
        $feeds_list.show();
        if(typeof cb === 'function') { cb(); }
      });
    } else if(typeof cb === 'function') {
      cb();
    }
  }

  function read_subscription (id, callback) {
    current_subid = id;
    hide_help();
    $reading_area.removeClass('show-iframe');
    var sub = data.get_subscription(id),
        title = sub.title;
    if(typeof callback !== 'function') {
      switch_nav_to_subs();
    }
    if(layout.select('.sub-list', "item-" + id)) {
      data.get_feeds(id, 0, 40, 'time', function (data) {
        current_feeds_cnt = data.length;
        if(data.length) {
          var html = to_html(tmpls.list, {feeds: data});
          $feeds_list.empty().append(html);
          var $welcome = set_welcome_title(title);
          $welcome.append(to_html(tmpls.welcome_section, {list: data }));
          focus_first_feed();
        }
        if(typeof callback === 'function') { callback(); }
      });
    } else if (typeof callback === 'function') {
      callback();
    }
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

  function read_feed (subid, feedid) {
    read_subscription(subid, function () {
      $reading_area.addClass('show-iframe');
      var me = "feed-" + feedid,
          $me = $('#' + me);
      switch_nav_to_feeds(function () {
        layout.select('#feed-list', me);
      });
      var feed = data.get_feed(subid, feedid),
          title = feed.title,
          link = feed.link;
      $footer_title.text(title).attr('href', link);
      $footer_domain.text(util.hostname(link));
      var iframe = $('iframe')[0];
      $loader.css({visibility: 'visible'});
      iframe.src = data.get_final_link(link, feedid);
      iframe.onload = function () {
        mark_as_read($me, feedid, subid);
        $loader.css({visibility: 'hidden'});
      };
    });
  }

  function mark_as_read ($me, feedid, subid) {
    if(!$me.hasClass('read')) {
      data.mark_as_read(feedid, subid);
      decrement_number($me, subid);
      $me.removeClass('unread sys-read').addClass('read');
    }
  }

  function show_welcome () {
    if(data.is_user_has_subscription()) { // user has subscriptions
      var $welcome = set_welcome_title('Rssminer - an intelligent RSS reader');
      data.get_welcome_list(function (data) {
        for(var section in data) {
          var d = data[section];
          if(d.list.length) {
            $welcome.append(to_html(tmpls.welcome_section, d));
            switch_nav_to_subs();
            focus_first_feed();
          }
        }
      });
    } else {
      location.hash = "add";
    }
  }

  function save_vote (vote, ele) {
    var $feed;
    //  1. select it's parent if ele is defined;
    if(ele) { $feed = $(ele).closest('li.feed'); }
    // guess target feed
    if(!$feed || !$feed.length) {
      if($('#reading-area').hasClass('show-iframe')) {
        $feed = $('#feed-list .selected');
      } else {
        $feed = $('.welcome-list .selected');
      }
    }
    var id = $feed.attr('data-id');
    if(!$feed.hasClass('sys')) {
      if(($feed.hasClass('dislike') && vote === -1)
         || ($feed.hasClass('like') && vote === 1)) {
        vote = 0;                 // reset
      }
    }
    if(id) {
      data.save_vote(id, vote, function () {
        if(vote === 1) {
          $feed.addClass('like').removeClass('dislike neutral sys');
        } else if(vote === -1) {
          $feed.addClass('dislike').removeClass('like neutral sys');
        } else if(vote === 0) {
          $feed.addClass('neutral').removeClass('like dislike sys');
        }
      });
    }
  }

  function settings () {
    hide_help();
    $reading_area.removeClass('show-iframe');
    var $welcome = set_welcome_title();
    $welcome.append(to_html(tmpls.settings, data.user_settings()));
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
        url = $input.val();
    if(url) {
      data.add_subscription(url, function () {
        $input.val('');
        // TODO adjust UI
      });
    }
  }

  function show_help () {
    hide_help();
    $('body').append(tmpls.keyboard);
  }

  function show_add_sub_ui () {
    hide_help();
    $reading_area.removeClass('show-iframe');
    var $welcome = set_welcome_title();
    $welcome.append(tmpls.add);
  }

  function hide_help () { $("#help, #subs").remove(); }

  function saveVoteUp (e) { save_vote(1, this); return false; }
  function saveVotedown (e) { save_vote(-1, this); return false; }

  function update_subs_sort_order (event, ui) {
    if(!ui.sender) { // prevent be callded twice if move bettween categories
      var $moved = $(ui.item),
          $before = $moved.prev(),
          moved_id = parseInt($moved.attr('data-id')),
          new_cat = $moved.closest('.rss-category').siblings('.folder').attr('data-name'),
          new_before_id = $before.length ? parseInt($before.attr('data-id')) : null;
      data.update_sort_order(moved_id, new_before_id, new_cat);
    }
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


  util.delegate_events($(document), {
    'click #add-subscription': add_subscription,
    'click #save-settings': save_settings,
    'click .vote span.down': saveVotedown,
    'click .vote span.up': saveVoteUp,
    'click #main .hover-switch': toggle_nav,
    'click #navigation .folder span': toggle_nav_foler
  });

  data.get_user_subs(function (subs) {
    var html = to_html(tmpls.nav, {subs: subs});
    $("#navigation ul.sub-list").empty().append(html);
    $("#navigation .item img").each(function (index, img) {
      img.onerror = function () { img.src="/imgs/16px-feed-icon.png"; };
    });
    // category sortable
    $('.sub-list').sortable({change: update_category_sort_order });
    $(".rss-category").sortable({ // subscription sortable with categories
      connectWith: ".rss-category",
      update: update_subs_sort_order
    });

    // $('#navigation .subs').mouseenter(switch_nav_to_subs);
    // $('#navigation .hover-switch').mouseenter(toggle_nav);

    RM.hashRouter({
      '': show_welcome,
      'settings': settings,
      'help': show_help,
      'add': show_add_sub_ui,
      'read/:id': read_subscription,
      'read/:id/:id': read_feed
    });
  });
  // export
  window.RM = $.extend(window.RM, {
    app: {
      hideHelp: hide_help,
      save_vote: save_vote,
      showHelp: show_help
    }
  });
})();
