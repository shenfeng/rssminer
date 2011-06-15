window.$(function(){

  // localize global
  var backbone = window.Backbone,
      freader = window.Freader,
      tmpls = window.Freader.tmpls,
      to_html = window.Mustache.to_html,
      $ = window.$,
      util = window.Freader.util;

  var selected = 'selected';

  var SubscriptionView = backbone.View.extend(function(){
    function template(data) {
      return to_html(tmpls.subscription, data);
    }
    function toggleExpandFeed(e) {
      var $entry = $(e.currentTarget).parents('.entry'),
          offset = $entry.offset().top - $('.entry:first').offset().top;
      $('.entry').not($entry).removeClass('expanded');
      $entry.toggleClass('expanded');
      $('#entries').scrollTop(offset);
    }

    function render() {
      var data = this.model.toJSON();
      _.each(data.items, function(item) {
        item.snippet = util.snippet(item.summary);
      });
      $(this.el).html(template(data));
      return this;
    }

    return {
      tagName: 'div',
      id: 'content',
      events: {
        'click .collapsed .entry-main': toggleExpandFeed
      },
      render: render
    };
  });

  var Router = backbone.Router.extend(function() {
    function index() {
      window.location.hash = '/subscription/1';
    }
    function subscription(id) {
      magic.showById(+id);
      util.removeClass(selected);
      $('#subs-' + id).addClass(selected);
    }
    return {
      routes:{
        '': index,
        '/subscription/:id': subscription
      }
    };
  });

  var magic = (function() {
    var fdata;                  // this `class` manipulate
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
        throw new Error('get by id ' + id + ' fail');
      }
      return ret;
    }
    function addFeeds (subscripton) {
      var saved = getById(subscripton.id);
      saved && _.extend(saved, subscripton);
    }
    function showSubscription (data) {
      var model = new backbone.Model(data),
          view = new SubscriptionView({model : model});
      $('#content').replaceWith(view.render().el);
      $(window).resize();
    }
    function showById (id) {
      var saved = getById(id);
      if(!saved.items) {
        freader.ajax.get('/api/feeds/'+id, function(data) {
          showSubscription(data);
          addFeeds(data);
        });
      }else {
        showSubscription(saved);
      }
    }
    function reshowNav() {
      if(fdata) {
        fdata.sort(group_comp);
        _.each(fdata, function(group) {
          group.subscriptions.sort(subs_comp);
        });
        var nav = to_html(tmpls.nav_template, {data: fdata}),
            $nav = $('.nav-tree');
        $nav.length > 0 ? $nav.replaceWith(nav) : $('nav').append(nav);
        $('a', $nav).click(function() {
          util.removeClass(selected);
          $(this).addClass(selected);
        });
      }
    }
    function init() {
      freader.ajax.get('/api/overview',function(data) {
        fdata = data;
        reshowNav();
        new Router();
        backbone.history.start();
      });
    }
    function addSubscription (link) {
      var post = freader.ajax.jpost('/api/feeds', {link: link});
      post.success(function(data, status, xhr) {
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
        reshowNav();
        window.location.hash = '/subscription/' + data.id;
      });
    }
    return {
      showById: showById,
      init: init,
      addSubscription: addSubscription
    };
  })();

  window.Freader = $.extend(window.Freader, {
    init: magic.init,
    magic: magic
  });

});
