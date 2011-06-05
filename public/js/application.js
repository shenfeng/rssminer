(function (window, document, undefined) {
  var Item = Backbone.Model.extend({
    parse: function(resp){
      console.log("1111");
      console.log(resp);
    }
  });

  var ItemList = Backbone.Collection.extend({
    model: Item,
    url:"/api/feeds/1"
  });

  var Items = new ItemList;

  var ItemView = Backbone.View.extend({
    tagName: "ol",
    className: "item"
  });

  var AppView = Backbone.View.extend({
    initialize: function() {
      this.model = Items;
    },
    alive: function() {
      this.model.fetch();
    }

  });
  window.Freader = $.extend(
    window.Freader, {
      AppView: AppView,
      Items: Items,
      ItemView: ItemView
    });
})(this, this.document);

$(function () {

  Handlebars.registerHelper('feeditems', function(items, fn) {
    console.log(items);
    var out = "<ul>";

    for (var i = 0, l = items.length; i < l; i++) {
      out = out + "<li>" + fn(items[i]) + "</li>";
    }

    return out + "</ul>";
  });
});
