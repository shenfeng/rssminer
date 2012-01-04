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
    if(layout.select('.sub-list', "item-" + id)) {
      ajax.get("/api/subs/" + id, function (d) {
        d = data.parseFeedList(id, d);
        var html = to_html(tmpls.list, {feeds: d});
        $('#feed-list ul').empty().append(html);
        if(typeof callback === 'function') { callback(); }
        else if(d.length > 0) {
          location.hash = "read/" + id + '/' + d[0].id;
        }
      });
    } else if (typeof callback === 'function') {
      callback();
    }
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

        link = util.getFinalLink(link, feedid);
        $loader.css({visibility: 'visible'});
        var iframe = $iframe.attr('src', link)[0];
        iframe.onload = function () { $loader.css({visibility: 'hidden'}); };
        $('#footer .info h5').text(title);
        if(!$me.hasClass('read')) {
          ajax.jpost('/api/feeds/' + feedid + '/read');
          $me.removeClass('unread sys-read').addClass('read');
        }
      }
    });
  }

  function welcome () {
    ajax.get('/api/user/welcome', function (resp) {
      var $welcome = $('.welcome-list').empty();
      if(typeof resp === 'string') { resp = JSON.parse(resp); }
      for(var name in titles) {
        var html = to_html(tmpls.welcome_section, {
          title: titles[name],
          list: data.parseWelcomeList(resp[name])
        });
        $welcome.append(html);
      }
    });
  }

  function saveVote (ele, vote) {
    var $feed = $(ele).closest('li.feed');
    $feed = $feed.length ? $feed : $('.feed.selected');
    var id = $feed.attr('data-id');
    if(($feed.hasClass('dislike') && vote === -1)
       || ($feed.hasClass('like') && vote === 1)) {
      vote = 0;                 // reset
    }
    if(id) {
      ajax.jpost('/api/feeds/' + id  + '/vote', {vote: vote}, function () {
        if(vote === 1) {
          $feed.addClass('like').removeClass('dislike neutral');
        } else if(vote === -1) {
          $feed.addClass('dislike').removeClass('like neutral');
        } else if(vote === 0) {
          $feed.addClass('neutral').removeClass('like dislike');
        }
      });
    }
  }

  function toggleWelcome () {
    var wantIframe = $(this).hasClass('iframe');
    if(wantIframe) {
      $reading_area.addClass('show-iframe');
    } else {
      welcome();
      $reading_area.removeClass('show-iframe');
    }
  }

  function saveVoteUp (e) { saveVote(this, 1); return false; }
  function saveVotedown (e) { saveVote(this, -1); return false; }

  // render feed list
  var nav = to_html(tmpls.nav, {subs: data.parseSubs(_RM_.subs)});
  $("#navigation ul.sub-list").empty().append(nav);

  util.delegateEvents($(document), {
    'click .vote span.up': saveVoteUp,
    'click .vote span.down': saveVotedown,
    'click .chooser li': toggleWelcome
  });

  util.hashRouter({
    '': welcome,
    'read/:id': readSubscription,
    'read/:id/:id': readFeed
  });
})();
