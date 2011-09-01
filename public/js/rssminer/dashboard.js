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

  var prev, data = init_plot_data(),
      options = {
        // series: { shadowSize: 0 }, // drawing is faster without shadows
        yaxis: { min: 0, max: 800 },
        xaxis: { min: 0, max: 100 }
      };


  var plot = $.plot($("#plot"), [prepare_data], options);

  var Settings = OC.Model.extend({
    id: 'feeds_count',          // revent isNew return true;
    black_domains: OC.Collection,
    reseted_domains: OC.Collection,
    url: "/api/dashboard/settings",
    parse: function (resp) {
      if(prev) {
        resp.feeds_count_delta = resp.feeds_count - prev.feeds_count;
        resp.rss_finished_delta = resp.rss_finished - prev.rss_finished;
        resp.rss_pending_delta = resp.rss_pending - prev.rss_pending;
        resp.crawled_count_delta = resp.crawled_count - prev.crawled_count;
        resp.pending_count_delta = resp.pending_count - prev.pending_count;
        data.splice(0,1);
        data.push(resp.crawled_count_delta);
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
          plot.setData([prepare_data(data)]);
          plot.draw();
          localStorage.setItem('plot_data', JSON.stringify(data));
          setTimeout(showSettings, 6000);
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
