window.$(function(){

  // localize global
  var backbone = window.Backbone,
      rssminer = window.Rssminer,
      models = rssminer.models,
      views = rssminer.views,
      $ = window.$;

  var Router = backbone.Router.extend(function() {

    var subscriptions = new models.SubscriptionGroupList,
        naview = new views.NavView({model: subscriptions}),
        subscriptionView;

    function initialize() {
      subscriptions.add(_OVERVIEW_);
      // fetch subscription list
      // subscriptions.fetch().done(function(data) {
      // done is executed in order
      var nav = naview.render().el;
      $('nav').replaceWith(nav);
      backbone.history.start();
      // });
    }

    function index() {
      window.location.hash = '/subscription/1';
    }

    function showSubscription(id) {
      var sub = subscriptions.getById(id);
      naview.select(id);
      sub.done(function(sub){
        subscriptionView = new views.SubscriptionView({model: sub});
        $("#content").replaceWith(subscriptionView.render().el);
        $(window).resize();
      });
    }

    return {
      initialize: initialize,
      routes:{
        '': index,
        '/subscription/:id': showSubscription
      }
    };
  });

  window.Rssminer = $.extend(window.Rssminer, {
    init: function() {
      new Router();
    }
  });

});
