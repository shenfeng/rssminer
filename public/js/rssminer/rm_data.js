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
      MAX_SORT_ORDER = 65535,
      INIT_SORT_ORDER = 256,
      NEUTRAL_SCORE = user_conf.neutral_score || 0; // db default 0

  var TITLES = {
    recommend: 'Recommand for you',
    voted: 'Recently voted',
    read: 'Recently read'
  };

  var SORTINGS = {
    'newest': util.cmp_by('published_ts', null, -1), // revert sort
    'oldest': util.cmp_by('published_ts', null, 1),
    'likest': util.cmp_by('vote_user', util.cmp_by('vote_sys', null, -1), -1)
    // first by user's vote, then by sys's vote
    // 'reading': util.cmp_by('read_date', null, -1)
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
      return subid === sub.id;
    });
  }

  function get_feed (subid, feedid) {
    var cache = get_cached_feeds(subid),
        feed = _.find(cache, function (feed) {
          return feed.id === feedid;
        });
    return transorm_item()(feed);
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
    return Math.round(new Date().getTime() / 1000);
  }

  function sub_array (list, offset, length) {
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
      if(!has) {
        feed.read_date = feed.read_date || -1; // db
        feed.vote_sys = feed.vote_sys || 0;
        feed.vote_user = feed.vote_user || 0;
        cache.push(feed);
      }
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

  function transorm_item (subid) {
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
                title_l: i.title.toLowerCase(),
                href: sub_hash(i.id, 'newest'),
                like: i.like_c,
                total: i.total_feeds,
                index: i.sort_index,
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
  function get_user_subs (cb) {
    if(global_cache.subscriptions) {
      cb(parse_subs(global_cache.subscriptions));
    } else {
      ajax.get('/api/subs', function (resp) {
        var result = parse_subs(resp),
            cache = [];
        _.each(result, function (group) { // for the first time, all sort_index is 0
          _.each(group.list, function (sub) {
            cache.push(_.find(resp, function (i) { return i.id === sub.id; }));
          });
        });
        global_cache.subscriptions = cache;
        cb(result);
      });
    }
  }

  function get_welcome_list (cb) { // no cache
    ajax.get('/api/welcome', function (resp) {
      var result = [];
      for(var section in TITLES) {
        result.push({
          title: TITLES[section],
          feeds: _.map(resp[section], transorm_item())
        });
      }
      cb({
        title: 'Rssminer - an intelligent RSS reader',
        section: result
      });
    });
  }

  function sub_hash (id, sort) {
    return 'read/' + id + '?s=' + sort;
  }

  function get_feeds (subid, offset, limit, sort, cb) {
    var cache = get_cached_feeds(subid),
        total = get_total_feeds(subid);
    // TODO, cache according to sorting
    if(cache && (total <= cache.length || cache.length >= offset + limit)) {
      var feeds = cache.sort(SORTINGS[sort]);
      feeds = _.map(feeds, transorm_item(subid));
      var sort_data = [];
      for(var s in SORTINGS) {
        sort_data.push({
          selected: !sort || s === sort,
          href: sub_hash(subid, s),
          text: s
        });
      }
      cb({
        feeds: sub_array(feeds, offset, limit),
        sort: sort_data
      });
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
      var cache = get_cached_feeds(subid),
          feed = _.find(cache, function (feed) {
            return feed.id === feedid;
          });
      feed.read_date = current_time();
      if(typeof cb === 'function') { cb(feed); }
    });
  }

  function save_vote (feedid, vote, cb) {
    ajax.spost('/api/feeds/' + feedid  + '/vote', {vote: vote}, function () {
      for(var key in global_cache) {
        // TODO brute force
        if(key.indexOf('sub_') === 0) {
          var feed = _.find(global_cache[key], function (feed) {
            return feed.id === feedid;
          });
          if(feed) { feed.vote_user = vote; break; }
        }
      }
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

  function update_sort_order (moved_id, new_before, new_cat) {
    var subs = global_cache.subscriptions;
    var step = Math.min(Math.floor(MAX_SORT_ORDER / subs.length), 256);
    var moved =  get_subscription(moved_id);
    var old_idx = _.indexOf(subs, moved);
    var before = get_subscription(new_before);
    var before_idx = _.indexOf(subs, before);
    var update_cat = moved.group_name !== new_cat;

    if(update_cat) { moved.group_name = new_cat; }

    var save_data = [],
        generate_all = false,
        self = update_cat ? {g: new_cat, id: moved.id}: {id: moved.id};

    if(before_idx === -1) { // no prev element
      if(subs[0].sort_index >= 2) {
        self.o = Math.floor(subs[0].sort_index / 2);
        save_data.push(self);
      } else {
        generate_all = true;
      }
    } else if (before_idx === subs.length - 2 ) { // the last one
      if(before.sort_index + step < MAX_SORT_ORDER) {
        self.o = before.sort_index + step;
        save_data.push(self);
      } else {
        generate_all = true;
      }
    } else {
      var gap = subs[before_idx + 1].sort_index - before.sort_index;
      if( gap > 2 ) {
        self.o = before.sort_index + Math.floor(gap / 2);
        save_data.push(self);
      } else {
        generate_all = true;
      }
    }

    if (generate_all){                    // regenerate all
      var sort_index = INIT_SORT_ORDER;
      for(var i = 0; i < subs.length; i++) {
        if(old_idx !== i) {
          save_data.push({id: subs[i].id, o: sort_index});
          subs[i].sort_index = sort_index;
          sort_index += step;
          if(before_idx === i) {
            self.o = sort_index;
            save_data.push(self);
            subs[old_idx].sort_index = sort_index;
            sort_index += step;
          }
        }
      }
    }

    if(self.o) { moved.sort_index = self.o; }    // update sort_index

    global_cache.subscriptions = _.sortBy(subs, function (s) {
      return s.sort_index;
    });
    ajax.spost('/api/subs/sort', save_data);
  }

  function get_search_result (q, limit, cb) {
    var result = [],
        count = 0,
        grouped = parse_subs(global_cache.subscriptions);
    _.each(grouped, function (group) {
      _.each(group.list, function (sub) {
        if((!q || sub.title_l.indexOf(q) !== -1) && count < limit) {
          if(sub.total) {
            result.push(sub);
            count++;
          }
        }
      });
    });
    cb(result);
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      get_search_result: get_search_result,
      get_feed: get_feed,
      get_feeds: get_feeds,
      get_final_link: get_final_link,
      get_subscription: get_subscription,
      get_user_subs: get_user_subs,
      get_welcome_list: get_welcome_list,
      is_user_has_subscription: is_user_has_subscription,
      mark_as_read: mark_as_read,
      save_vote: save_vote,
      user_settings: user_settings,
      update_sort_order: update_sort_order
    }
  });
})();
