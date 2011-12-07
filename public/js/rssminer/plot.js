(function () {
  var key = 'plot_data',
      data_size =  200;

  var line_opts = {
    yaxis: { min: 0, max: 3500 },
    xaxis: { min: 0, max: data_size }
  },
      pie_opts = {
        series: {
          pie: { show: true }
        }
      },
      server = {
        1000: 'crawler_counter',
        1200: 'crawler_queue',
        1250: 'queue_aval',
        1300: 'crawler_start'
      },
      http = {
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
      };


  function read_plot_data () {
    var d = window.localStorage.getItem(key);
    if(d) {
      return JSON.parse(d);
    } else {
      d = [];
      for(var i = 0; i < data_size; ++i) {
        d.push([0, 0, 0, 0]);
      }
      return d;
    }
  }
  var prev, crawler = {}, lines_data = read_plot_data();

  function prepare_data () {  // convert data to plot format
    var lines = [];
    for(var j = 0; j < lines_data[0].length; ++j) {
      var line = [];
      for(var i = 0; i < lines_data.length; ++i) {
        line.push([i, lines_data[i][j]]);
      }
      lines.push(line);
    }
    var pie = [];
    _.each(http, function (v, k) {
      if(crawler && crawler[k]) {
        pie.push({label: v, data: crawler[k]});
      }
    });
    return {lines: lines, pie: pie};
  }

  function parse (resp) {
    crawler = resp.crawler;
    if(crawler) {
      _.each(server, function (v, k) {
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
      lines_data.splice(0,1);
      lines_data.push([resp.crawler_counter_delta, // crawler speed
                       resp.crawler_links_delta,   // increase links
                       resp.rss_links_delta,       // increase rss links
                       resp.feeds_delta]);         // increase rss feed
    }
    prev = resp;
    return resp;
  }

  function plot () {
    var d = prepare_data();
    $.plot($("#line-chart"), d.lines, line_opts);
    $.plot($("#crawler-pie"), d.pie , pie_opts);
    window.localStorage.setItem(key, JSON.stringify(lines_data));
  }

  window.RM = $.extend(window.RM, {
    plot: {
      parse: parse,
      plot: plot
    }
  });
})();
