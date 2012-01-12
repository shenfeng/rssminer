(function () {
  var data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      ajax = RM.ajax,
      layout = RM.layout,
      to_html = Mustache.to_html;

  var titles = {
    read: 'Recently read',
    voted: 'Recently voted',
    recommend: 'Recommand for you'
  };


  var $footer = $('#footer'),
      $iframe = $('iframe'),
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
    var title = $("#item-" + id + " .title").text().trim();
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
        else { hideFooterList(); }
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
    showFooterList();
    readSubscription(subid, function () {
      var me = "feed-" + feedid,
          $me = $('#' + me),
          link = $me.attr('data-link'),
          title = $('.title', $me).text().trim();
      if(layout.select('#feed-list', me)){
        $('#footer .info a').text(link).attr('href', link);

        link = data.getFinalLink(link, feedid);
        $loader.css({visibility: 'visible'});
        var iframe = $iframe.attr('src', link)[0];
        iframe.onload = function () { $loader.css({visibility: 'hidden'}); };
        $('#footer .info h5').text(title);
        if(!$me.hasClass('read')) {
          ajax.jpost('/api/feeds/' + feedid + '/read', function () {
            descrementNumber($me, subid);
            $me.removeClass('unread sys-read').addClass('read');
          });
        }
      }
    });
  }

  function welcome () {
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
    }
  }

  function saveVote (vote, ele) {
    var $feed;
    //  1. select it's parent if ele is defined;
    if(ele) { $feed = $(ele).closest('li.feed'); }
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

  function saveVoteUp (e) { saveVote(1, this); return false; }
  function saveVotedown (e) { saveVote(-1, this); return false; }

  // render feed list
  var nav = to_html(tmpls.nav, {subs: data.parseSubs(_RM_.subs)});
  $("#navigation ul.sub-list").empty().append(nav);
  if(_RM_.subs.length) { addWelcomeTitle(); }

  util.delegateEvents($(document), {
    'click .vote span.up': saveVoteUp,
    'click #save-settings': saveSettings,
    'click .vote span.down': saveVotedown,
    'click .chooser li': toggleWelcome
  });

  window.RM = $.extend(window.RM, {
    app: {
      welcome: welcome,
      saveVote: saveVote
    }
  });

  util.hashRouter({
    '': welcome,
    'settings': settings,
    'read/:id': readSubscription,
    'read/:id/:id': readFeed
  });

})();
