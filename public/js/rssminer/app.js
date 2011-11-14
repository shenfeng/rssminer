$(function(){
  var backbone = window.Backbone,
      rssminer = window.Rssminer,
      to_html = window.Mustache.to_html,
      _ = window._,
      $ = window.$;

  function layout() {
    $("#main, #main>div").height($(window).height() - $('#head').height());
  }

  function _compute_by_tag() {
    var subs =  _.reduce(window._BY_TAG_, function (result, item, index) {
      result.push({
        href: "#/tag/" + item.t,
        c: item.c,
        text: item.t
      });
      return result;
    }, []);
    subs =  _.sortBy(subs, function (item) { return -item.c; });
    return { text: 'By Tag', href: '#/tag/', has_sub: true, subs: subs };
  }

  function _compute_nav() {
    var count = _.reduce(_.values(window._BY_TIME_), function (memo, num) {
      return memo + num;
    }, 0),
        subs = _.map(window._SUBS_, function (i) {
          return { text: i.title, href: "#/sub/" + i.id, c: _BY_SUB_[i.id] };
        });
    subs = _.sortBy(subs, function (i) { return  _.isNumber(i.c) ? -i.c : 1; });
    return  [{ text: 'All', href: '#/all', c: count},
             { text: 'Recommanded', has_sub: true,
               subs: [{text: 'Items', href: '#r/items'},
                      {text: 'Subscriptions', href: '#r/subs'}]},
             _compute_by_tag(),
             { text: 'By Subscription',has_sub: true, subs: subs}];
  }

  $(window).resize(_.debounce(layout, 100));
  layout();

  var Router = backbone.Router.extend(function(){

    function initialize () {
      var html = to_html(rssminer.tmpls.nav, {navs: _compute_nav()});
      $("#left").empty().append(html);
      backbone.history.start();
    }

    function index () {

    }

    return {
      initialize: initialize,
      routes: {
        '': index
      }
    };
  });

  new Router();
});
