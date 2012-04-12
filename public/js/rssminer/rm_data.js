(function () {
  var RM = window.RM,           // namespace
      _RM_ = window._RM_,       // inject to html, data
      ajax = RM.ajax,
      util = RM.util;

  var user = (_RM_ && _RM_.user) || {},
      user_conf = user.conf || {},
      expire = user_conf.expire || 45,
      sub_list_cache;

  var CACHE_TIME = 1000 * 60 * 60 * 4, // 4 hour
      PROXY_SERVER = window._RM_.proxy_server,
      STATIC_SERVER = window._RM_.static_server,
      LIKE_SCORE = user_conf.like_score || 1,
      NEUTRAL_SCORE = user_conf.neutral_score || 0; // db default 0

  function current_time () {
    return new Date().getTime();
  }

  function get_all_subscription () {
    if(sub_list_cache) {
      return sub_list_cache;
    } else {
      var subs = _RM_.subs,
          grouped = _.groupBy(subs, 'group_name'),
          result = [],
          collapsed = user_conf.nav || [];
      for(var tag in grouped) {
        var list = _(grouped[tag]).chain()
              .sortBy(function (i) { return i.sort_index; })
              .map(function(i) {
                return {
                  img: util.favicon_path(i.url),
                  title: i.title || i.o_title, // original title
                  href: 'read/' + i.id,
                  like: i.like_c,
                  dislike: i.dislike_c,
                  neutral: i.total_c - i.like_c - i.dislike_c,
                  id: i.id
                };
              }).value();
        list = _.filter(list, function (i) { return i.title; });
        result.push({
          tag: tag,
          list: list,
          collapse: _.include(collapsed, tag)
        });
      }
      result = _.sortBy(result, function (i) { return i.tag.toLowerCase(); });
      sub_list_cache = result;
      return result;
    }
  }

  function mark_as_read (subid, feedid) {

  }

  function update_group_name_sort_order (subid, sort_order, groupname) {

  }

  function get_feed_list_by_subid (subid, sort_by, cb) {

  }

})();
