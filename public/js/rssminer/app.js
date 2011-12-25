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
        }
      });
    } else {
      if(typeof callback === 'function') {
        callback();
      }
    }
  }

  function readFeed (subid, feedid) {
    readSubscription(subid, function () {
      var me = "feed-" + feedid;
      if(layout.select('#feed-list', me)) {
        $('iframe').attr('src', '/f/o/' + feedid);
        var title = $('.title', "#" + me).text().trim();
        $('#footer .info h5').text(title);
      }
    });
  }

  util.hashRouter({
    'read/:id': readSubscription,
    'read/:id/:id': readFeed
  });

})();
