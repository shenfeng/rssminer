$(function () {

  var SubscriptionView = Backbone.View.extend({
    tagName: "div",
    id: 'content',
    template: Freader.tmpls.subscripton,
    events: {
      "click .collapsed .entry-main": "toggleExpandFeed"
    },
    initialize: function () {
      _.bindAll(this, "render", "toggleExpandFeed");
    },
    toggleExpandFeed: function(e) {
      var $entry = $(e.currentTarget).parents('.entry'),
          offset = $entry.offset().top - $(".entry:first").offset().top;
      $(".entry").not($entry).removeClass('expanded');
      $entry.toggleClass("expanded");
      $("#entries").scrollTop(offset);
    },
    render: function () {
      var data = this.model.toJSON();
      _.each(data.items, function(item){
        var summary = item.summary,
            snippet = summary && summary.replace(/<[^<>]+>/g, "")
              .replace(/\s+/g, " ")
              .replace(/&[^&;]+;/g,"")
              .slice(0,200);
        item.snippet = snippet;
      });
      $(this.el).html(this.template(data));
      return this;
    }
  });

  $.get("/api/overview",function(data) {
    var template = Freader.tmpls.nav_template;
    // console.log(template);
    $("nav").append(template(data));
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
    if($entries.length > 0) {
      $entries.height($(window).height() - $entries.offset().top - 20);
    }
    if($nav_tree.length > 0) {
      $nav_tree.height($(window).height() - $nav_tree.offset().top - 20);
    }
  }

  $(window).resize(_.debounce(layout, 100));

  $(".nav-tree a").live('click',function(){
    $(".nav-tree a").not(this).removeClass('selected');
    $(this).toggleClass('selected');
  });

});
