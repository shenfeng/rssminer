$(function(){
  var backbone = window.Backbone,
      OC = window.OC_Backbone,
      tmpls = window.Rssminer.tmpls,
      $ = window.$,
      to_html = window.Mustache.to_html;

  var CommonView = backbone.View.extend({
    id: 'content',
    initialize: function() {
      this.model.bind('change', this.render, this);
    },
    render: function() {
      $(this.el).html(to_html(this.options.tmpl, this.model.toJSON()));
      return this;
    }
  });

  var Settings = OC.Model.extend({
    id: 'feeds_count',          // revent isNew return true;
    black_domains: OC.Collection,
    reseted_domains: OC.Collection,
    url: "/api/dashboard/settings"
  });


  var SettingsView = CommonView.extend(function () {
    function addDomainPatten (name) {
      return function (e) {
        var patten = $(e.currentTarget).val();
        if(patten && e.which === 13) {
          var model = this.model;
          model.snapshot();
          model.get(name).add({patten: patten});
          model.savediff();
        }
      };
    }
    function startStopService (name) {
      return function () {
        var  model = this.model,
            running = model.get(name),
            attr = {};
        attr[name] = !running;
        model.snapshot();
        model.set(attr);
        model.savediff();
      };
    }
    return {
      events: {
        "keypress #black-domains input": addDomainPatten('black_domains'),
        "keypress #reseted-domains input": addDomainPatten('reseted_domains'),
        "click .crawler button" : startStopService('crawler_running'),
        "click .fetcher button" : startStopService('fetcher_running')
      }
    };
  });

  var Router = window.Backbone.Router.extend(function () {

    function common (path) {
      $("#nav li").removeClass('active')
        .filter('.' + path).addClass('active');
    }

    function showSettings () {
      common('settings');
      var settings = new Settings();
      settings.fetch({
        success: function () {
          var view = new SettingsView({
            model: settings,
            tmpl: tmpls.settings
          });
          $("#content").replaceWith(view.render().el);
        }
      });
    }

    function showInfo (path) {
      common(path);
      var model = new backbone.Model();
      model.fetch({
        url: "/api/dashboard/" + path,
        success: function () {
          var view = new CommonView({
            model: model,
            tmpl: tmpls[path]
          });
          $("#content").replaceWith(view.render().el);
        }
      });
    }

    return  {
      initialize: function () {
        backbone.history.start();
      },
      routes: {
        "": showSettings,
        "settings/:page":showSettings,
        ":path/:page":showInfo
      }
    };
  });

  new Router();
});
