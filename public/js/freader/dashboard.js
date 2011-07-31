$(function(){
  var reader = window.Freader,
      ajax = reader.ajax;

  var update = function () {
    var f = _.template($("#template").html());
    ajax.get("/api/dashboard/crawler").done(function (data) {
      var html = f(data);
      $("#page-wrap").empty().append(html);
    });
  };
  update();
});
