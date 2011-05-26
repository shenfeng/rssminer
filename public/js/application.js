window.READER = (function (window, document, undefined) {
  "use strict";
  
  var Feed = Backbone.Model.extend({
    
  });

  var FeedList = Backbone.Collection.extend({
    
  });


  var Item = Backbone.Model.extend({
    initialize: function (attr) {
      var feeds  = new FeedList();
      _.each(attr.items, function (item) { 
        feeds.add(new Feed(item));
      });
      this.set({ feeds: feeds });
    }
  });

  var ItemList = Backbone.Collection.extend({
    model: Item,
    url:"/api/feeds"
  });

  var Items = new ItemList;

  var ItemView = Backbone.View.extend({
    className: "item",
    tagName: "li"
  });

  var AppView = Backbone.View.extend({
    initialize: function() {
      this.model = Items;
    },
    alive: function() {
      this.model.fetch();
    }

  });
  return {
    AppView: AppView,
    Items: Items,
    ItemView: ItemView
  };
})(this, this.document);

$(function () {
  var app = new Backbone.Backrub($("#app-template").html());
  $("#app-template").after(app.render());
  app.makeAlive();
});
