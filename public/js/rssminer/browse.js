$(function(){
  var $ = window.$,
      tmpls = window.Rssminer.tmpls,
      ajax = window.Rssminer.ajax,
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
  $("#main").append(html).delegate(".feed h3", "click", function (e) {
    var $feed = $(e.currentTarget).parents('.feed'),
        id = $feed.attr('data-feedid'),
        $snippet = $(".snippet", $feed),
        $summary = $(".summary", $feed);
    if($summary.length == 0){
      ajax.get("/api/feeds/" + id).done(function (data) {
        $snippet.hide();
        $feed.append($('<div class="summary"/>').append(data.summary));
      });
    } else {
      $snippet.show();
      $summary.remove();
    }
  });

  $("#main").delegate(".related span", "click", function (e) {
    var id = $(e.currentTarget).parents('.feed').attr('data-docid');
    ajax.get('/api/feeds/likethis/' + id).done(function (data) {
      var html = $("ul", to_html(tmpls.likethis, {feeds: data}));
      $(e.currentTarget).parents(".related").append(html);
    });
  });
});
