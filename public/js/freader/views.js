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

    function showForm() {
      if(!isFormShow) {
        that.$('.form').show();
        that.$('input').focus();
        $(document).bind('click', hideForm);
        isFormShow = true;
      }
    }

    function hideForm(e) {
      // e => undefined, hide it,
      // form is show && event is happenning outside el, hide it
      if( !e || (isFormShow && !($.contains(el, e.target) || el === e.target))) {
        that.$('.form').fadeOut();
        isFormShow = false;
        $(document).unbind('click', hideForm);
      }
    }

    function addOnEneter(e) {
      if(e.which === 13) {
        var $input = that.$('input'),
            dfd = subscriptionGroupList.addSubscription($input.val());
        $input.val('');
        hideForm();
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

  // render left link sidebar, respond to click event
  var NavView = backbone.View.extend(function() {
    var model,
        el,
        that,
        addSubscriptionView,
        refresh = false;

    function template(data) {
      return to_html(tmpls.nav_template, data);
    }

    function render() {
      var $el = $(el);
      // only refresh nav tree
      if(refresh){
        $el.find('.nav-tree').replaceWith(template({data: model.toJSON()}));
      } else {
        $el.append(addSubscriptionView.render().el);
        $el.append(template({data: model.toJSON()}));
        refresh = true;
      }
      return that;
    }

    function select(id){
      util.removeClass('selected');
      $('#subs-' + id).addClass('selected');
    }

    function toggleFolder(e) {
      $(e.currentTarget).parents('.folder').toggleClass('collapsed');
    }
    return {
      tagName: 'nav',
      render: render,
      select: select,
      events: {
        'click .folder .toggle': toggleFolder
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
