// functions to transform data
(function () {
  var utils = RM.util,
      user = _RM_ && _RM_.user;

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
    else {cls = 'unread' ;}

    if (i.vote < 0) { cls += ' dislike'; }
    else if (i.vote > 0) { cls += ' like'; }
    else { cls += ' neutral'; }
    return cls;
  }

  function imgPath (url) {
    var host = encodeURIComponent(utils.hostname(url));
    return _RM_.proxy_server + '/fav?h=' + host;
  }

  function parseSubs (subs) {
    var grouped = _.groupBy(subs, 'group_name'),
        result = [],
        collapsed = (user && user.conf && user.conf.nav) || [];
    for(var tag in grouped) {
      var list = _(grouped[tag]).chain()
            .sortBy(function (i) { return i.sort_index; })
            .map(function(i) {
              return {
                img: imgPath(i.url),
                title: i.title || i.o_title, // original title
                href: 'read/' + i.id,
                like: i.like_c,
                dislike: i.dislike_c,
                neutral: i.total_c - i.like_c - i.dislike_c,
                id: i.id
              };
            }).value();
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

  function parseWelcomeList (data) { return _.map(data, transformItem()); }

  function parseFeedList (subid, data) {
    if(typeof data === 'string') { data = JSON.parse(data); }
    var now = new Date().getTime() / 1000;
    _.each(data, function (e) {      // convert null to default
      // mark old enough (45d) as readed
      if(now - e.published_ts > 3600 * 24 * 45) e.read_date = 1;
      // -1 is db default value
      e.read_date = e.read_date === null ? -1 : e.read_date;
      e.vote_sys = e.vote_sys === null ? 1 : e.vote_sys;
      e.vote = e.vote === null ? 0 : e.vote;
    });

    var unread = _.filter(data, function (i) { return i.read_date <= 0;});
    var readed = _.filter(data, function (i) { return i.read_date > 0;});
    var cmp = by('vote', by('vote_sys',
                            by('published_ts', null, -1), -1), -1);
    readed.sort(cmp);
    unread.sort(cmp);
    data = unread.concat(readed);

    var result = _.map(data,transformItem(subid));
    return result;
  };

  var reseted = ["wordpress", "appspot", 'emacsblog',
                 "blogspot", 'mikemccandless'];

  var proxy_sites = ['google', "feedproxy"];       // X-Frame-Options

  function getFinalLink (link, feedid) {
    var h = utils.hostname(link);
    for(var i = 0; i < proxy_sites.length; i++) {
      if(h.indexOf(proxy_sites[i]) != -1) {
        return _RM_.proxy_server + '/f/o/' + feedid;
      }
    }
    for(i = 0; i < reseted.length; i++) {
      if(h.indexOf(reseted[i]) != -1) {
        return  _RM_.proxy_server + '/f/o/' + feedid + "?p=t";
      }
    }
    return link;
  }


  // export
  window.RM = $.extend(window.RM, {
    data: {
      getFinalLink: getFinalLink,
      parseSubs: parseSubs,
      parseFeedList: parseFeedList,
      parseWelcomeList: parseWelcomeList
    }
  });

})();
