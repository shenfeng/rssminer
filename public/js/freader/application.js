$(function () {

  var nofity = (function() {
    var $nofity = $("#notification"),
        $p = $("p",$nofity),
        message,
        count = 0;
    function msg(a, r, msg) {
      if(message !== msg){
        count = 1;
        message = msg;
        $p.html(msg);
        $nofity.removeClass(r).addClass(a)
          .css({
            marginLeft: -$p.width()/2,
            visibility: 'visible'
          });
      }else {
        count++;
      }
    }

    function hide (msg) {
      if(msg === message){
        count--;
      }
      if( !msg || count === 0) {
        _.delay( function (){
          message = null;
          $nofity.css('visibility', 'hidden');
        }, 2000);
      }
    }
    return {
      msg: _.bind(msg, null, 'message', 'error'),
      error: _.bind(msg, null, 'error', 'message'),
      hide: hide
    };

  })();

  var ajax = (function() {
    var loading = 'Loading...';
    function handler(a) {
      return a.success(function () {
        nofity.hide(loading);
      });
    };
    function get(url, success){
      nofity.msg(loading);
      return handler($.ajax({
        url: url,
        success: success
      }));
    }
    function jpost(url, data){
      nofity.msg(loading);
      var ajax = $.ajax({
        url: url,
        type: 'POST',
        datatype: 'json',
        contentType: 'application/json',
        data: JSON.stringify(data)
      });
      return handle(ajax);
    }
    return {
      get: get,
      jpost: jpost
    };
  })();


  var d_selected = "selected";
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
      var $entry = $(e.currentTarget).parents(".entry"),
          offset = $entry.offset().top - $(".entry:first").offset().top;
      $(".entry").not($entry).removeClass("expanded");
      $entry.toggleClass("expanded");
      $("#entries").scrollTop(offset);
    },
    render: function () {
      var data = this.model.toJSON();
      _.each(data.items, function(item) {
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

  var Magic = (function() {
    var fdata;
    function subs_comp(asub, bsub){
      return asub.title.toLowerCase() < bsub.title.toLowerCase();
    }
    function group_comp(ag, bg){
      return ag.group_name.toLowerCase() < bg.group_name.toLowerCase();
    };
    function getById(id) {
      var ret;
      _.each(fdata, function(group) {
        _.each(group.subscriptions, function(subscripton) {
          if(subscripton.id === id)
            ret = subscripton;
        });
      });
      if(!ret) {
        throw new Error("get by id " + id + " fail");
      }
      return ret;
    }
    function addFeeds (subscripton) {
      var saved = getById(subscripton.id);
      saved && _.extend(saved, subscripton);
    }
    function showSubscription (data) {
      var model = new Backbone.Model(data),
          view = new SubscriptionView({model : model});
      $("#content").replaceWith(view.render().el);
      $(window).resize();
    }
    function showById (id) {
      var saved = getById(id);
      if(!saved.items) {
        ajax.get('/api/feeds/'+id, function(data) {
          showSubscription(data);
          addFeeds(data);
        });
      }else {
        showSubscription(saved);
      }
    }
    function reShowNav() {
      if(fdata) {
        fdata.sort(group_comp);
        _.each(fdata, function(group) {
          group.subscriptions.sort(subs_comp);
        });
        var nav = Freader.tmpls.nav_template(fdata),
            $nav = $(".nav-tree");
        $nav.length > 0 ? $nav.replaceWith(nav) : $("nav").append(nav);
        $("a", $nav).click(function() {
          $(".selected").removeClass(d_selected);
          $(this).addClass(d_selected);
        });
      }
    }
    function init() {
      ajax.get("/api/overview",function(data) {
        window.Freader.data = data;
        fdata = data;
        reShowNav();
        new Router();
        Backbone.history.start();
      });
    }
    function addSubscription (link) {
      var ajax=  ajax.jpost("/api/feeds", {link: link});
      ajax.success(function(data, status, xhr) {
        var ungroup = 'freader_ungrouped',
            group =  _.detect(fdata, function(e) {
              return e.group_name === ungroup;
            });
        if(group) {
          group.subscriptions.push(data);
        }else {
          data.push({
            group_name: ungroup,
            subscriptons: [data]
          });
        }
        reShowNav();
        window.location.hash = "/subscription/" + data.id;
      });
    }
    return {
      showById: showById,
      init: init,
      addSubscription: addSubscription
    };
  })();

  var Router = Backbone.Router.extend({
    routes:{
      "": "index",
      "/subscription/:id": "subscription"
    },
    index: function () {
      window.location.hash = "/subscription/1";
    },
    subscription: function(id) {
      Magic.showById(+id);
      $(".selected").removeClass(d_selected);
      $("#subs-" + id).addClass(d_selected);
    }
  });

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

  (function() {
    var $form = $("#add-subscription .form"),
        $input = $("input",$form);
    $("#add-subscription span").click(function() {
      $form.toggle();
      $input.focus();
    });
    $form.keydown(function(e) {
      if(e.which === 13) {
        Magic.addSubscription($input.val());
        $form.hide();
        $input.val("");
      }
    });
  })();
  Magic.init();
});
