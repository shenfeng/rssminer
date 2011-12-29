// functions to transform data
(function () {
  var utils = RM.util;

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
    if(tags) {
      return tags.split("; ");
    } else {
      return [];
    }
  }

  function parseSubs (subs) {
    var grouped = _.groupBy(subs, 'group_name'),
        result = [];
    for(var tag in grouped) {
      var list = _(grouped[tag]).chain()
            .sortBy(function (i) { return i.sort_index; })
            .map(function(i) {
              return {
                img: utils.imgPath(i.url),
                title: i.title || i.o_title, // original title
                href: 'read/' + i.id,
                neutral: i.count,
                id: i.id
              };
            }).value();
      result.push({tag: tag, list: list});
    }
    result = _.sortBy(result, function (i) { return i.tag.toLowerCase(); });
    return result;
  }

  function parseFeedList (subid, data) {
    if(typeof data === 'string') { data = JSON.parse(data); }
    var now = new Date().getTime() / 1000;
    _.each(data, function (e) {      // convert null to default
      // mark old enough as readed
      // TODO published_ts is not always available
      if(now - e.published_ts > 3600 * 24 * 60) e.read_date = 1;
      e.read_date = e.read_date === null ? 0 : e.read_date;
      e.vote = e.vote === null ? 0 : e.vote;
    });

    var unread = _.filter(data, function (i) { return i.read_date === 0;});
    var readed = _.filter(data, function (i) { return i.read_date > 0;});
    unread.sort(by('vote', by('published_ts', null, -1), -1));
    readed.sort(by('vote', by('published_ts', null, -1), -1));
    data = unread.concat(readed);

    var result = _.map(data,(function (i) {
      var cls = i.read_date > 0 ? 'read' : 'unread';
      if (i.vote < 0) { cls += ' dislike'; }
      else if (i.vote > 0) { cls += ' like'; }
      else { cls += ' neutral'; }
      return {
        date: utils.ymdate(i.published_ts * 1000),
        title: i.title,
        cls: cls,
        tags: parseTags(i.tags),
        id: i.id,
        href: 'read/' + subid + "/" + i.id,
        link: i.link
      };
    }));
    return result;
  };

  // export
  window.RM = $.extend(window.RM, {
    data: {
      parseSubs: parseSubs,
      parseFeedList: parseFeedList
    }
  });

})();
