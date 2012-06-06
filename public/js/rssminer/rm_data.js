(function () {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util,
      call_if_fn = util.call_if_fn;

  var user_data = (_RM_ && _RM_.user) || {},
      user_conf = JSON.parse(user_data.conf || "{}");

  var subscriptions_cache,
      sub_titles = {},          // use by transform_item
      feeds_cache = {},
      cache_fixer = {};         // fix browser cache, inconsistency

  var STORAGE_KEY = '_rm_',
      MAX_PAGER = 9,
      WELCOME_MAX_PAGE = 7,
      // per item 29 pixel, first feed to top 138px, 140 px for brower use
      PER_PAGE_FEEDS = Math.floor((screen.height - 138 - 140) / 30),
      // show search result count according to screen height
      SEARCH_RESUTL_COUNT = Math.min(Math.floor((screen.height - 260) / 43), 17),
      LIKE_SCORE = user_data.like_score,        // default 1
      NEUTRAL_SCORE =  user_data.neutral_score;    // db default 0

  // how many pages does each section has
  var WELCOME_TABS = {recommand: 1, latest: 1, read: 1, voted: 1};

  var SORTINGS_TABS = { likest: 1, newest: 1, oldest: 1 }; // 1 means true

  function save_to_cache_fixer (feedid, data) {
    cache_fixer[feedid] = _.extend(cache_fixer[feedid] || {}, data);
    if(window.localStorage) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cache_fixer));
    }
  }

  function get_subscription (subid) {
    subid = parseInt(subid);
    var sub = _.find(subscriptions_cache, function (sub) {
      return subid === sub.id;
    });
    sub.group = sub.group || 'null';
    return transorm_sub(sub);
  }

  function get_feed (feedid) {
    var feed;
    feedid = parseInt(feedid);
    for(var key in feeds_cache) {
      var feeds = feeds_cache[key];
      feed = _.find(feeds, function (f) {
        return f.id === feedid;
      });
      if(feed) { break; }
    }
    return feed ? transform_item(feed) : {};
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
    return  _RM_.static_server + '/fav?h=' + h;
  }

  function split_tag (tags) {
    if(tags) { return sub_array(tags.split("; "), 0, 3); } // at most 3
    else { return []; }
  }

  function feed_css_class (i) {
    var cls;
    if(i.readts === 1) { cls = 'sys-read'; }
    else if(i.readts > 1) { cls = 'read';}
    else {cls = 'unread';}

    if (i.vote < 0) { cls += ' dislike'; }
    else if (i.vote > 0) { cls += ' like'; }
    else if(i.score > LIKE_SCORE) { cls += ' like sys'; }
    // score === 0 means server give no score info
    else if(i.score > NEUTRAL_SCORE || i.score === 0) {
      cls +=' neutral sys';
    } else { cls += ' dislike sys';}

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

  function transform_item (feed, page, sort, section) {
    var cf = cache_fixer[feed.id],
        rssid = feed.rssid;
    section = section || rssid;
    if(cf) {
      // try to fix outdated data, browser cache 1 hour
      feed.readts = 'readts' in cf ? cf.readts : feed.readts;
      feed.vote = 'vote' in cf ? cf.vote : feed.vote;
    }
    var info = [];              // used in context menu
    if(feed.read > 1) {
      info.push({text: 'You read it in ' + ymdate(feed.read)});
    }
    return {
      author: feed.author || util.hostname(feed.link),
      sub: sub_titles[rssid],    // use to show search result
      rssid: rssid,
      cls: feed_css_class(feed),
      user_like: feed.vote > 0,
      user_dislike: feed.vote < 0,
      date: ymdate(feed.publishedts),
      href: feed_hash(section, feed.id, page, sort),
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
      group: sub.group,
      title_l: title.toLowerCase(),
      href: sub_hash(sub.id, 1, 'newest'),
      like: sub.like,
      total: sub.total,
      index: sub.index,
      neutral: sub.neutral,
      id: sub.id        // rss_link_id
    };
  }

  function parse_subs (subs) {
    var grouped = _.groupBy(subs, 'group'),
        result = [],
        collapsed = user_conf.nav || [];
    for(var group in grouped) {
      var list = _(grouped[group]).chain()
            .sortBy(function (i) { return i.index; })
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
        try_sync_with_storage(resp);
        var result = parse_subs(resp);
        subscriptions_cache = resp;
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
      feeds_cache[section] = resp; // cache unchanged
      var feeds = _.map(resp, function (feed) {
        var result = transform_item(feed, page, 'score', section);
        if(section === 'read') { // read show read date
          result.date = ymdate(feed.readts);
        } else if(section === 'voted') {
          result.date = ymdate(feed.votets);
        }
        return result;
      });
      cb({
        title: section + ' - Rssminer, an intelligent RSS reader',
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

  function feed_hash (sub, id, page, sort) {
    var h = 'read/' + sub + "/" + id;
    if(page) { h += '?p=' + page + '&s=' + sort; }
    return h;
  }

  function get_feeds (subid, page, sort, cb) {
    var sub =_.find(subscriptions_cache, function (sub) {
      return sub.id === subid;
    }),
        total = sub ? sub.total : 0,
        offset = Math.max(0, page -1) * PER_PAGE_FEEDS;

    sort = SORTINGS_TABS[sort] ? sort : 'newest';
    var url = '/api/subs/' + subid + '?' + util.params({
      offset: offset,
      limit: PER_PAGE_FEEDS,
      sort: sort
    });
    ajax.get(url, function (resp) {
      feeds_cache['current_sub'] = resp;
      var feeds = _.map(resp, function (feed) {
        return transform_item(feed, page, sort);
      }),
          sort_data = [];
      for(var s in SORTINGS_TABS) {
        sort_data.push({
          selected: !sort || s === sort,
          href: sub_hash(subid, 1, s),
          text: s
        });
      }
      cb({
        title: sub.title,
        url: sub.url,
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
        pages: pages,
        prev: page > 1,         // has prev page
        next: count > page      // has next page
      };
    }
  }

  function mark_as_read (feedid, cb) {
    ajax.spost('/api/feeds/' + feedid + '/read', function () {
      save_to_cache_fixer(feedid, {
        readts: Math.round(new Date().getTime() / 1000)
      });
      call_if_fn(cb);
    });
  }

  function save_vote (feedid, vote, cb) {
    ajax.spost('/api/feeds/' + feedid  + '/vote', {vote: vote}, function () {
      save_to_cache_fixer(feedid, {vote: vote});
      call_if_fn(cb);
    });
  }

  function polling_rss_link (rss_link_id, interval, times, cb) {
    if(times > 0) {
      ajax.sget('/api/subs/p/' + rss_link_id, function (sub) {
        // TODO refetch user subs
        if(sub && sub.title) {  // ok, title is fetched
          sub.group = null; // server return no group_name
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
    id = parseInt(id);
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

  var last_search_ajax;
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
      if(last_search_ajax) { last_search_ajax.abort(); }
      var url = '/api/search?q=' + q + "&limit=" + limit;
      last_search_ajax = ajax.sget(url, function (resp) {
        last_search_ajax = undefined;
        feeds_cache['search_result'] = resp; // cache unchanged
        var feeds = _.map(resp, function (feed) {
          // no dedicated url and page, since I can just click the search box,
          // and get the result again
          return transform_item(feed, 1, 'score');
        });
        cb({subs: subs, feeds: feeds, sub_cnt: subs.length});
      });
    } else {
      cb({subs: subs, sub_cnt: subs.length});
    }
  }

  function try_sync_with_storage (subscriptions) {
    if(localStorage) {
      var data = JSON.parse(localStorage.getItem('__sort__'));
      if(data) {
        update_subscrption(subscriptions, data);
      }
    }
  }

  function update_subscrption (subscriptions, data) {
    var index = 1;
    _.each(data, function (group) {
      _.each(group.ids, function (id) {
        var s = _.find(subscriptions, function (sub) { return id === sub.id;});
        s.sort_index = index;
        index += 1;
        s.group = group.g;
      });
    });
  }

  function list_folder_names (subid) {
    var names = {},
        me;
    _.each(subscriptions_cache, function (sub) {
      names[sub.group] = true;
      if(sub.id === subid) { me = sub.group; }
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
      save_settings: save_settings
    }
  });

  $(RM).bind('sub-sorted.rm',function (e, data) {
    update_subscrption(subscriptions_cache, data);
  });

  if(window.localStorage) {   // load data from localStorage
    cache_fixer = JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
  }
})();
