(function () {
  var rssminer = window.Rssminer,
      util = rssminer.util,
      tmpls = rssminer.tmpls,
      to_html = window.Mustache.to_html,
      _ = window._,
      $ = window.$;

  function _compute_by_tag() {
    var subs =  _.reduce(window._BY_TAG_, function (result, item, index) {
      result.push({
        href: "#tag/" + item.t,
        c: item.c,
        text: item.t
      });
      return result;
    }, []);
    subs =  _.sortBy(subs, function (item) { return -item.c; });
    return { text: 'By Tag', has_sub: true, subs: subs };
  }

  function render_nav() {
    var count = _.reduce(_.values(window._BY_TIME_), function (memo, num) {
      return memo + num;
    }, 0),
        subs = _.map(window._SUBS_, function (i) {
          return { text: i.title, href: "#sub/" + i.id, c: _BY_SUB_[i.id] };
        });
    subs = _.sortBy(subs, function (i) { return  _.isNumber(i.c) ? -i.c : 1; });

    var navs = [{ text: 'All', href: '#all', c: count},
                { text: 'Recommanded', has_sub: true,
                  subs: [{text: 'Items', href: '#r/items'},
                         {text: 'Subscriptions', href: '#r/subs'}]},
                _compute_by_tag(),
                { text: 'By Subscription',has_sub: true, subs: subs}];

    return to_html(tmpls.nav, {navs: navs});
  }

  function render_mid () {
    var feeds = _.map(window._FEEDS_, function (i) {
      i.date = util.ymdate(i.published_ts * 1000);
      i.host = util.hostname(i.link);
      return i;
    });
    return to_html(tmpls.feeds, {feeds: feeds});
  }

  rssminer = $.extend(rssminer, {
    render_nav: render_nav,
    render_mid: render_mid
  });
})();
