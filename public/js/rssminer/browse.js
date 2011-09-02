$(function(){
  var $ = window.$,
      tmpls = window.Rssminer.tmpls,
      to_html = window.Mustache.to_html,
      _ = window._;

  var feeds = _.map(window._FEEDS_, function (f) {
    f.categories = f.categories || '';
    var c = _.map(f.categories.trim().split(','), function (t) {
      if(t) {
        t = $.trim(t);
        if(t.indexOf(' ') != -1) {
          return '"' + t +'"';
        } else {
          return t;
        }
      }
    });
    f.categories = _.select(c, function (t) {
      return t && t.length > 0;
    });
    var author = f.author;
    if(author && author.indexOf(' ') != -1) {
      f.authorTag = '"' + author + '"';
    } else {
      f.authorTag = author;
    }

    return f;
  });

  var html = to_html(tmpls.browse, {feeds: feeds,
                                    tags: window._TAGS_});
  $("#main").append(html);
  $("#main").delegate('.feed .snippet', 'click', function (e) {
    var id = $(e.currentTarget).parents('.feed').attr('data-id');
    $.getJSON('/api/feeds/likethis/' + id, function (data) {
      $("#similar").remove();
      $("#right-side").append(to_html(tmpls.likethis, {feeds: data}));
    });
  });
});
