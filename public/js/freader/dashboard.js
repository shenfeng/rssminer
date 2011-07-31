$(function(){
  var reader = window.Freader,
      ajax = reader.ajax;

  var update = function () {
    ajax.get("/api/dashboard/crawler").done(function (data) {
      var t = $("#template").html(),
          html = _.template(t, data);
      $("#page-wrap").empty().append(html);
      // _.delay(update, 15000);
    });
  };
  update();
});
