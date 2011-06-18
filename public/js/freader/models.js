window.$(function(){
  // localize global
  var backbone = window.Backbone,
      freader = window.Freader,
      util = freader.util,
      $ = window.$,
      _ = window._;

  var Feed = backbone.Model.extend(function() {
    var that;

    return {
      name: 'Feed',
      initialize : function() {
        that = this;
      },
      toJSON: function (){
        var j = _.clone(that.attributes);
        j.snippet = util.snippet(j.summary);
        return j;
      }
    };
  });

  // Feeds + title + favicon + count + ....
  var Subscription = backbone.Model.extend(function() {
    var FeedsList = backbone.Collection.extend( function() {
      return {
        model: Feed,
        name: 'FeedsList'
      };
    });

    var url,
        that,                   //void using this;
        feeds = new FeedsList;

    // override
    function parse (data) {
      if(data.items) {
        // maintain a collection
        feeds.add(data.items);
        delete data.items;
      }
      return data;
    }

    function toJSON () {
      var j = _.clone(that.attributes);
      j.items = feeds.toJSON();
      return j;
    }

    function set(attr, options) {
      attr = parse(_.clone(attr));
      return backbone.Model.prototype.set.call(that, attr, options);
    }

    return {
      parse: parse,
      name: 'Subscription',
      toJSON: toJSON,
      set: set,
      initialize: function(attributes, options) {
        that = this;
      },
      isFetched: function() {
        return feeds.length > 0;
      },
      fetch: function() {
        // return jquery ajax deffer
        return freader.ajax.get("/api/subscription/" + that.id);
      }
    };
  });

  // groupname + subscriptions
  var SubscriptionGroup = backbone.Model.extend(function () {
    var SubscriptonList = backbone.Collection.extend({
      name: 'SubscriptonList',
      model: Subscription,
      comparator: function (subscription) {
        return subscription.get('title').toLowerCase();
      }
    });

    //subscriptions in this group
    var subscriptions = new SubscriptonList,
        that;

    function getById(id) {
      return subscriptions.get(id);
    }

    // return a promise of add success
    function addSubscription (link) {
      var post = freader.ajax.jpost('/api/subscription', {link: link}),
          dfd = $.Deferred();
      post.done(function(data, status, xhr) {
        subscriptions.add(data);
        dfd.resolve(getById(data.id));
      });
      return dfd.promise();
    }

    function parse (data) {
      if(data.subscriptions) {
        // maintain a collection
        subscriptions.add(data.subscriptions);
        delete data.subscriptions;
      }
      return data;
    }

    // override
    function toJSON () {
      var j = _.clone(that.attributes);
      j.subscriptions = subscriptions.toJSON();
      return j;
    }
    function set(attr, options) {
      attr = parse(_.clone(attr));
      return backbone.Model.prototype.set.call(that, attr, options);
    }
    return {
      initialize: function() {
        that = this;
      },
      name: 'SubscriptionGroup',
      parse: parse,
      set: set,
      toJSON: toJSON,
      getById: getById,
      addSubscription: addSubscription
    };
  });

  var SubscriptionGroupList = backbone.Collection.extend(function () {
    var ungroup = 'freader_ungrouped',
        that;                   //avoid using this
    // get subscription by id
    function getById(id) {
      var dfd = $.Deferred(),
          sub;
      that.each(function(g) {
        var s = g.getById(id);
        if(s) {
          sub = s;
        }
      });

      if(sub.isFetched()) {    // already in memory
        dfd.resolve(sub);
      } else {
        sub.fetch().done(function(data) { // fetch from server
          sub.set(data);                  // cache in memory
          dfd.resolve(sub);
        });
      }
      return dfd.promise();
    }

    function addSubscription(link) {
      var unamed =  that.detect(function(g){
        return g.get('group_name') === ungroup;
      });
      return unamed.addSubscription(link);
    }

    return {
      addSubscription: addSubscription,
      name: 'SubscriptionGroupList',
      initialize: function () {
        that = this;
        // groups = that.models;
      },
      fetch: function (){
        return freader.ajax.get('/api/overview').done(function(data) {
          that.add(data);
        });
      },
      model: SubscriptionGroup,
      comparator: function (subscriptiongrop) {
        return subscriptiongrop.get('group_name').toLowerCase();
      },
      getById: getById
    };
  });

  window.Freader = $.extend(window.Freader, {
    models: {
      SubscriptionGroupList: SubscriptionGroupList
    }
  });

});
