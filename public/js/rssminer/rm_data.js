(function () {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util;

  var user = (_RM_ && _RM_.user) || {},
      user_conf = user.conf || {},
      expire = user_conf.expire || 45,
      global_cache = {};

  var CACHE_TIME = 1000 * 60 * 60 * 4, // 4 hour
      POLLING_TIMES = 4,
      POLLING_INTERVAL = 3000,
      PROXY_SERVER = window._RM_.proxy_server,
      STATIC_SERVER = window._RM_.static_server,
      LIKE_SCORE = user_conf.like_score || 1,
      NEUTRAL_SCORE = user_conf.neutral_score || 0; // db default 0

  var TITLES = {
    recommend: 'Recommand for you',
    voted: 'Recently voted',
    read: 'Recently read'
  };

  var BYPASS_PROXY_SITES = ['groups.google', // X-Frame-Options
                            "feedproxy",
                            // "alibuybuy",
                            "javaworld" // for Readability
                                           ];

  function user_settings () {
    var expire_times = [];
    for(var i = 15; i <= 120; i += 15) {
      expire_times.push({time: i, selected: i === expire});
    }
    return {expire_times: expire_times};
  }

  var RESETED_SITES = ["wordpress", "appspot", 'emacsblog','blogger',
                       "blogspot", 'mikemccandless'];

  function is_bypass_proxy_site (link) {
    var h = util.hostname(link);
    return _.any(BYPASS_PROXY_SITES, function (site) {
      return h.indexOf(site) !== -1;
    });
  }

  function get_subscription (subid) {
    return _.find(global_cache.subscriptions, function (sub) {
      return subid = sub.id;
    });
  }

  function get_feed (subid, feedid) {
    var cache = get_cached_feeds(subid);
    return _.find(cache, function (feed) {
      return feed.id === feedid;
    });
  }

  function get_final_link (link, feedid) {
    if(is_bypass_proxy_site(link)) {
      return PROXY_SERVER + "/f/o/" + feedid + "?p=1";
    } else {
      if(util.enableProxy() && is_reseted_site(link)) {
        return PROXY_SERVER + "/f/o/" + feedid + "?p=1";
      }
      return link;
    }
  }

  function is_reseted_site (link) {
    var h = util.hostname(link);
    return _.any(RESETED_SITES, function (site) {
      return h.indexOf(site) !== -1;
    });
  }

  function shouldProxy (link) {
    var hostname= util.hostname(link);
    for(var i = 0; i < RESETED_SITES.length; i++) {
      if(hostname.indexOf(RESETED_SITES[i]) != -1) {
        return true;
      }
    }
    return false;
  }

  // helper function
  function current_time () {
    return new Date().getTime();
  }

  function sub_list (list, offset, length) {
    var result = [];
    if(list.length) {
      for(var i = offset; i < list.length && i < offset + length; i++) {
        result.push(list[i]);
      }
    }
    return result;
  }

  function get_total_feeds (subid) {
    var sub =_.find(global_cache.subscriptions, function (sub) {
      return sub.id === subid;
    });
    return sub ? sub.total_feeds : 0;
  }

  function get_cached_feeds (subid) {
    return global_cache['sub_' + subid];
  }

  function cache_feeds (subid, feeds) {
    var cache = get_cached_feeds(subid) || [];
    _.each(feeds, function (feed) {
      var has = _.any(cache, function (c) { return c.id === feed.id; });
      if(!has) { cache.push(feed); }
    });
    global_cache['sub_' + subid] = cache;
  }

  function favicon_path (url) {
    var host = util.hostname(url),
        h = encodeURIComponent(host.split("").reverse().join(''));
    return STATIC_SERVER + '/fav?h=' + h;
  }

  function split_tag (tags) {
    if(tags) { return tags.split("; "); }
    else { return []; }
  }

  function feed_css_class (i) {
    var cls;
    if(i.read_date === 1) { cls = 'sys-read'; }
    else if(i.read_date > 1) { cls = 'read';}
    else {cls = 'unread';}

    if (i.vote_user < 0) { cls += ' dislike'; }
    else if (i.vote_user > 0) { cls += ' like'; }
    else if(i.vote_sys > LIKE_SCORE) { cls += ' like sys'; }
    else if(i.vote_sys < NEUTRAL_SCORE) { cls += ' dislike sys';}
    else { cls += ' neutral sys'; }

    return cls;
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

  function  transorm_item (subid) {
    // subid is undefined when used by parsewelcomelist
    return function (i) {
      return {
        author: i.author,
        cls: feed_css_class(i),
        date: subid ? ymdate(i, i.published_ts) : ymdate(i),
        href: 'read/' + (subid || i.rss_link_id) + "/" + i.id,
        id: i.id,
        link: i.link,
        tags: split_tag(i.tags),
        title: i.title
      };
    };
  }

  function parse_subs (subs) {
    var grouped = _.groupBy(subs, 'group_name'),
        result = [],
        collapsed = user_conf.nav || [];
    for(var group in grouped) {
      var list = _(grouped[group]).chain()
            .sortBy(function (i) { return i.sort_index; })
            .map(function(i) {
              return {
                img: favicon_path(i.url),
                title: i.title,
                href: 'read/' + i.id,
                like: i.like_c,
                index: i.sort_index,
                sort_index: i.sort_index + '',
                dislike: i.dislike_c,
                neutral: i.total_c - i.like_c - i.dislike_c,
                id: i.id
              };
            }).value();
      list = _.filter(list, function (i) { return i.title; });
      if(list.length) {
        result.push({
          group: group,
          list: list,
          collapse: _.include(collapsed, group)
        });
      }
    }
    result = _.sortBy(result, function (i) { return i.list[0].index; });
    return result;
  }

  // helper funciton end here

  // API
  function get_user_subs (cb, bypass_cache) {
    if(global_cache.subscriptions) {
      cb(parse_subs(global_cache.subscriptions));
    } else {
      ajax.get('/api/subs', function (resp) {
        global_cache.subscriptions = resp;
        cb(parse_subs(resp));
      });
    }
  }

  function get_welcome_list (cb) { // no cache
    ajax.get('/api/welcome', function (resp) {
      var result = [];
      for(var section in TITLES) {
        result.push({
          title: TITLES[section],
          list: _.map(resp[section], transorm_item())
        });
      }
      cb(result);
    });
  }

  function get_feeds (subid, offset, limit, sort, cb) {
    var cache = get_cached_feeds(subid),
        total = get_total_feeds(subid);
    if(cache && (total <= cache.length || cache.length >= offset + limit)) {
      var cmp = util.cmp_by(sort === 'time' ? 'published_ts' : 'vote_sys',
                            null, -1); // revert sort
      var list = cache.sort(cmp);
      list = _.map(list, transorm_item(subid));
      cb(sub_list(list, offset, limit));
    } else {
      var url = '/api/subs/' + subid + '?offset=' + offset + '&limit=' + limit + '&sort=' + sort;
      ajax.get(url, function (resp) {
        cache_feeds(subid, resp);
        // recursive
        get_feeds(subid, offset, limit, sort, cb);
      });
    }
  }

  function is_user_has_subscription () {
    if(global_cache.subscriptions) {
      return global_cache.subscriptions.length;
    } else {
      return false;
    }
  }

  function mark_as_read (subid, feedid, cb) {
    ajax.spost('/api/feeds/' + feedid + '/read', function () {
      if(typeof cb === 'function') { cb(); }
    });
  }

  function save_vote (feedid, vote, cb) {
    ajax.spost('/api/feeds/' + feedid  + '/vote', {vote: vote}, function () {
      if(typeof cb === 'function') { cb(); }
    });
  }

  function polling_rss_link (rss_link_id, interval, times, cb) {
    if(times > 0) {
      ajax.sget('/api/subs/p/' + rss_link_id, function (sub) {
        // TODO refetch user subs
        if(sub && sub.title) {  // ok, title is fetched
          if(typeof cb === 'function') {
            cb();
          }
        } else {                // fetch again
          interval += 1500;
          times -= 1;
          window.setTimeout(function () {
            polling_rss_link(rss_link_id, interval, times, cb);
          }, interval);
        }
      });
    }
  }

  function add_subscription (url, cb) {
    ajax.jpost('/api/subs/add' , {link: url}, function (data) {
      polling_rss_link(data.rss_link_id, POLLING_INTERVAL, POLLING_TIMES, cb);
    });
  }

  function save_settings (data, cb) {
    ajax.jpost('/api/settings', data, function () {
      if(typeof cb === 'function') { cb(); }
    });
  }

  function update_group_name_sort_order (subid, sort_order, groupname) {

  }

  function get_all_sub_titles (filter) {
    var result = [];
    _.each(global_cache.subscriptions, function (sub) {
      if(!filter || sub.title.toLowerCase().indexOf(filter) != -1) {
        result.push({title: sub.title, id: sub.id});
      }
    });
    return result;
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      get_all_sub_titles: get_all_sub_titles,
      get_feed: get_feed,
      get_feeds: get_feeds,
      get_final_link: get_final_link,
      get_subscription: get_subscription,
      get_user_subs: get_user_subs,
      get_welcome_list: get_welcome_list,
      is_user_has_subscription: is_user_has_subscription,
      mark_as_read: mark_as_read,
      save_vote: save_vote,
      user_settings: user_settings
    }
  });
})();
