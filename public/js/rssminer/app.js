window.$(function(){
  var backbone = window.Backbone,
      rssminer = window.Rssminer,
      ajax = rssminer.ajax,
      utils = rssminer.util,
      _ = window._,
      $ = window.$;

  function layout() {
    $("#main, #main>div").height($(window).height() - $('#head').height());
  }

  $(window).resize(_.debounce(layout, 100));

  var Router = backbone.Router.extend(function() {
    var $left = $("#left"),
        $mid = $("#mid"),
        $right = $("#right .wrapper"),
        rerender_nav = function () {
          $left.empty().append(rssminer.render_nav());
        },
        rerender_mid = function () {
          $mid.empty().append(rssminer.render_mid());
        };

    function initialize () {
      layout();
      rerender_nav();
      rerender_mid();
      utils.delegateEvents($left, {
        'click a': function () {
          $('.selected', $left).removeClass('selected');
          $('.item', this).addClass('selected');
        }
      });

      utils.delegateEvents($mid, {
        'click li': function (e) {
          $('.selected', $mid).removeClass('selected');
          $(this).addClass('selected');
        },
        'click .vote span': function (e) {
        }
      });
      backbone.history.start();
    }

    function index () {
    }

    function showTag (tag) {
      ajax.get("/api/tag/" + tag).done(function (data) {
        if(data) {
          _FEEDS_ = data;
          rerender_mid();
          var t = _.find($(".text", $left), function (item) {
            return $.trim($(item).text()) === tag;
          });
          if(t) {
            $(t).parent().addClass('selected');
          }
        }
      });
    }

    function showSub (id) {
      ajax.get("/api/subs/" + id).done(function (data) {
        if(data) {
          _FEEDS_ = data;
          rerender_mid();
        }
      });
    }

    function showFeed (id) {
      ajax.get("/api/feeds/" + id).done(function (data) {
        if(data) {
          $right.html(rssminer.render_right(data));
        }
      });
    }

    return {
      initialize: initialize,
      routes: {
        '': index,
        'all': index,
        'all/:id' : showFeed,
        'tag/:tag': showTag,
        'subs/:s_id': showSub,
        'subs/:s_id/:f_id': function (sub_id, feed_id) {
          showSub(sub_id);
          showFeed(feed_id);
        },
        'tag/:tag/:id': function (tag, id) {
          showTag(tag);
          showFeed(id);
        }
      }
    };
  });

  new Router();
});
