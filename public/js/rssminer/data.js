// functions to transform data
(function () {

  var utils = RM.util;

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
                title: i.title,
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
    if(typeof data === 'string') {
      data = JSON.parse(data);
    }

    var result =    _(data).chain()
          .sortBy(function (i) { return i.id; })
          .map(function (i) {
            return {
              date: utils.ymdate(i.published_ts * 1000),
              title: i.title,
              cls: 'unread like',
              tags: parseTags(i.tags),
              id: i.id,
              href: 'read/' + subid + "/" + i.id,
              link: i.link
            };
          }).value();
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
