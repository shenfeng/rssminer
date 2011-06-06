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

  
  
  var model = new Backbone.Model(Freader.fixtures.feeds),
      view = new SubscriptionView({model : model});
  $("#content").replaceWith(view.render().el);
  
  // var items_template = Freader.tmpls["items_template"],
  //     feeds = Freader.fixtures.feeds;
  // $("#content").append(items_template(feeds));
  
  var nav_template = Freader.tmpls["nav_template"],
      nav = Freader.fixtures.unread_count;
  $("nav").append(nav_template(nav));
});
