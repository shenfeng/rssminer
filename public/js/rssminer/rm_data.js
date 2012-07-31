(function (undefined) {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util,
      call_if_fn = util.call_if_fn;

  var user_data = (_RM_ && _RM_.user) || {},
      user_conf = JSON.parse(user_data.conf || "{}");

  var subscriptions_cache,
      feeds_cache = {},
      sub_titles = {};          // use by transform_item

  var last_search_ajax;

  var MAX_PAGER = 9,
      WELCOME_MAX_PAGE = 7,
      h = screen.height,
      // works for 900, 800, 1080 screen (height)
      // per item 34.85 pixel, first feed to top 138px, 140 px for brower use
      PER_PAGE_FEEDS = Math.floor((h - 138 - 140) / 40),
      // PER_PAGE_FEEDS = Math.floor((screen.height - 138 - 140) / 32.9),
      // show search result count according to screen height
      SEARCH_RESUTL_COUNT = Math.min(Math.floor((h - 260) / 43), 17),
      LIKE_SCORE = user_data.like_score, // default 1
      NEUTRAL_SCORE =  user_data.neutral_score, // db default 0
      MIN_COUNT = 5;

  // how many pages does each section has
  var WELCOME_TABS = {recommend: 1, newest: 1, read: 1, voted: 1};
  var RECOMMEND_TAB = 'recommend',
      NEWEST_TAB = 'newest',
      OLDEST_TAB = 'oldest',
      READ_TAB = 'read',
      VOTED_TAB = 'voted';
  var SUB_TABS = [RECOMMEND_TAB, NEWEST_TAB, OLDEST_TAB, READ_TAB, VOTED_TAB];

  // --------------HELPER functions-------------
  function favicon_path (url) {
    var host = util.hostname(url),
        h = encodeURIComponent(host.split("").reverse().join(''));
    return  _RM_.static_server + '/fav?h=' + h;
  }

  function split_tags (tags) {
    if(tags) { return tags.split("; ").slice(0, 3); } // at most 3
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
    return {
      author: feed.author || util.hostname(feed.link),
      sub: sub_titles[rssid],    // use to show search result
      rssid: rssid,
      cls: feed_css_class(feed),
      user_like: feed.vote > 0,
      user_dislike: feed.vote < 0,
      date: date,
      href: feed_hash(section, feed.id, page, sort),
      id: feed.id,
      link: feed.link,
      tags: split_tags(feed.tags),
      title: $.trim(feed.title || feed.link)
    };
  }

  function default_sort (like, neutral) {
    if(like + neutral > MIN_COUNT) {
      return RECOMMEND_TAB;
    }
    return NEWEST_TAB;
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
  function gen_sub_titles () {
    sub_titles = {};
    _.each(subscriptions_cache, function (sub) {
      sub_titles[sub.id] = transorm_sub(sub);
    });
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
        gen_sub_titles();
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
          page: 'next',
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
        text: tab,
        selected: section === tab,
        href: welcome_tab_hash(tab, 1)
      };
    });

    ajax.get('/api/welcome?' + params, function (resp) {
      resp = resp || [];
      var feeds = _.map(resp, function (feed) {
        return transform_item(feed, page, 'score', section);
      });

      // feeds = _.filter(feeds, function (f) { return f.title; });
      cb({
        title: section + ' - Rssminer, an intelligent RSS reader',
        feeds: remove_duplicate_feed(feeds),
        pager: compute_welcome_paging(section, page, resp.length),
        sort: sort_data
      });
    });
  }


  function remove_duplicate_feed (feeds) {
    var results = [];
    _.each(feeds, function (f) {
      var included = _.any(results, function (r) {
        return r.title === f.title || r.link === f.link;
      });
      if(!included) { results.push(f); }
    });
    return results;
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
        total = resp.length === PER_PAGE_FEEDS;
      }
      var feeds = _.map(resp, function (feed) {
        return transform_item(feed, data.page, sort, data.section);
      });

      var sort_tabs = [];
      _.each(SUB_TABS, function (s) {
        sort_tabs.push({
          selected: !sort || s === sort,
          href: sub_hash(data.section, 1, s),
          text: s
        });
      });

      data.cb({
        title: data.title,
        url: data.url,
        feeds: remove_duplicate_feed(feeds),
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
            gen_sub_titles();
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
      window.setTimeout(function () {
        var id = data.rss_link_id;
        polling_rss_link(id, POLLING_INTERVAL, POLLING_TIMES,
                         fetcher_finished);
      }, 300);
      call_if_fn(added_cb);     // added successfully
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
    ajax.jpost('/api/settings', data, function () { call_if_fn(cb); });
  }

  function fetch_search_result (q, limit, cb) {
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
        var feeds = _.map(resp, function (feed) {
          // no dedicated url and page, since I can just click the search box,
          // and get the result again
          return transform_item(feed, 1, NEWEST_TAB);
        });
        feeds = remove_duplicate_feed(feeds);
        cb({subs: subs, feeds: feeds, sub_cnt: subs.length});
      });
    } else {
      cb({subs: subs, sub_cnt: subs.length});
    }
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
    });
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

  function fetch_feed (id, cb) {
    ajax.get('/api/feeds/' + id, function (feed) {
      var t = transform_item(feed);
      t.summary = feed.summary;
      cb(t);
    });
  }

  window.RM = $.extend(window.RM, {
    data: {
      add_subscription: add_subscription,
      fetch_group_feeds: fetch_group_feeds,
      fetch_search_result: fetch_search_result,
      fetch_sub_feeds: fetch_sub_feeds,
      fetch_welcome: fetch_welcome,
      fetch_feed: fetch_feed,
      get_subscription: get_subscription,
      get_subscriptions: function () { return subscriptions_cache || []; },
      get_user_subs: get_user_subs,
      list_folder_names: list_folder_names,
      mark_as_read: mark_as_read,
      save_settings: save_settings,
      save_vote: save_vote,
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
