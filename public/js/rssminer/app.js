(function () {
  var data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      hashRouter = RM.hashRouter,
      ajax = RM.ajax,
      layout = RM.layout,
      to_html = Mustache.to_html;

  var titles = {
    recommend: 'Recommand for you',
    voted: 'Recently voted',
    read: 'Recently read'
  };

  var $footer = $('#footer'),
      $loader = $('#reading-chooser .loader'),
      $reading_area = $('#reading-area');

  function focusFirstFeed () {
    $($('.welcome-list li.feed')[0]).addClass('selected');
    $reading_area.removeClass('show-iframe');
  }

  function addWelcomeTitle (title) {
    var $welcome = $('.welcome-list').empty();
    if(title) { $welcome.append('<h2>' + title +'</h2>'); }
    return $welcome;
  }

  function showFooterList () {
    $footer.show();
    $reading_area.addClass('show-iframe');
    layout.reLayout();
  }

  function hideFooterList () {
    $footer.hide();
    $reading_area.removeClass('show-iframe');
    layout.reLayout();
  }

  function readSubscription (id, callback) {
    hideHelp();
    var title = $("#item-" + id + " .title").text().trim();
    hideFooterList();
    if(layout.select('.sub-list', "item-" + id)) {
      ajax.get("/api/subs/" + id, function (resp) {
        var d = data.parseFeedList(id, resp),
            html = to_html(tmpls.list, {feeds: d});
        $('#feed-list ul').empty().append(html);
        if(d.length > 0) {
          var $welcome = addWelcomeTitle(title),
              result = data.parseFeedListForWelcome(id, resp);
          for(var i = 0; i < result.length; i++) {
            var r = result[i];
            if(r.list.length) {
              $welcome.append(to_html(tmpls.welcome_section, result[i]));
              focusFirstFeed();
            }
          }
        }
        if(typeof callback === 'function') { callback(); }
      });
    } else if (typeof callback === 'function') {
      callback();
    }
  }

  function descrementNumber ($just_read, subid) {
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

  function readFeed (subid, feedid) {
    readSubscription(subid, function () {
      showFooterList();
      var me = "feed-" + feedid,
          $me = $('#' + me),
          link = $me.attr('data-link'),
          title = $('.title', $me).text().trim();
      if(layout.select('#feed-list', me)){
        $('#footer .info a').text(link).attr('href', link);
        $('#footer .info h5').text(title);
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
      ajax.jpost('/api/feeds/' + feedid + '/read', function () {
        descrementNumber($me, subid);
        $me.removeClass('unread sys-read').addClass('read');
      });
    }
  }

  function show_welcome () {
    if(_RM_.subs) {             // user has subscriptions
      var $welcome = addWelcomeTitle('Rssminer - an intelligent RSS reader');
      ajax.get('/api/user/welcome', function (resp) {
        if(typeof resp === 'string') { resp = JSON.parse(resp); }
        for(var name in titles) {
          var list = data.parseWelcomeList(resp[name]),
              html = to_html(tmpls.welcome_section, {
                title: titles[name],
                list: list
              });
          if(list.length) { $welcome.append(html); focusFirstFeed(); }
        }
      });
    } else { location.hash = "add"; }
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
    if(($feed.hasClass('dislike') && vote === -1)
       || ($feed.hasClass('like') && vote === 1)) {
      vote = 0;                 // reset
    }
    if(id) {
      ajax.jpost('/api/feeds/' + id  + '/vote', {vote: vote}, function () {
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
    var $welcome = addWelcomeTitle();
    $welcome.append(to_html(tmpls.settings, data.userSettings()));
  }

  function saveSettings (e) {
    var d = util.extractData( $('#settings') );
    for(var i in d) { if(!d[i]) { delete d[i]; } }
    d.expire = parseInt(d.expire, 10);
    if(d.password && d.password !== d.password2) {
      alert('2 password not match');
      return;
    }
    delete d.password2;
    RM.ajax.jpost('/api/user/settings', d, function () {
      location = "/a";
    });
  }

  function addSubscriptionHandler (e) {
    var $input = $("#rss_atom_url"),
        url = $input.val();
    if(url) {
      ajax.jpost("/api/subs/add", {link: url}, function (data) {
        $input.val('');
        var max_times = 3,
            polling_interval = 2000;
        var polling = function () {
          if(max_times > 0) {
            max_times -= 1;
            ajax.get('/api/subs/p/' + data.rss_link_id, function (sub) {
              var s0 = sub && sub[0];
              if(s0 && s0.title) {
                s0.group_name = null; // newly added, no-group
                _RM_.subs.push(s0);
                render_nav_list();
              } else {
                polling_interval += 1500;
                setTimeout(polling, polling_interval);
              }
            });
          }
        };
        setTimeout(polling, polling_interval);
      });
    }
  }

  function showHelp () {
    hideHelp();
    $('body').append(tmpls.keyboard);
  }

  function addSubscription () {
    hideHelp();
    $reading_area.removeClass('show-iframe');
    var $welcome = addWelcomeTitle();
    $welcome.append(tmpls.add);
  }

  function hideHelp () { $("#help, #subs").remove(); }

  function saveVoteUp (e) { saveVote(1, this); return false; }
  function saveVotedown (e) { saveVote(-1, this); return false; }

  function save_sort_order (event, ui) {
    // console.log('saveing', event, ui, $(ui.item));
  }

  function render_nav_list () {
    var nav = to_html(tmpls.nav, {subs: data.parseSubs(_RM_.subs)});
    $("#navigation ul.sub-list").empty().append(nav);
    $('.sub-list').sortable();  // category sortable
    $(".rss-category").sortable({ // subscription sortable with categories
      connectWith: ".rss-category",
      stop: save_sort_order
    });
  }

  util.delegate_events($(document), {
    'click .vote span.up': saveVoteUp,
    'click #save-settings': saveSettings,
    'click #add-subscription': addSubscriptionHandler,
    'click .vote span.down': saveVotedown,
    'click .chooser li': toggleWelcome
  });

  window.RM = $.extend(window.RM, {
    app: {
      hideHelp: hideHelp,
      save_vote: saveVote,
      showHelp: showHelp
    }
  });

  render_nav_list();              // should before hashRouter;

  hashRouter({
    '': show_welcome,
    'settings': settings,
    'help': showHelp,
    'add': addSubscription,
    'read/:id': readSubscription,
    'read/:id/:id': readFeed
  });
})();
