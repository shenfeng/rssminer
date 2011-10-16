$(function(){
  var backbone = window.Backbone,
      JSON = window.JSON,
      ajax = window.Rssminer.ajax,
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

  var stat = (function () {
    var key = 'plot_data';

    function read_plot_data () {
      var d = window.localStorage.getItem(key);
      if(d) {
        return JSON.parse(d);
      } else {
        d = [];
        for(var i = 0; i < 100; ++i) {
          d.push(0);
        }
        return d;
      }
    }
    var prev, crawler = {}, data = read_plot_data(),
        line_opts = {
          yaxis: { min: 0, max: 3500 },
          xaxis: { min: 0, max: 100 }
        }, pie_opts = {
          series: {
            pie: { show: true }
          }
        }, meta = {
          server: {
            1000: 'crawler_counter',
            1200: 'crawler_queue',
            1250: 'queue_aval',
            1300: 'crawler_start'
          },
          http: {
            150: '150 unknow error',
            160: '160 unknow host',
            170: '170 connection error',
            172: '172 connection timeout',
            175: '175 connection reset',
            181: '181 null location',
            182: '182 bad url',
            183: '183 ignored url',
            190: '190 unknow content type',
            200: '200 ok',
            275: '275 proxied ok',
            301: '301 moved permanently',
            302: '302 found',
            304: '304 not modified',
            400: '400 bad request',
            401: '401 unauthorized',
            402: '402 payment required',
            403: '403 forbidden',
            404: '404 not found',
            471: '471 client abort',
            500: '500 internal server error',
            502: '502 bad gateway',
            503: '503 service unavailable',
            504: '504 gateway timeout',
            513: '513 body too large',
            520: '520 server timeout'
          }
        };

    function prepare_data () {
      var line = [];
      for(var i = 0; i < data.length; ++i) {
        line.push([i, data[i]]);
      }
      var pie = [];
      _.each(meta.http, function (v, k) {
        if(crawler && crawler[k]) {
          pie.push({label: v, data: crawler[k]});
        }
      });
      return {line: line, pie: pie};
    }

    function parse (resp) {
      crawler = resp.crawler;
      if(crawler) {
        _.each(meta.server, function (v, k) {
          resp[v] = crawler[k];
        });
        var e = new Date().getTime() / 1000 - resp.crawler_start;
        resp.crawler_speed = Math.floor(resp.crawler_counter / e * 60);
      }
      if(prev) {
        for(var attr in resp) {
          if(_.isNumber(resp[attr])) {
            resp[attr+"_delta"] = resp[attr] - prev[attr];
          }
        }
        data.splice(0,1);
        data.push(resp.crawler_counter_delta);
      }
      prev = resp;
      return resp;
    }

    function plot () {
      var d = prepare_data();
      $.plot($("#line-chart"), [d.line], line_opts);
      $.plot($("#crawler-pie"), d.pie , pie_opts);
      window.localStorage.setItem(key, JSON.stringify(data));
    }
    return {
      parse: parse,
      plot: plot
    };
  })();

  var Settings = backbone.Model.extend({
    url: "/api/dashboard/stat",
    parse: function (resp) {
      return stat.parse(resp);
    }
  });

  var SettingsView = CommonView.extend(function () {
    function toggleService (e) {
      var $tr = $(e.currentTarget).parents('tr'),
          section = $tr.attr('data-sid'),
          status = $.trim($tr.find('.status').text()),
          command = status  === 'false' ? 'start' : 'stop';
      ajax.jpost("/api/dashboard", {which: section, command: command});
    }
    return {
      events: {
        "click #controls button": toggleService
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
          stat.plot();
          setTimeout(showSettings, 150000);
        }
      });
    }

    function showInfo (path) {
      common(path);
      var model = new backbone.Model();
      var tmpl = "links";
      if(path === 'settings') tmpl = 'settings';
      model.fetch({
        url: "/api/dashboard/" + path,
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
