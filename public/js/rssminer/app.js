window.$(function(){
  var backbone = window.Backbone,
      rssminer = window.Rssminer,
      utils = rssminer.util,
      _ = window._,
      $ = window.$;

  function layout() {
    $("#main, #main>div").height($(window).height() - $('#head').height());
  }

  $(window).resize(_.debounce(layout, 100));

  var Router = backbone.Router.extend(function() {
    function initialize () {
      layout();
      var $left = $("#left"),
          $mid = $("#mid");

      $left.empty().append(rssminer.render_nav());
      $mid.empty().append(rssminer.render_mid());

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
    }

    function showSub (sub) {
    }

    return {
      initialize: initialize,
      routes: {
        '': index,
        'tag/:tag': showTag,
        'sub/:sub': showSub
      }
    };
  });

  new Router();
});
