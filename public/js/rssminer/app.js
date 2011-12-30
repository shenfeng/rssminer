(function () {
  var data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      ajax = RM.ajax,
      layout = RM.layout,
      to_html = Mustache.to_html;

  var nav = to_html(tmpls.nav, {subs: data.parseSubs(_RM_.subs)});
  $("#navigation ul.sub-list").empty().append(nav);

  function readSubscription (id, callback) {
    if(layout.select('.sub-list', "item-" + id)) {
      ajax.get("/api/subs/" + id, function (d) {
        d = data.parseFeedList(id, d);
        var html = to_html(tmpls.list, {feeds: d});
        $('#feed-list ul').empty().append(html);
        if(typeof callback === 'function') {
          callback();
        } else if(d.length > 0) {
          location.hash = "read/" + id + '/' + d[0].id;
        }
      });
    } else {
      if(typeof callback === 'function') { callback(); }
    }
  }

  function readFeed (subid, feedid) {
    readSubscription(subid, function () {
      var me = "feed-" + feedid,
          $me = $('#' + me),
          link = $me.attr('data-link'),
          title = $('.title', $me).text().trim();
      if(layout.select('#feed-list', me)) {
        var src = link;
        if(util.isNeedProxy(link)) {
          src = _RM_.proxy_server + '/f/o/' + feedid + "?p=t";
        }
        $('iframe').attr('src', src);
        $('#footer .info h5').text(title);
        if($me.hasClass('unread')) {
          $me.removeClass('unread').addClass('read');
          ajax.jpost('/api/feeds/' + feedid + '/read');
        }
      }
    });
  }

  function saveVote (vote) {
    var $feed = $('.feed.selected'),
        id = $feed.attr('data-id');
    if(($feed.hasClass('dislike') && vote === -1)
       || ($feed.hasClass('like') && vote === 1)) {
      return;                   // already voted
    }
    if(id) {
      ajax.jpost('/api/feeds/' + id  + '/vote', {vote: vote}, function () {
        if(vote === 1) {
          $feed.addClass('like').removeClass('dislike neutral');
        } else if(vote === -1) {
          $feed.addClass('dislike').removeClass('like neutral');
        }
      });
    }
  }

  function saveVoteUp (e) { saveVote(1); return false; }
  function saveVotedown (e) { saveVote(-1); return false; }

  util.delegateEvents($(document), {
    'click .vote span.up': saveVoteUp,
    'click .vote span.down': saveVotedown
  });

  util.hashRouter({
    'read/:id': readSubscription,
    'read/:id/:id': readFeed
  });
})();
