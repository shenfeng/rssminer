(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      layout = RM.layout,
      to_html = Mustache.to_html;

  var current_subid;

  var $footer = $('#footer'),
      $loader = $('#reading-chooser .loader'),
      $reading_area = $('#reading-area');

  function focus_first_feed () {
    $($('.welcome-list li.feed')[0]).addClass('selected');
    $reading_area.removeClass('show-iframe');
  }

  function set_welcome_title (title) {
    var $welcome = $('.welcome-list').empty();
    if(title) { $welcome.append('<h2>' + title +'</h2>'); }
    return $welcome;
  }

  function show_footer_list () {
    data.get_feeds(current_subid, 0, 40, 'time', function (feeds) {
      var html = to_html(tmpls.list, {feeds: feeds});
      $('#feed-list ul').empty().append(html);
      $footer.show();
      $reading_area.addClass('show-iframe');
      layout.reLayout();
    });
  }

  function hide_footer_list () {
    $footer.hide();
    $reading_area.removeClass('show-iframe');
    layout.reLayout();
  }

  function read_subscription (id, callback) {
    current_subid = id;
    hideHelp();
    hide_footer_list();
    var sub = data.get_subscription(id),
        title = sub.title;
    if(layout.select('.sub-list', "item-" + id)) {
      data.get_feeds(id, 0, 40, 'time', function (data) {
        if(data.length) {
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
      show_footer_list();
      var me = "feed-" + feedid,
          $me = $('#' + me);
      if(layout.select('#feed-list', me)) {
        var feed = data.get_feed(subid, feedid),
            title = feed.title,
            link = feed.link;
        $('#footer .info a').text(title).attr('href', link);
        var iframe = $('iframe')[0];
        $loader.css({visibility: 'visible'});
        iframe.src = data.get_final_link(link, feedid);
        iframe.onload = function () {
          mark_as_read($me, feedid, subid);
          $loader.css({visibility: 'hidden'});
        };
      }
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
            focus_first_feed();
          }
        }
      });
    } else {
      location.hash = "add";
    }
  }

  function saveVote (vote, ele) {
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

  function toggleWelcome () {
    var wantIframe = $(this).hasClass('iframe');
    if(wantIframe) {
      $reading_area.addClass('show-iframe');
    } else {
      $reading_area.removeClass('show-iframe');
    }
  }

  function settings () {
    hideHelp();
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
    hideHelp();
    $('body').append(tmpls.keyboard);
  }

  function show_add_sub_ui () {
    hideHelp();
    $reading_area.removeClass('show-iframe');
    var $welcome = set_welcome_title();
    $welcome.append(tmpls.add);
  }

  function hideHelp () { $("#help, #subs").remove(); }

  function saveVoteUp (e) { saveVote(1, this); return false; }
  function saveVotedown (e) { saveVote(-1, this); return false; }

  function save_sort_order (event, ui) {
    // console.log('saveing', event, ui, $(ui.item));
  }

  util.delegate_events($(document), {
    'click #add-subscription': add_subscription,
    'click #save-settings': save_settings,
    'click .chooser li': toggleWelcome,
    'click .vote span.down': saveVotedown,
    'click .vote span.up': saveVoteUp
  });

  data.get_user_subs(function (subs) {
    var nav = to_html(tmpls.nav, {subs: subs});
    $("#navigation ul.sub-list").empty().append(nav);
    $("#navigation .item img").each(function (index, img) {
      img.onerror = function () { img.src="/imgs/16px-feed-icon.png"; };
    });
    $('.sub-list').sortable();  // category sortable
    $(".rss-category").sortable({ // subscription sortable with categories
      connectWith: ".rss-category",
      stop: save_sort_order
    });

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
      hideHelp: hideHelp,
      save_vote: saveVote,
      showHelp: show_help
    }
  });
})();
