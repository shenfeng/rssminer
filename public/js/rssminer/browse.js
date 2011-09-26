$(function(){
  var $ = window.$,
      tmpls = window.Rssminer.tmpls,
      ajax = window.Rssminer.ajax,
      to_html = window.Mustache.to_html,
      _ = window._;

  var feeds = _.map(window._FEEDS_, function (f) {
    f.likeClass = f.dislikeClass = 'vote';
    if(f.pref === false) {
      f.dislikeClass += ' selected';
    } else if (f.pref === true) {
      f.likeClass += ' selected';
    }

    f.tags = f.tags || '';
    f.tags = _.map(f.tags.split(', '), function (t) {
      if($.trim(t).indexOf(' ') != -1) {
        return '"' + t + '"';
      }
      return t;
    });

    f.snippet = window.Rssminer.util.snippet(f.summary, 300);

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
    var $feed = $(e.currentTarget).parents('.feed');
    $(".snippet", $feed).toggle();
    $(".summary", $feed).toggle();
  }).delegate(".related span", "hover", function (e) {
    var $feed = $(e.currentTarget).parents('.feed'),
        id = $feed.attr('data-docid'),
        $ul = $(".related ul", $feed);
    if($ul.length == 0) {
      ajax.get('/api/feeds/' + id + "/alike").done(function (data) {
        var html = to_html(tmpls.likethis, {feeds: data});
        $(e.currentTarget).parents(".related").append(html);
      });
    }
  }).delegate("span.vote", "click", function (e) {
    var $this = $(this),
        $feed = $this.parents('.feed'),
        id = $feed.attr('data-docid'),
        like = $this.text() === 'good';
    $.post('/api/feeds/' + id + '/pref', {pref: like}, function (resp, stats,xhr) {
      $('.vote', $feed).removeClass('selected');
      $this.addClass('selected');
    });
  });
});
