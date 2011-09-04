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

  function prepare_data (data) {
    var ret = [];
    for(var i = 0; i < data.length; ++i) {
      ret.push([i, data[i]]);
    }
    return ret;
  }

  function init_plot_data () {
    var d = localStorage.getItem('plot_data');
    if(d) {
      return JSON.parse(d);
    } else {
      d = [];
      for(var i = 0; i < 100; ++i) {
        d.push(0);
      }
      localStorage.setItem('plot_data', JSON.stringify(d));
      return d;
    }
  }

  var prev, data = init_plot_data();

  var Settings = OC.Model.extend({
    id: 'feeds_count',          // revent isNew return true;
    black_domains: OC.Collection,
    reseted_domains: OC.Collection,
    url: "/api/dashboard/?q=settings",
    parse: function (resp) {
      if(prev) {
        for(var attr in resp) {
          if(_.isNumber(resp[attr])) {
            resp[attr+"_delta"] = resp[attr] - prev[attr];
          }
        }
        data.splice(0,1);
        data.push(resp.feeds_count_delta);
      }
      prev = resp;
      return resp;
    }
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
        var model = this.model,
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
        "click .fetcher button" : startStopService('fetcher_running'),
        "click .index button" : startStopService('commit_index')
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

          var options = {
            // series: { shadowSize: 0 }, // drawing is faster without shadows
            yaxis: { min: 0, max: 800 },
            xaxis: { min: 0, max: 100 }
          };
          $.plot($("#plot"), [prepare_data], options);
          localStorage.setItem('plot_data', JSON.stringify(data));
        }
      });
    }

    function showInfo (path) {
      common(path);
      var model = new backbone.Model();
      var tmpl = "links";
      if(path === 'settings') tmpl = 'settings';
      model.fetch({
        url: "/api/dashboard/?q=" + path,
        success: function () {
          var view = new CommonView({
            model: model,
            tmpl: tmpls[tmpl]
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
