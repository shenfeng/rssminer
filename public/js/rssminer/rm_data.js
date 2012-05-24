(function () {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util,
      call_if_fn = util.call_if_fn;

  var user_data = (_RM_ && _RM_.user) || {},
      user_conf = user_data.conf || {};

  var subscriptions_cache,
      sub_titles = {},                // use by transform_item
      feeds_cache = {},
      cache_fixer = {};         // fix browser cache, inconsistency

  var STORAGE_KEY = '_rm_',
      MAX_PAGER = 9,
      WELCOME_MAX_PAGE = 7,
      STATIC_SERVER = window._RM_.static_server,
      // per item 29 pixel, first feed to top 138px, 140 px for brower use
      PER_PAGE_FEEDS = Math.floor((screen.height - 138 - 140) / 30),
      // show search result count according to screen height
      SEARCH_RESUTL_COUNT = Math.min(Math.floor((screen.height - 260) / 43), 17),
      LIKE_SCORE = 1,           // default 1
      NEUTRAL_SCORE =  0;       // db default 0

  var __scores = user_data.scores;
  if(__scores) {
    LIKE_SCORE = parseFloat(__scores.split(',')[0]);
    NEUTRAL_SCORE = parseFloat(__scores.split(',')[1]);
  }

  // how many pages does each section has
  var WELCOME_TABS = {recommand: 1, latest: 1, read: 1, voted: 1};

  var SORTINGS_TABS = { newest: 1, oldest: 1, likest: 1 }; // 1 means true

  function save_to_cache_fixer (feedid, data) {
    cache_fixer[feedid] = _.extend(data, cache_fixer[feedid]);
    if(window.localStorage) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cache_fixer));
    }
  }

  function user_settings () {
    var expire_times = [];
    for(var i = 15; i <= 60; i += 15) {
      expire_times.push({time: i, selected: i === (user_conf.expire || 30)});
    }
    return {
      expire_times: expire_times,
      groups: parse_subs(subscriptions_cache)
    };
  }

  function get_subscription (subid) {
    var sub = _.find(subscriptions_cache, function (sub) {
      return subid === sub.id;
    });
    sub.group_name = sub.group_name || 'null';
    return sub;
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

  function sub_array (list, offset, length) {
    var result = [];
    if(list.length) {
      for(var i = offset; i < list.length && i < offset + length; i++) {
        result.push(list[i]);
      }
    }
    return result;
  }

  function favicon_path (url) {
    var host = util.hostname(url),
        h = encodeURIComponent(host.split("").reverse().join(''));
    return STATIC_SERVER + '/fav?h=' + h;
  }

  function split_tag (tags) {
    if(tags) { return sub_array(tags.split("; "), 0, 3); } // at most 3
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

  function ymdate (i) {
    var d = new Date(i * 1000),
        m = d.getMonth() + 1,
        day = d.getDate();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  }

  function transform_item (feed) {
    var cf = cache_fixer[feed.id],
        sub_id = feed.rss_link_id;
    if(cf) {
      // try to fix outdated data, browser cache 1 hour
      feed.read_date = cf.read_date || feed.read_date;
      feed.vote_user = cf.vote_user || feed.vote_user;
    }
    return {
      author: feed.author || util.hostname(feed.link),
      sub: sub_titles[sub_id],    // use to show search result
      cls: feed_css_class(feed),
      date: ymdate(feed.published_ts),
      href: feed_hash(sub_id, feed.id),
      id: feed.id,
      link: feed.link,
      tags: split_tag(feed.tags),
      title: feed.title
    };
  }

  function transorm_sub (sub) {
    // the url is site's alternate url
    var title = sub.title || sub.url || '';
    return {
      img: favicon_path(sub.url),
      title: title,
      link: sub.url,
      group: sub.group_name,
      title_l: title.toLowerCase(),
      href: sub_hash(sub.id, 1, 'newest'),
      like: sub.like_c,
      total: sub.total_feeds,
      index: sub.sort_index,
      dislike: sub.dislike_c,
      neutral: sub.total_c - sub.like_c - sub.dislike_c,
      id: sub.id        // rss_link_id
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

  function gen_sub_titles () {
    sub_titles = {};
    _.each(subscriptions_cache, function (sub) {
      sub_titles[sub.id] = transorm_sub(sub);
    });
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
        gen_sub_titles();
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
    var sort_data = _.map(_.keys(WELCOME_TABS), function (tab) {
      return {
        text: tab,
        selected: section === tab,
        href: tab_hash(tab, 1)
      };
    });

    ajax.get('/api/welcome?' + params, function (resp) {
      resp = resp || [];
      var feeds = _.map(resp, function (feed) {
        var result = transform_item(feed);
        result.href = feed_hash(section, feed.id); // change href
        if(section === 'read') { // read show read date
          result.date = ymdate(feed.read_date);
        }
        return result;
      });
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

  function tab_hash (section, page) { // welcome page tab
    return '?s=' + section + '&p=' + page;
  }

  function feed_hash (sub, id) {
    return 'read/' + sub + "/" + id;
  }

  function get_feeds (subid, page, sort, cb) {
    var sub =_.find(subscriptions_cache, function (sub) {
      return sub.id === subid;
    }),
        total = sub ? sub.total_feeds : 0,
        offset = Math.max(0, page -1) * PER_PAGE_FEEDS;

    sort = SORTINGS_TABS[sort] ? sort : 'newest';
    var url = '/api/subs/' + subid + '?' + util.params({
      offset: offset,
      limit: PER_PAGE_FEEDS,
      sort: sort
    });
    ajax.get(url, function (resp) {
      var feeds =  _.map(resp, transform_item),
          sort_data = [];
      feeds_cache['current_sub'] = feeds;
      for(var s in SORTINGS_TABS) {
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
      WELCOME_TABS[section] = Math.max(WELCOME_TABS[section], page + 1);
    }
    if(page === 1 && !has_more) {
      return false;
    } else {
      var pages = [];
      for(var i = 1; i <= WELCOME_TABS[section]; i++) {
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

  function mark_as_read (feedid, cb) {
    ajax.spost('/api/feeds/' + feedid + '/read', function () {
      save_to_cache_fixer(feedid, {
        read_date: Math.round(new Date().getTime() / 1000)
      });
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
          var find = _.find(subscriptions_cache, function (s) {
            return s.id === sub.id;
          });
          if(!find) {
            subscriptions_cache.push(sub);
            gen_sub_titles();
            sub.refresh = true;
          }
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
    var POLLING_INTERVAL = 3000,
        POLLING_TIMES = 4;

    ajax.jpost('/api/subs/add' , {link: url}, function (data) {
      window.setTimeout(function () {
        var id = data.rss_link_id;
        polling_rss_link(id, POLLING_INTERVAL, POLLING_TIMES,
                         fetcher_finished);
      }, 1000);
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
      gen_sub_titles();
      call_if_fn(cb);
    });
  }

  function save_settings (data, cb) {
    ajax.jpost('/api/settings', data, function () {
      call_if_fn(cb);
    });
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
      limit = Math.max(SEARCH_RESUTL_COUNT - subs.length, 10);
      ajax.sget('/api/search?q=' + q + "&limit=" + limit, function (feeds) {
        feeds = _.map(feeds, transform_item);
        feeds_cache['search_result'] = feeds;
        cb({subs: subs, feeds: feeds, sub_cnt: subs.length});
      });
    } else {
      cb({subs: subs, sub_cnt: subs.length});
    }
  }

  function list_folder_names (subid) {
    var names = {},
        me;
    _.each(subscriptions_cache, function (sub) {
      names[sub.group_name] = true;
      if(sub.id === subid) { me = sub.group_name; }
    });
    if(me === null) { me = 'null'; } // map null => null
    var result = [];
    _.each(_.keys(names), function (name) {
      result.push({name: name, selected: me === name});
    });
    return result;
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      get_feed: get_feed,
      get_feeds: get_feeds,
      get_search_result: get_search_result,
      get_subscription: get_subscription,
      get_subscriptions: function () { return subscriptions_cache || []; },
      get_user_subs: get_user_subs,
      get_welcome_list: get_welcome_list,
      mark_as_read: mark_as_read,
      save_vote: save_vote,
      list_folder_names: list_folder_names,
      unsubscribe: unsubscribe,
      user_settings: user_settings,
      save_settings: save_settings
    }
  });

  if(window.localStorage) {   // load data from localStorage
    cache_fixer = JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
  }
})();
