(function () {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util,
      call_if_fn = util.call_if_fn;

  var user = (_RM_ && _RM_.user) || {},
      user_conf = user.conf || {},
      expire = user_conf.expire || 45;

  var subscriptions_cache,
      feeds_cache = {},
      cache_fixer = {};         // fix browser cache, inconsistency

  var CACHE_TIME = 1000 * 60 * 60 * 4, // 4 hour
      POLLING_TIMES = 4,
      STORAGE_KEY = '_rm_',
      MAX_PAGER = 9,
      WELCOME_MAX_PAGE = 5,
      POLLING_INTERVAL = 3000,
      PROXY_SERVER = window._RM_.proxy_server,
      STATIC_SERVER = window._RM_.static_server,
      MAX_SORT_ORDER = 65535,
      INIT_SORT_ORDER = 256,
      // per item 29 pixel, first feed to top 138px, 140 px for brower use
      PER_PAGE_FEEDS = Math.floor((screen.height - 138 - 140) / 29),
      LIKE_SCORE = user_conf.like_score || 1,
      NEUTRAL_SCORE = user_conf.neutral_score || 0; // db default 0

  // how many pages does each section has
  var WELCOME_TAGS = {recommand: 1, latest: 1, read: 1, voted: 1};

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

  function save_to_cache_fixer (feedid, data) {
    cache_fixer[feedid] = _.extend(data, cache_fixer[feedid]);
    if(window.localStorage) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cache_fixer));
    }
  }

  function user_settings () {
    var expire_times = [];
    for(var i = 15; i <= 120; i += 15) {
      expire_times.push({time: i, selected: i === expire});
    }
    return {
      expire_times: expire_times,
      groups: parse_subs(subscriptions_cache)
    };
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
    return _.find(subscriptions_cache, function (sub) {
      return subid === sub.id;
    });
  }

  function get_feed (feedid) {
    var feed;
    for(var key in feeds_cache) {
      var feeds = feeds_cache[key];
      feed = _.find(feeds, function (f) {
        return f.id === feedid;
      });
      if(feed) { break; }
    }
    return feed || {};
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

  function get_feeds_count (subid) {
    var sub =_.find(subscriptions_cache, function (sub) {
      return sub.id === subid;
    });
    return sub ? sub.total_feeds : 0;
  }

  function favicon_path (url) {
    var host = util.hostname(url),
        h = encodeURIComponent(host.split("").reverse().join(''));
    return STATIC_SERVER + '/fav?h=' + h;
  }

  function split_tag (tags) {
    if(tags) { return sub_array(tags.split("; "), 0, 4); } // at most 4
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

  function transform_item (subid) {
    var titles = {};
    _.each(subscriptions_cache, function (sub) {
      titles[sub.id] = transorm_sub(sub);
    });
    // subid is undefined when used by parsewelcomelist
    return function (i) {
      var cf = cache_fixer[i.id],
          sub_id = subid || i.rss_link_id;
      if(cf) {
        // try to fix outdated data, browser cache 1 hour
        i.read_date = cf.read_date || i.read_date;
        i.vote_user = cf.vote_user || i.vote_user;
      }
      return {
        author: i.author,
        sub: titles[sub_id],    // use to show search result
        cls: feed_css_class(i),
        date: subid ? ymdate(i, i.published_ts) : ymdate(i),
        href: 'read/' + sub_id + "/" + i.id,
        id: i.id,
        link: i.link,
        tags: split_tag(i.tags),
        title: i.title
      };
    };
  }

  function transorm_sub (i) {
    var title = i.title || i.url;
    return {
      img: favicon_path(i.url),
      title: title,
      title_l: title.toLowerCase(),
      href: sub_hash(i.id, 1, 'newest'),
      like: i.like_c,
      total: i.total_feeds,
      index: i.sort_index,
      dislike: i.dislike_c,
      neutral: i.total_c - i.like_c - i.dislike_c,
      id: i.id        // rss_link_id
    };
  }

  function parse_subs (subs) {
    var grouped = _.groupBy(subs, 'group_name'),
        result = [],
        collapsed = user_conf.nav || [];
    for(var group in grouped) {
      var list = _(grouped[group]).chain()
            .sortBy(function (i) { return i.sort_index; })
            .map(transorm_sub).value();
      list = _.filter(list, function (i) { return i.title; });
      if(list.length) {
        result.push({
          group: group,
          subs: list,
          collapse: _.include(collapsed, group)
        });
      }
    }
    result = _.sortBy(result, function (i) { return i.subs[0].index; });
    return result;
  }

  // helper funciton end here

  // API
  function get_user_subs (cb) {
    if(subscriptions_cache) {
      cb(parse_subs(subscriptions_cache));
    } else {
      ajax.get('/api/subs', function (resp) {
        var result = parse_subs(resp),
            cache = [];
        _.each(result, function (group) { // for the first time, all sort_index is 0
          _.each(group.subs, function (sub) {
            // keep them in sort order
            cache.push(_.find(resp, function (i) { return i.id === sub.id; }));
          });
        });
        subscriptions_cache = cache;
        cb(result);
      });
    }
  }

  function get_welcome_list (section, page, cb) { // no cache
    var params = util.params({
      section: section,
      limit: PER_PAGE_FEEDS,
      offset: Math.max(0, page-1) * PER_PAGE_FEEDS
    });
    var sort_data = _.map(_.keys(WELCOME_TAGS), function (tab) {
      return {
        text: tab,
        selected: section === tab,
        href: tab_hash(tab, 1)
      };
    });

    ajax.get('/api/welcome?' + params, function (resp) {
      var feeds = _.map(resp, transform_item());
      feeds_cache[section] = feeds;
      cb({
        title: 'Rssminer - an intelligent RSS reader',
        feeds: feeds,
        pager: compute_welcome_paging(section, page, resp.length),
        sort: sort_data
      });
    });
  }

  function sub_hash (id, page, sort) {
    var href = 'read/' + id;
    if(page) { href = href + '?p='+ page; }
    if(sort) { href = href + "&s=" + sort; }
    return href;
  }

  function tab_hash (section, page) {
    return '?s=' + section + '&p=' + page;
  }

  function get_feeds (subid, page, sort, cb) {
    var total = get_feeds_count(subid),
        offset = Math.max(0, page -1) * PER_PAGE_FEEDS;

    sort = SORTINGS[sort] ? sort : 'newest';
    var url = '/api/subs/' + subid + '?' + util.params({
      offset: offset,
      limit: PER_PAGE_FEEDS,
      sort: sort
    });
    ajax.get(url, function (resp) {
      var feeds =  _.map(resp, transform_item(subid)),
          sort_data = [];
      feeds_cache['current_sub'] = feeds;
      for(var s in SORTINGS) {
        sort_data.push({
          selected: !sort || s === sort,
          href: sub_hash(subid, 1, s),
          text: s
        });
      }
      cb({
        feeds: feeds,
        sort: sort_data,
        pager: compute_sub_paging(subid, sort, total, page, PER_PAGE_FEEDS)
      });
    });
  }

  function should_include (page_count, current, page) {
    if(page_count < MAX_PAGER) {
      return true;
    } else if(page === 1 || page === page_count || page === current){
      return true;
    } else if(Math.abs(current - page) < Math.ceil(MAX_PAGER / 2)){
      return true;
    }
    return false;
  }

  function compute_welcome_paging (section, page, last_count) {
    var has_more = last_count === PER_PAGE_FEEDS; // maybe has more
    if(page >= WELCOME_MAX_PAGE) {
      has_more = false;         // do not show too much
    }
    if(has_more) {
      // how many pages
      WELCOME_TAGS[section] = Math.max(WELCOME_TAGS[section], page + 1);
    }
    if(page === 1 && !has_more) {
      return false;
    } else {
      var pages = [];
      for(var i = 1; i <= WELCOME_TAGS[section]; i++) {
        pages.push({
          page: i,
          current: i === page,
          href: tab_hash(section, i)
        });
      }
      if(has_more) {
        pages.push({
          page: 'next',
          current: false,
          href: tab_hash(section, page + 1)
        });
      }
      return {pages: pages};
    }
  }

  function compute_sub_paging (subid, sorting, total, page, per_page) {
    if(total <= per_page) {
      return false;
    } else {
      var count = Math.ceil(total / per_page),
          pages = [];
      for(var i = 1; i < count + 1; i++) {
        if(should_include(count, page, i)) {
          pages.push({
            page: i,
            current: i === page,
            href: sub_hash(subid, i, sorting)
          });
        }
      }
      return {
        count: count,
        page: page,
        pages: pages
      };
    }
  }

  function is_user_has_subscription () {
    if(subscriptions_cache) {
      return subscriptions_cache.length;
    } else {
      return false;
    }
  }

  function mark_as_read (feedid, cb) {
    ajax.spost('/api/feeds/' + feedid + '/read', function () {
      save_to_cache_fixer(feedid, {read_date: current_time()});
      call_if_fn(cb);
    });
  }

  function save_vote (feedid, vote, cb) {
    ajax.spost('/api/feeds/' + feedid  + '/vote', {vote: vote}, function () {
      save_to_cache_fixer(feedid, {vote_user: vote});
      call_if_fn(cb);
    });
  }

  function polling_rss_link (rss_link_id, interval, times, cb) {
    if(times > 0) {
      ajax.sget('/api/subs/p/' + rss_link_id, function (sub) {
        // TODO refetch user subs
        if(sub && sub.title) {  // ok, title is fetched
          sub.group_name = null; // server return no group_name
          subscriptions_cache.push(sub);
          call_if_fn(cb, sub);
        } else {                // fetch again
          interval += 1500;
          times -= 1;
          window.setTimeout(function () {
            polling_rss_link(rss_link_id, interval, times, cb);
          }, interval);
        }
      });
    } else {
      call_if_fn(cb);
    }
  }

  function add_subscription (url, added_cb, fetcher_finished) {
    ajax.jpost('/api/subs/add' , {link: url}, function (data) {
      polling_rss_link(data.rss_link_id,
                       POLLING_INTERVAL, POLLING_TIMES, fetcher_finished);
      call_if_fn(added_cb);
    });
  }

  function unsubscribe (id, cb) {
    ajax.del('/api/subs/' + id, function (resp) {
      var cache = [];
      _.each(subscriptions_cache, function (sub) {
        if(sub.id !== id) {
          cache.push(sub);
        }
      });
      subscriptions_cache = cache;
      call_if_fn(cb);
    });
  }

  function save_settings (data, cb) {
    ajax.jpost('/api/settings', data, function () {
      call_if_fn(cb);
    });
  }

  function update_sort_order (moved_id, new_before, new_cat) {
    var subs = subscriptions_cache;
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

    subscriptions_cache = _.sortBy(subs, function (s) {
      return s.sort_index;
    });
    ajax.spost('/api/subs/sort', save_data);
  }

  function get_search_result (q, limit, cb) {
    var subs = [],
        count = 0,
        grouped = parse_subs(subscriptions_cache);
    _.each(grouped, function (group) {
      _.each(group.subs, function (sub) {
        if((!q || sub.title_l.indexOf(q) !== -1) && count < limit) {
          if(sub.total) {
            subs.push(sub);
            count++;
          }
        }
      });
    });
    if(q.length > 1) {
      limit = Math.max(17 - subs.length, 10);
      ajax.sget('/api/search?q=' + q + "&limit=" + limit, function (feeds) {
        feeds = _.map(feeds, transform_item());
        feeds_cache['search_result'] = feeds;
        cb({subs: subs, feeds: feeds});
      });
    } else {
      cb({subs: subs});
    }
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      get_feed: get_feed,
      get_feeds: get_feeds,
      get_final_link: get_final_link,
      get_search_result: get_search_result,
      get_subscription: get_subscription,
      get_user_subs: get_user_subs,
      get_welcome_list: get_welcome_list,
      is_user_has_subscription: is_user_has_subscription,
      mark_as_read: mark_as_read,
      save_vote: save_vote,
      unsubscribe: unsubscribe,
      update_sort_order: update_sort_order,
      user_settings: user_settings
    }
  });

  if(window.localStorage) {   // load data from localStorage
    cache_fixer = JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
  }
})();
