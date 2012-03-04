// functions to transform data
(function () {
  var util = RM.util,
      user = (_RM_ && _RM_.user) || {},
      proxy_server = _RM_.proxy_server,
      static_server = _RM_.static_server,
      user_conf = user.conf || {},
      expire = user_conf.expire || 45,
      like_score = user_conf.like_score || 1,
      neutral_score = user_conf.neutral_score || 0; // db default 0

  var by = function (name, minor, reverse) { // reverse when -1
    reverse = reverse || -1;
    return function (o, p) {
      var a, b;
      if (o && p && typeof o === 'object' && typeof p === 'object') {
        a = o[name];
        b = p[name];
        if (a === b) {
          return typeof minor === 'function' ? minor(o, p) : 0;
        }
        if (typeof a === typeof b) {
          return reverse * (a < b ? -1 : 1);
        }
        return reverse * (typeof a < typeof b ? -1 : 1);
      } else {
        throw {
          name: 'Error',
          message: 'Expected an object when sorting by ' + name
        };
      }
    };
  };

  function parseTags (tags) {
    if(tags) { return tags.split("; "); }
    else { return []; }
  }

  function  feedClass (i) {
    var cls;
    if(i.read_date === 1) { cls = 'sys-read'; }
    else if(i.read_date > 1) { cls = 'read';}
    else {cls = 'unread';}

    if (i.vote < 0) { cls += ' dislike'; }
    else if (i.vote > 0) { cls += ' like'; }
    else if(i.vote_sys > like_score) { cls += ' like sys'; }
    else if(i.vote_sys < neutral_score) { cls += ' dislike sys';}
    else { cls += ' neutral sys'; }

    return cls;
  }

  function faviconPath (url) {
    var host = encodeURIComponent(util.hostname(url));
    return static_server + '/fav?h=' + host;
  }

  function getAllSubTitle (filter) {
    var subs = _RM_.subs, result = [];
    _.each(subs, function (sub) {
      var title = sub.title || sub.o_title;
      if(!filter || title.toLowerCase().indexOf(filter) !== -1) {
        result.push({ title: title, id: sub.id });
      }
    });
    return result;
  }

  function parseSubs (subs) {
    var grouped = _.groupBy(subs, 'group_name'),
        result = [],
        collapsed = user_conf.nav || [];
    for(var tag in grouped) {
      var list = _(grouped[tag]).chain()
            .sortBy(function (i) { return i.sort_index; })
            .map(function(i) {
              return {
                img: faviconPath(i.url),
                title: i.title || i.o_title, // original title
                href: 'read/' + i.id,
                like: i.like_c,
                dislike: i.dislike_c,
                neutral: i.total_c - i.like_c - i.dislike_c,
                id: i.id
              };
            }).value();
      list = _.filter(list, function (i) { return i.title; });
      result.push({
        tag: tag,
        list: list,
        collapse: _.include(collapsed, tag)
      });
    }
    result = _.sortBy(result, function (i) { return i.tag.toLowerCase(); });
    return result;
  }

  function ymdate (i, force) {
    var date = force || (i.read_date > 1 ? i.read_date : i.published_ts);
    var d = new Date(date * 1000),
        m = d.getMonth() + 1,
        day = d.getDate();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  }

  function  transformItem (subid) {
    // subid is undefined when used by parsewelcomelist
    return function (i) {
      return {
        author: i.author,
        cls: feedClass(i),
        date: subid ? ymdate(i, i.published_ts) : ymdate(i),
        href: 'read/' + (subid || i.rss_link_id) + "/" + i.id,
        id: i.id,
        link: i.link,
        tags: parseTags(i.tags),
        title: i.title
      };
    };
  }

  function parseWelcomeList (data, subid) {
    return _.map(data, transformItem(subid));
  }

  function defaultFeedData (e) {
    var now = new Date().getTime() / 1000;
    // mark old enough (45d) as readed
    if(now - e.published_ts > expire * 3600 * 24 && e.read_date < 1) {
      e.read_date = 1;
    }
    // -1 is db default value
    e.read_date = e.read_date === null ? -1 : e.read_date;
    e.vote_sys = e.vote_sys === null ? 1 : e.vote_sys;
    e.vote = e.vote === null ? 0 : e.vote;
  }

  function parseFeedListForWelcome (subid, data) {
    _.each(data, defaultFeedData); // default value
    var unread = _.filter(data, function (i) { return i.read_date < 0;}),
        outdated = _.filter(data, function (i) { return i.read_date === 1;}),
        readed = _.filter(data, function (i) { return i.read_date > 1;});

    var cmp = by('vote', by('vote_sys',
                            by('published_ts', null, -1), -1), -1);

    readed.sort(cmp);
    outdated.sort(cmp);
    unread.sort(cmp);

    return [{
      title: 'Feeds that are not read',
      list: _.map(unread, transformItem(subid))
    },{
      title: 'Feeds that have been read',
      list: _.map(readed, transformItem(subid))
    }, {
      title: 'Feeds that are outdated',
      list: _.map(outdated, transformItem(subid))
    }];
  }

  function parseFeedList (subid, data) {
    _.each(data, defaultFeedData); // default value
    var unread = _.filter(data, function (i) { return i.read_date <= 0;}),
        readed = _.filter(data, function (i) { return i.read_date > 0;});
    var cmp = by('vote', by('vote_sys',
                            by('published_ts', null, -1), -1), -1);
    readed.sort(cmp);
    unread.sort(cmp);
    data = unread.concat(readed);

    var result = _.map(data,transformItem(subid));
    return result;
  };

  function userSettings () {
    var expire_times = [];
    for(var i = 15; i <= 120; i += 15) {
      expire_times.push({time: i, selected: i === expire});
    }
    return {expire_times: expire_times};
  }

  var bypass_proxy_sites = ['groups.google', // X-Frame-Options
                            "feedproxy",
                            "alibuybuy",
                            "javaworld" // for Readability
                                           ];

  var reseted_sites = ["wordpress", "appspot", 'emacsblog','blogger',
                       "blogspot", 'mikemccandless'];

  function is_bypass_proxy_site (link) {
    var h = util.hostname(link);
    return _.any(bypass_proxy_sites, function (site) {
      return h.indexOf(site) !== -1;
    });
  }

  function get_final_link (link, feedid) {
    if(is_bypass_proxy_site(link)) {
      return proxy_server + "/f/o/" + feedid + "?p=1";
    } else {
      if(util.enableProxy() && is_reseted_site(link)) {
        return proxy_server + "/f/o/" + feedid + "?p=1";
      }
      return link;
    }
  }

  function is_reseted_site (link) {
    var h = util.hostname(link);
    return _.any(reseted_sites, function (site) {
      return h.indexOf(site) !== -1;
    });
  }

  function shouldProxy (link) {
    var hostname= util.hostname(link);
    for(var i = 0; i < reseted_sites.length; i++) {
      if(hostname.indexOf(reseted_sites[i]) != -1) {
        return true;
      }
    }
    return false;
  }

  // export
  window.RM = $.extend(window.RM, {
    data: {
      parseSubs: parseSubs,
      get_final_link: get_final_link,
      userSettings: userSettings,
      parseFeedListForWelcome: parseFeedListForWelcome,
      parseFeedList: parseFeedList,
      parseWelcomeList: parseWelcomeList,
      getAllSubTitle: getAllSubTitle
    }
  });

})();
