(function (undefined) {
  "use strict";
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util,
      call_if_fn = util.call_if_fn;

  var user_data = (_RM_ && _RM_.user) || {},
      user_conf = JSON.parse(user_data.conf || "{}");

  var subscriptions_cache,
      summary_cache = util.LRUCache(50);

  var last_search_ajax;

  var MAX_PAGER = 9,
      WELCOME_MAX_PAGE = 7,
      SEPERATOR = ';',
      PER_PAGE_FEEDS,
      SEARCH_PAGE_SIZE,
      SEARCH_RESUTL_COUNT,
      LIKE_SCORE = user_data.like_score, // default 1
      NEUTRAL_SCORE =  user_data.neutral_score, // db default 0
      MIN_COUNT = 5;

  function count_count () {
    var _h = $(window).height();
    // per item 40 pixel, first feed top: 126px, pager 24px, bottom 15px
    PER_PAGE_FEEDS = Math.floor((_h - 137 - 24 - 15) / 40);
    SEARCH_PAGE_SIZE = PER_PAGE_FEEDS - 3;
    SEARCH_RESUTL_COUNT = Math.min(Math.floor((_h - 230) / 40), 17);
  }

  $(window).resize(count_count);
  count_count();

  // how many pages does each section has
  var WELCOME_TABS = {recommend: 1, newest: 1, read: 1, voted: 1};

  var RECOMMEND_TAB = 'recommend',
      NEWEST_TAB = 'newest',
      OLDEST_TAB = 'oldest',
      READ_TAB = 'read',
      RCMD_TIP = _LANG_ZH_? '依据您以往的阅读，和你喜欢过的文章，精心为您推荐': 'Sorting feeds by leaning from your reading and voting history',
      VOTED_TAB = 'voted';
  var SUB_TABS = [RECOMMEND_TAB, NEWEST_TAB, OLDEST_TAB, READ_TAB, VOTED_TAB];


  // --------------HELPER functions-------------
  function favicon_path (url) {
    var host = util.hostname(url),
        h = encodeURIComponent(host.split("").reverse().join(''));
    return  _RM_.static_server + '/fav?h=' + h;
  }

  function split_tags (tags) {
    if(tags) { return tags.split(/;\s?/).slice(0, 4); } // at most 4
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
    var rssid = feed.rssid;
    section = section || rssid;
    var date = ymdate(feed.pts); // publish ts
    if(section === READ_TAB || sort === READ_TAB) {
      date = ymdate(feed.readts);
    } else if(section === VOTED_TAB || sort === VOTED_TAB) {
      date = ymdate(feed.votets);
    }
    var title = $.trim(feed.title || feed.link),
        sub_title = get_subscription(feed.rssid).title,
        author = sub_title;
    if(feed.author) {
      author = feed.author + '@' + author;
    }
    return {
      tooltip: util.tooltip(author, 24),
      author: author,
      sub: sub_title[rssid],    // use to show search result
      rssid: rssid,
      cls: feed_css_class(feed),
      user_like: feed.vote > 0,
      user_dislike: feed.vote < 0,
      date: date,
      href: feed_hash(section, feed.id, page, sort),
      id: feed.id,
      link: feed.link,
      link_d: decodeURI(feed.link),
      tags: split_tags(feed.tags),
      title: title,
      title_booltip: util.tooltip(title, 70)
    };
  }

  function default_sort (like, neutral) {
    // default show newest. 3 place need to change
    if(user_conf.pref_sort === RECOMMEND_TAB && like + neutral > MIN_COUNT) {
      return RECOMMEND_TAB;
    }
    return NEWEST_TAB;
  }

  function transorm_sub (sub) {
    // the url is site's alternate url
    var title = sub.title || sub.url || '',
        img = null,
        url = sub.url || '';
    if(url.indexOf('http') === 0) { img = favicon_path(url); }
    return {
      img: img,
      title: title,
      tooltip: util.tooltip(title, 30),
      link: sub.url,
      group: sub.group,
      title_l: title.toLowerCase(),
      cls: sub.like > 0 ? 'has-like' : 'no-like',
      // sort by likest if has likest
      href: sub_hash(sub.id, 1, default_sort(sub.like, sub.neutral)),
      like: sub.like,
      total: sub.total,
      index: sub.index,
      neutral: sub.neutral,
      id: sub.id        // rss_link_id
    };
  }

  function get_first_group () {
    var grouped = parse_subs(subscriptions_cache);
    if(grouped && grouped.length) {
      return grouped[0].group.name;
    }
    return null;
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
      var like = _.reduce(list, function (m, sub) { return sub.like + m; }, 0),
          neutral = _.reduce(list, function (m, sub) { return sub.neutral + m; }, 0),
          total = _.reduce(list, function (m, sub) { return sub.total + m; }, 0),
          hash = sub_hash('f_' + group, 1, default_sort(like, neutral));
      if(list.length) {
        result.push({
          group: {
            name: group, like: like, neutral: neutral,
            hash: hash, total: total
          },
          subs: list,
          collapse: _.include(collapsed, group)
        });
      }
    }
    result = _.sortBy(result, function (i) { return i.subs[0].index; });
    return result;
  }

  function sub_hash (id, page, sort) {
    var href = 'read/' + id;
    if(page) { href = href + '?p='+ page; }
    if(sort) { href = href + "&s=" + sort; }
    return href;
  }

  function welcome_tab_hash (section, page) { // welcome page tab
    return '?s=' + section + '&p=' + page;
  }

  function feed_hash (sub, id, page, sort) {
    var h = 'read/' + sub + "/" + id;
    if(page) { h += '?p=' + page + '&s=' + sort; }
    return h;
  }
  // helper funciton end here
  // -------------------------------------------


  // API
  function get_user_subs (cb) {
    if(subscriptions_cache) {
      cb(parse_subs(subscriptions_cache));
    } else {
      ajax.get('/api/subs', function (resp) {
        try_sync_with_storage(resp);
        // exclude subscription that has no title
        subscriptions_cache = _.filter(resp, function (s) { return s.title; });
        get_user_subs(cb);
      });
    }
  }

  function compute_welcome_paging (section, page, last_count) {
    var has_more = last_count === PER_PAGE_FEEDS; // maybe has more
    if(page >= WELCOME_MAX_PAGE) {
      has_more = false;         // do not show too much
    }
    if(has_more) {      // how many pages
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
          href: welcome_tab_hash(section, i)
        });
      }
      if(has_more) {
        pages.push({
          page: _LANG_('next'),
          current: false,
          href: welcome_tab_hash(section, page + 1)
        });
      }
      return {
        pages: pages,
        has_more: has_more
      };
    }
  }

  // sort is not used, just the same api as fetch_sub, fetch_folder
  function fetch_welcome (section, page, sort, cb) { // no cache
    if(page > WELCOME_MAX_PAGE) { cb({}); return; }          // do not load
    var params = util.params({
      section: section,
      limit: PER_PAGE_FEEDS,
      offset: Math.max(0, page-1) * PER_PAGE_FEEDS
    });
    var sort_data = _.map(_.keys(WELCOME_TABS), function (tab) {
      return {
        text: _LANG_(tab),
        selected: section === tab,
        href: welcome_tab_hash(tab, 1),
        tip: tab === RECOMMEND_TAB ? RCMD_TIP : ''
      };
    });

    ajax.get('/api/welcome?' + params, function (resp) {
      resp = resp || [];
      var feeds = _.map(resp.feeds, function (feed) {
        return transform_item(feed, page, 'score', section);
      });

      // feeds = _.filter(feeds, function (f) { return f.title; });
      cb({
        title: section + ' - Rssminer, an intelligent RSS reader',
        feeds: feeds,
        pager: compute_welcome_paging(section, page, resp.count),
        sort: sort_data
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


  function mark_as_read (feedid, cb) {
    ajax.spost('/api/feeds/' + feedid + '/read', function () {
      var s = summary_cache.get(feedid);
      if(s && s[feedid]) {
        s[feedid].readts = Math.floor(new Date().getTime() / 1000);
      }
      call_if_fn(cb);
    });
  }

  function save_vote (feedid, vote, cb) {
    ajax.spost('/api/feeds/' + feedid  + '/vote', {vote: vote}, function () {
      call_if_fn(cb);
    });
  }


  function get_subids_for_group (group) {
    var grouped = parse_subs(subscriptions_cache);
    var result = _.find(grouped, function (g) { return g.group.name === group; });
    return result;
  }

  function compute_sub_paging (subid, sorting, total, page) {
    var pages = [],
        count = page,
        has_more = false;
    if(total === false) {
      if(page === 1) {
        return false;
      }
    } else if(total === true) {
      has_more = true;
    } else if (total <= PER_PAGE_FEEDS) {
      return false;
    } else {
      count = Math.ceil(total / PER_PAGE_FEEDS);
    }
    for(var i = 1; i < count + 1; i++) {
      if(should_include(count, page, i)) {
        pages.push({
          page: i,
          current: i === page,
          href: sub_hash(subid, i, sorting)
        });
      }
    }
    if(has_more) {
      pages.push({
        page: 'next',
        current: false,
        href: sub_hash(subid, i, sorting)
      });
    }
    return {
      count: count,
      page: page,
      has_more : page <= count,
      pages: pages
      // prev: page > 0,         // has prev page
      // next: count > page      // has next page
    };
  }

  function fetch_feeds (data) {
    var sort = data.sort;
    var offset = Math.max(0, data.page - 1) * PER_PAGE_FEEDS;
    var url = '/api/subs/' + data.id + '?' + util.params({
      offset: offset,
      limit: PER_PAGE_FEEDS,
      sort: sort
    });

    ajax.get(url, function (resp) {
      var sub_group = data.sub_group;
      var total = sub_group.like + sub_group.neutral; // recommand
      if(sort === NEWEST_TAB || data.sort === OLDEST_TAB) {
        total = sub_group.total;
      } else if(sort === READ_TAB || sort === VOTED_TAB) {
        total = resp.count === PER_PAGE_FEEDS;
      }
      var feeds = _.map(resp.feeds, function (feed) {
        return transform_item(feed, data.page, sort, data.section);
      });

      var sort_tabs = _.map(SUB_TABS, function (s) {
        return {
          selected: !sort || s === sort,
          href: sub_hash(data.section, 1, s),
          text: _LANG_(s),
          tip: s === RECOMMEND_TAB ? RCMD_TIP : ''
        };
      });

      data.cb({
        title: data.title,
        url: data.url,
        feeds: feeds,
        sort: sort_tabs,
        pager: compute_sub_paging(data.section, sort, total, data.page)
      });
    });
  }

  function fetch_group_feeds (group, page, sort, cb) {
    var grouped = get_subids_for_group(group);
    var ids = _.pluck(grouped.subs, 'id').sort(function (a, b) {
      return a - b;             // asc sorting
    });
    fetch_feeds({
      title: sort + " - " + group + ' [folder]',
      cb: cb,
      id: ids.join('-'),
      section: 'f_' + group,
      sub_group: grouped.group,
      page: page,
      sort: sort
    });
  }

  function fetch_sub_feeds (subid, page, sort, cb) {
    var sub =_.find(subscriptions_cache, function (s) {
      return s.id === subid;
    }) || {};
    fetch_feeds({
      title: sort + ' - ' + sub.title,
      cb: cb,
      id: subid,
      section: subid,
      sub_group: sub,
      page: page,
      sort: sort,
      url: sub.url
    });
  }

  function polling_rss_link (rss_link_id, interval, times, cb) {
    if(times > 0) {
      ajax.sget('/api/subs/p/' + rss_link_id, function (sub) {
        // TODO refetch user subs
        if(sub && sub.title) {  // ok, title is fetched
          sub.group = get_first_group(); // server return no group_name
          var find = _.find(subscriptions_cache, function (s) {
            return s.id === sub.id;
          });
          if(!find) {
            subscriptions_cache.push(sub);
            sub.refresh = true;
          }
          call_if_fn(cb, sub);  // fetcher successfully
        } else {                // fetch again
          interval += 400;
          times -= 1;
          window.setTimeout(function () {
            polling_rss_link(rss_link_id, interval, times, cb);
          }, interval);
        }
      });
    } else {
      call_if_fn(cb);           // fetcher timeout
    }
  }

  function add_subscription (url, added_cb, fetcher_finished) {
    var POLLING_INTERVAL = 1500,
        POLLING_TIMES = 4;
    ajax.jpost('/api/subs/add' , {link: url, g: get_first_group() }, function (data) {
      if(data.rss_link_id) {
        window.setTimeout(function () {
          var id = data.rss_link_id;
          polling_rss_link(id, POLLING_INTERVAL, POLLING_TIMES,
                           fetcher_finished);
        }, 300);
      }
      call_if_fn(added_cb, data);     // added successfully
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
      call_if_fn(cb);
    });
  }

  function save_settings (data, cb) {
    if(data.pref_sort) {
      user_conf.pref_sort = data.pref_sort;
    }
    ajax.jpost('/api/settings', data, function () { call_if_fn(cb); });
  }

  function hight_search (str, keywords) {
    _.each(keywords, function (k) {
      str = str.replace(new RegExp(k, 'ig'), function (m) {
        return "<b>" + m + "</b>";
      });
    });
    return str;
  }

  function instant_search (q, cb) {
    var subs = [],
        count = 0,
        grouped = parse_subs(subscriptions_cache);
    _.each(grouped, function (group) {
      var g = group.group;
      // if q is empty, indexOf return 0
      if(g.name.indexOf(q) !== -1 && count < SEARCH_RESUTL_COUNT) {
        if(g.total) {
          g = _.clone(g);
          g.is_group = true;
          g.href = g.hash;
          g.title = hight_search(g.name, [q]);
          subs.push(g);
          count++;
        }
      }
      _.each(group.subs, function (sub) {
        if(sub.title_l.indexOf(q) !== -1 && count < SEARCH_RESUTL_COUNT) {
          if(sub.total) {
            sub = _.clone(sub);
            sub.title = hight_search(sub.title, [q]);
            subs.push(sub);
            count++;
          }
        }
      });
    });
    if(q.length > 1 && subs.length < SEARCH_RESUTL_COUNT) {
      var limit = SEARCH_RESUTL_COUNT - subs.length;
      if(last_search_ajax) { last_search_ajax.abort(); }
      var url = '/api/search?q=' + q + "&limit=" + limit;
      last_search_ajax = ajax.sget(url, function (resp) {
        last_search_ajax = undefined;
        transform_searched_feeds(resp, q);
        cb({subs: subs, server: resp, sub_cnt: subs.length, q: q});
      });
    } else {
      cb({subs: subs, sub_cnt: subs.length, q: q});
    }
  }

  function transform_searched_feeds (resp, q) {
    resp.feeds = _.map(resp.feeds, function (feed) {
      feed = transform_item(feed, 1, NEWEST_TAB);
      feed.title_h = hight_search(feed.title, [q]);
      return feed;
    });
  }

  function filter_hash (tags, authors, q, offset) {
    var filter = "";
    if(q !== undefined) {
      filter += "search?q=" + q + "&";
    }
    filter += ("tags=" + tags.join(SEPERATOR) + "&authors=" + authors.join(SEPERATOR));
    if(offset !== undefined) {
      filter += ("&offset=" + offset);
    }
    return filter;
  }

  function fetch_search (q, tags, authors, offset, fs, cb) {
    var url = "/api/search?" + util.params({
      q: q, fs: fs ? 1 : '', limit: SEARCH_PAGE_SIZE,
      tags: tags, offset: offset, authors: authors
    });
    tags = _.filter(tags.split(SEPERATOR), _.identity);
    authors = _.filter(authors.split(SEPERATOR), _.identity);
    ajax.get(url, function (resp) {
      transform_searched_feeds(resp, q);
      resp.authors = _.map(resp.authors, function (val, key, map) {
        var idx = _.indexOf(authors, key),
            tmp = _.clone(authors);
        if(idx === -1) { tmp.push(key); } else {
          tmp = _.filter(tmp, function (t) { return t !== key; });
        }
        var filter = filter_hash(tags, tmp);
        return {author: key, count: val, filter: filter, selected: idx > -1};
      });
      resp.tags = _.map(resp.tags, function (val, key, map) {
        var idx = _.indexOf(tags, key),
            tmp = _.clone(tags);
        if(idx === -1) { tmp.push(key); } else {
          tmp = _.filter(tmp, function (t) { return t !== key; });
        }
        var filter = filter_hash(tmp, authors);
        return {tag: key, count: val, filter: filter, selected: idx > -1};
      });
      var pages = [], os = 0;
      for(var i = 0; i < 7; i++) {
        if(resp.total > SEARCH_PAGE_SIZE && resp.total >= (os + 1)) {
          var page = {page: (i + 1), href: filter_hash(tags, authors, q, os)};
          if(os === offset) { page.current = true; }
          pages.push(page);
          os += SEARCH_PAGE_SIZE;
        } else {
          break;
        }
      }
      if(pages.length) { resp.pager = {pager: 1, pages: pages}; }
      cb(resp);
    });
  }

  function update_subscrptions (subscriptions, data) {
    var index = 1;
    _.each(data, function (group) {
      _.each(group.ids, function (id) {
        var s = _.find(subscriptions, function (sub) { return id === sub.id;});
        if(s) {
          s.sort_index = index;
          index += 1;
          s.group = group.g;
        }
      });
    });
  }

  function get_subscription (subid) {
    subid = parseInt(subid);
    var sub = _.find(subscriptions_cache, function (sub) {
      return subid === sub.id;
    }) || {};
    sub.group = sub.group || 'null';
    return transorm_sub(sub);
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

  function _handle_resp (ids, cached, fetched, cb) {
    _.each(fetched, function (f) { summary_cache.put(f.id, f); });
    var results = _.map(ids, function (id) {
      var feed = cached[id] || _.find(fetched, function (f) { return f.id === id; });
      feed = _.clone(feed);
      var t = transform_item(feed);
      t.summary = feed.summary;
      if(feed.readts) {
        t.rdate = ymdate(feed.readts);
      }
      t.sub = get_subscription(t.rssid);
      return t;
    });
    cb(results);
  }

  function fetch_summary (ids, cb) {
    var cached = summary_cache.get(ids),
        fetch_ids = _.filter(ids, function (id) { return !(id in cached); });
    // console.log('cached: ', _.keys(cached), cached);
    if(fetch_ids.length) {
      var url = '/api/feeds/' + fetch_ids.join('-');
      ajax.get(url, function (fetched) { // array list of feeds
        _handle_resp(ids, cached, fetched, cb);
      }, fetch_ids.length < 3);         // less than 3, silent
    } else {
      // console.log("-----------------------done----------------");
      _handle_resp(ids, cached, [], cb);
    }
  }

  function save_reading_times (data, cb) {
    ajax.jpost('/api/feeds', data, function () {call_if_fn(cb);});
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      fetch_group_feeds: fetch_group_feeds,
      fetch_search: fetch_search,
      fetch_sub_feeds: fetch_sub_feeds,
      fetch_summary: fetch_summary,
      fetch_welcome: fetch_welcome,
      get_subscription: get_subscription,
      get_subscriptions: function () { return subscriptions_cache || []; },
      get_user_subs: get_user_subs,
      instant_search: instant_search,
      list_folder_names: list_folder_names,
      mark_as_read: mark_as_read,
      save_reading_times: save_reading_times,
      save_settings: save_settings,
      save_vote: save_vote,
      user_conf: user_conf,
      unsubscribe: unsubscribe
    }
  });

  $(RM).bind('sub-sorted.rm',function (e, data) {
    update_subscrptions(subscriptions_cache, data);
  });

  function try_sync_with_storage (subscriptions) {
    if(localStorage) {
      var data = JSON.parse(localStorage.getItem('__sort__'));
      if(data) {
        update_subscrptions(subscriptions, data);
      }
    }
  }
})();
