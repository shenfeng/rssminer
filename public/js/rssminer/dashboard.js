$(function(){
  var reader = window.Rssminer,
      backbone = window.Backbone,
      tmpls = reader.tmpls,
      to_html = window.Mustache.to_html,
      ajax = reader.ajax;

  var Router = backbone.Router.extend(function () {

    function makeAlive (url, tmpl, callback) {
      ajax.get(url).done(function (data) {
        $("#content").empty().append(to_html(tmpl, data));
        if(typeof callback === 'function') {
          callback();
        }
      });
    }

    function handler (path, page) {
      path = path || "settings";
      page = path || 1;
      var callback = function () {
        $("#nav li").removeClass('active')
          .filter('.' + path).addClass('active');
        if(path === 'black') {
          var $input = $("#add-patten");
          $input.keyup(function (e) {
            if(e.which === 13) {
              var patten = $input.val();
              ajax.jpost("/api/dashboard/black",
                         {patten: patten}).done(function (data) {
              });
            }
          });
        }
      };
      makeAlive("/api/dashboard/" + path , tmpls[path], callback);
    };

    return  {
      initialize: function () {
        backbone.history.start();
      },
      routes: {
        ":path": handler,
        ":path/:page": handler
      }
    };
  });

  new Router();
});
