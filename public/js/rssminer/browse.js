$(function(){
  var $ = window.$,
      tmpls = window.Rssminer.tmpls,
      ajax = window.Rssminer.ajax,
      voteCls = '.vote-up, .vote-down',
      to_html = window.Mustache.to_html,
      _ = window._;

  populate(window._FEEDS_);

  $("#rssminer-search").keyup(function (e) {
    var $this = $(this),
        term = $this.val();
    if(term.length > 1) {
      $.get("/api/search?" + $.param({term: term, limit: 10}), function (data) {
        window._FEEDS_ = data;
        populate(data);
      });
    }
  });

  $("#main").delegate(".feed h3", "click", function (e) {
    var $feed = $(e.currentTarget).parents('.feed'),
        id = + $feed.attr("data-id"), // to int
        $summary = $(".summary", $feed).toggle();
    // only load summary on click. delay load image, etc
    if($.trim($summary.html()).length === 0) {
      var s = _.detect(window._FEEDS_, function (f) {
        return f.id == id;
      });
      $summary.html(s.summary);
    } else {
      $summary.html('');
    }
    $(".snippet", $feed).toggle();
  }).delegate(voteCls, "click", function (e) {
    var $this = $(this),
        $feed = $this.parents('.feed'),
        like = $this.hasClass('vote-up');
    $.post('/api/feeds/' + $feed.attr('data-id') + '/pref',
           {pref: like}, function () {
             $(voteCls, $feed).removeClass('selected');
             $this.addClass('selected');
           });
  });

  function populate (data) {
    var feeds = _.map(data, function (f) {
      if(f.pref === false) {
        f.dislike = 'selected';
      } else if (f.pref === true) {
        f.like = 'selected';
      }

      f.tags = f.tags.split('; ');

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
    $("#main").children().remove();
    $("#main").append(html);
  };
});
