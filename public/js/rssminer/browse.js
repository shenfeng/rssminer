$(function(){
  var $ = window.$,
      tmpls = window.Rssminer.tmpls,
      to_html = window.Mustache.to_html,
      _ = window._;

  var feeds = _.map(window._FEEDS_, function (f) {
    var c = _.map(f.categories.split(','), function (t) {
      return $.trim(t);
    });
    f.categories = _.select(c, function (t) {
      return t && t.length > 0;
    });
    return f;
  });

  var html = to_html(tmpls.browse, {feeds: feeds,
                                    tags: window._TAGS_});
  $("#main").append(html);
});
