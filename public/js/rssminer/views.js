(function () {
  var util = RM.util,
      tmpls = RM.tmpls,
      to_html = Mustache.to_html;

  function _compute_by_tag() {
    var subs = _.map(window._BY_TAG_, function (item) {
      return {
        href: "#tag/" + item.t,
        c: item.c,              // count
        text: item.t            // tag
      };
    });
    subs = subs.sort(function (l, r) {
      return r.c - l.c;         // sort by count desc
    });
    return { text: 'By Tag', has_sub: true, subs: subs };
  }

  function render_nav() {
    var count = _.reduce(_.values(window._BY_TIME_), function (memo, num) {
      return memo + num;
    }, 0),
        subs = _.map(window._SUBS_, function (i) {
          return {
            text: i.title,
            id: i.id,
            hostname: util.hostname(i.url),
            href: "#subs/" + i.id,
            c: _BY_SUB_[i.id] };
        });
    subs = _.sortBy(subs, function (i) { return  _.isNumber(i.c) ? -i.c : 1; });

    var navs = [{ text: 'All', href: '#all', c: count},
                { text: 'Recommanded', has_sub: true,
                  subs: [{text: 'Items', href: '#r/items'},
                         {text: 'Subscriptions', href: '#r/subs'}]},
                { text: 'Subscriptions',has_sub: true, subs: subs}];

    return to_html(tmpls.nav, {navs: navs});
  }

  function render_mid () {
    var hash = window.location.hash || '#all',
        s = hash.split('/');
    if(s.length == 3) {
      hash = s[0] + '/' + s[1];
    }
    var feeds = _.map(window._FEEDS_, function (i) {
      i.date = util.ymdate(i.published_ts * 1000);
      i.host = util.hostname(i.link);
      i.href = hash + '/' + i.id;
      return i;
    });
    return to_html(tmpls.feeds, {feeds: feeds});
  }

  window.RM = _.extend(window.RM, {
    render_nav: render_nav,
    render_mid: render_mid,
    render_right: function (data) {
      data.published_ts = util.ymdate(data.published_ts * 1000);
      return to_html(tmpls.feed, data);
    }
  });
})();
