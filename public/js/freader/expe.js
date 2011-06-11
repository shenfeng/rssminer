$(function () {



  var SubscriptionView = Backbone.View.extend({
    tagName: "div",
    id: 'content',
    template: Freader.tmpls["subscripton"],
    events: {
      "click .collapsed .entry-main": "toggleExpandedFeed"
    },
    initialize: function () {
      _.bindAll(this, "render", "toggleExpandedFeed");
    },
    toggleExpandedFeed: function(e) {
      $(e.currentTarget).parents('.entry').toggleClass("expanded");
    },
    render: function () {
      var data = this.model.toJSON();
      $(this.el).html(this.template(data));
      return this;
    }
  });

  $.get("/api/overview",function(data) {
    var nav_template = Freader.tmpls["nav_template"];
    $("nav").append(nav_template(data));
  });

  var Router = Backbone.Router.extend({
    routes:{
      "": "index",
      "/subscription/:id": "subscription"
    },
    index: function () {
      window.location.hash = "/subscription/1";
    },
    subscription: function(id) {
      $.get('/api/feeds/'+id, function(data) {
        var model = new Backbone.Model(data),
            view = new SubscriptionView({model : model});
        $("#content").replaceWith(view.render().el);
        $(window).resize();
      });
    }
  });

  var router = new Router();
  Backbone.history.start();

  function layout() {
    var $entries = $("#entries"),
        $nav_tree = $(".nav-tree");
    $entries.height($(window).height() - $entries.offset().top - 20);
    $nav_tree.height($(window).height() - $nav_tree.offset().top - 20);
  }

  $(window).resize(_.debounce(layout, 100));
});
