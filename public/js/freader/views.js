window.$(function(){
  var backbone = window.Backbone,
      freader = window.Freader,
      tmpls = freader.tmpls,
      $ = window.$,
      to_html = window.Mustache.to_html,
      util = window.Freader.util;

  var subscriptionadded = 'subscriptionadded';

  // render a feed, toggle it
  var FeedView = backbone.View.extend(function() {
    return {
      name: 'FeedView',
      events : {

      }
    };
  });

  // render a list of Feedview
  var SubscriptionView = backbone.View.extend(function(){
    var model,
        el,
        that;

    function toggleExpandFeed(e) {
      var $entry = $(e.currentTarget).parents('.entry'),
          offset = $entry.offset().top - $('.entry:first').offset().top;
      $('.entry').not($entry).removeClass('expanded');
      $entry.toggleClass('expanded');
      $('#entries').scrollTop(offset);
    }

    function template(data) {
      return to_html(tmpls.subscription, data);
    }
    function render() {
      $(el).html(template(model.toJSON()));
      return that;
    }
    return {
      tagName: 'div',
      id: 'content',
      events: {
        'click .collapsed .entry-main': toggleExpandFeed
      },
      render: render,
      initialize: function() {
        that = this;
        model = that.model;
        el = that.el;
      }
    };
  });

  // render add link, import opml
  var AddSubscriptionView = backbone.View.extend(function(){
    var that,
        el,
        subscriptionGroupList,
        isFormShow = false;

    function showForm(e) {
      if(!isFormShow) {
        that.$('.form').show();
        that.$('input').focus();
        $(document).bind('click', dismissForm);
        isFormShow = true;
      }
    }

    function dismissForm(e) {
      // e => undefined, hide it,
      // form is show && event is happenning outside el, hide it
      if( !e || (isFormShow && !util.within(e.target, el))) {
        that.$('.form').fadeOut();
        isFormShow = false;
        $(document).unbind('click', dismissForm);
        e && e.stopPropagation();
      }
    }

    function addOnEneter(e) {
      if(e.which === 13) {
        var $input = that.$('input'),
            dfd = subscriptionGroupList.addSubscription($input.val());
        $input.val('');
        dismissForm();
        dfd.done(function(sub) {
          subscriptionGroupList.trigger(subscriptionadded, sub);
        });
      }
    }

    return {
      tagName: 'div',
      id: 'add-subscription',
      initialize: function (options) {
        that =this;
        el = that.el;
        subscriptionGroupList = options.subscriptionGroupList;
        $(el).html(tmpls.add_subscription);
      },
      events: {
        'click span': showForm,
        'keypress input': addOnEneter
      }
    };
  });

  var MenuView =backbone.View.extend(function() {
    var subscription,
        el,
        that,
        isShow = false;

    function optionHandler(e) {
      // console.log(e);
    }

    return {
      tagName: 'div',
      id: 'options-menu',
      remove: function (){
        $(el).fadeOut('fast', function() {
          $(el).remove();
        });
      },
      initialize: function (options) {
        that = this;
        el = that.el;
        subscription = that.model;
      },
      render: function(){
        $(el).html(to_html(tmpls.menu, subscription.getMenuJSON()));
        return that;
      },
      events: {
        'click .option': optionHandler
      }
    };
  });

  // render left link sidebar, respond to click event
  var NavView = backbone.View.extend(function() {
    var model,
        el,
        that,
        addSubscriptionView,
        menuView,
        isMenuShown = false,
        currentMenuId,          // which subscription
        refresh = false;

    function template(data) {
      return to_html(tmpls.nav_template, data);
    }

    function render() {
      var $el = $(el);
      // only refresh nav tree
      if(refresh) {
        $el.find('.nav-tree').replaceWith(template({data: model.toJSON()}));
      } else {
        $el.append(addSubscriptionView.render().el);
        $el.append(template({data: model.toJSON()}));
        // no need to init add subscription view anymore from now on
        refresh = true;
      }
      return that;
    }
    // select and hight a given subscription in left navigation
    function select(id){
      util.removeClass('selected');
      $('#subs-' + id).addClass('selected');
    }

    function toggleFolder(e) {
      $(e.currentTarget).parents('.folder').toggleClass('collapsed');
    }

    function dissmissMenu(e) {
      if(e && $(e.target).is('.icon')){
        return;                 // this is handlered by toggleMenu
      }
      // e => undefined: dissmiss
      if(!e || (isMenuShown && !util.within(e.target, menuView.el))) {
        menuView && menuView.remove();
        isMenuShown = false;
        $(document).unbind('click', dissmissMenu);
        e && e.stopPropagation();
      }
    }

    function toggleMenu(e) {
      var $icon =  $(e.currentTarget),
          $a =$icon.parents('.sub').find('a'),
          id = $a.attr('id').split('-')[1];

      if(currentMenuId && currentMenuId !== id) { // differenct subscription
        dissmissMenu();
      }
      // same subscription, is show, hide it
      if( isMenuShown ) {
        dissmissMenu();
      } else {
        // different subscription || first time open
        model.getById(id, true).done(function(subscription) {
          menuView = new MenuView({model: subscription});

          var $menu = $(menuView.render().el),
              top = ($icon.offset().top + $icon.height()) + 'px';

          $('#container').append($menu);
          $menu.css('top', top);
          currentMenuId = id;
          // dissmissMenu will be called after this toggleMenu returns
          $(document).bind('click', dissmissMenu);
          isMenuShown = true;
        });
      }
    }

    return {
      tagName: 'nav',
      render: render,
      select: select,
      events: {
        'click .folder .toggle': toggleFolder,
        'click .icon': toggleMenu
      },
      initialize: function() {
        that = this;
        model = that.model;
        el = that.el;
        // get notified when new subscription added
        model.bind(subscriptionadded, function(sub) {
          render();             // refresh feed link ui
          window.location.hash = "/subscription/" + sub.id;
        });
        addSubscriptionView = new AddSubscriptionView({
          // needed to add subscription
          subscriptionGroupList: model
        });
      }
    };

  });

  // show keyboard help, respond to keyboard event
  var KeyboardView = backbone.View.extend(function() {
    var isHelpShow = false;

    function showHelp() {
      if(!isHelpShow){
        // TODO show help
        isHelpShow = true;
      }
    }

    function hideHelp() {
      if(isHelpShow) {
        // TODO hide help
        isHelpShow = false;
      }
    }

    function bindEvent() {
    }
  });

  window.Freader = $.extend(window.Freader, {
    views : {
      NavView: NavView,
      SubscriptionView: SubscriptionView
    }
  });

});
