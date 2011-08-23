$(function(){
  var rssminer = window.Rssminer,
      $ = window.$,
      util = rssminer.util,
      tmpls = rssminer.tmpls,
      to_html = window.Mustache.to_html,
      _ = window._;

  var feeds = _.map(_FEEDS_, function (f) {
    f.snippet = util.snippet(f.summary);
    return f;
  });
  var html = to_html(tmpls.browse, {feeds: feeds});
  $("#page-wrap").replaceWith(html);
});
