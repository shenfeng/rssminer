$(function(){
  var reader = window.Rssminer,
      backbone = window.Backbone,
      tmpls = reader.tmpls,
      ajax = reader.ajax;

  var Router = backbone.Router.extend(function () {
    function makeAlive (url, tmpl, data) {
      ajax.get(url).done(function (data) {
        $("#tables").empty()
          .append(_.template(tmpls['stats'], data))
          .append(_.template(tmpl, data));
      });
    }

    function handler (path, page) {
      path = path || "rsslinks";
      page = path || 1;
      makeAlive("/api/dashboard/" + path , tmpls[path]);
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
