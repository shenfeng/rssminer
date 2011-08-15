$(function(){
  var reader = window.Rssminer,
      backbone = window.Backbone,
      tmpls = reader.tmpls,
      ajax = reader.ajax;

  var Router = backbone.Router.extend(function () {
    function makeAlive (url, tmpl, callback) {
      ajax.get(url).done(function (data) {
        $("#tables").empty()
          .append(_.template(tmpls['stats'], data))
          .append(_.template(tmpl, data));
        if(typeof callback === 'function') {
          callback();
        }
      });

    }

    function handler (path, page) {
      path = path || "rsslinks";
      page = path || 1;
      var callback = function () {
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
